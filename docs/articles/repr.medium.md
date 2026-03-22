# Repr Derivation (Products + Sums): A Step-by-Step Build Story

When debugging gets serious, raw values are rarely enough.
You also want context: field names, types, and nested structure.
For example:

```scala
Item(id: Long = 100500, content: Package = Box(count: Short = 256, massKg: Float = 9.81))
```

This tells a much clearer story than `Item(100500, Box(256, 9.81))`.

A manual `toString` can provide this, but writing and maintaining that by hand quickly turns into boilerplate.
And boilerplate usually drifts out of sync.

So the idea of this article is simple: let metaprogramming do the repetitive work.
We derive `Repr` automatically for product and sum types and keep the output rich enough for real debugging.



## Table of Contents
- [The Solution](#the-solution)
  - [ReprSpec (compact intent)](#reprspec-compact-intent)
  - [Repr (compact intent)](#repr-compact-intent)
- [Warming up: `inline`-s](#warming-up-inline-s)
- [Background](#background)
  - [Case 1: simplest product — `Foo()` bootstrap](#case-1-the-simplest-product-type-foo-bootstrap)
  - [Case 2: simplest sum — `Option`](#case-2-the-simplest-sum-type-option-sums)
  - [Development process at a glance](#development-process-at-a-glance-after-the-bootstrap-step)
    - [Step 1: concrete first, generic next](#step-1-concrete-first-generic-next)
    - [Step 2 — Mirror fallback; ambiguous given instances](#step-2--add-mirror-fallback-challenge-ambiguous-given-instances)
    - [Step 3 — final solution: resolve each element with a concrete type](#step-3--final-solution-resolve-each-element-with-a-concrete-type)
- [Recursion and Refactoring](#recursion-and-refactoring)
  - [Challenge: infinite recursion with Lst](#challenge-infinite-recursion-with-lst)
  - [Solution: make reprs lazy](#solution-make-reprs-lazy)
- [Summary](#summary)

## The Solution (if you already know the answer, otherwise skip to the Warming Up section)

At a high level, the final result does two jobs:

- derive product representations with field names and type labels
- derive sum representations by dispatching to the active subtype

### ReprSpec (compact intent)

```scala
  case class Bar(n: Int, m: Int) derives Repr
  case class Baz(n: Int, bar: Bar) derives Repr
  
  test("Repr for Baz(1, Bar(2, 3))") {
    val baz = Baz(1, Bar(2, 3))
    baz.repr shouldBe "Baz(n: Int = 1, bar: Bar = Bar(n: Int = 2, m: Int = 3))"
  }

  test("Repr for List") {
    given R: Repr[List[Int]] = Repr.derived
    R.label shouldBe "List"
    R.repr(Nil) shouldBe "Nil()"
    R.repr(List(1)) shouldBe "::(head: Int = 1, next: List = Nil())"
    R.repr(List(1, 2)) shouldBe "::(head: Int = 1, next: List = ::(head: Int = 2, next: List = Nil()))"
  }
```

### Repr (compact intent)

```scala
inline def derived[T](using m: Mirror.Of[T]): Repr[T] =
  val label = constValue[m.MirroredLabel]
  lazy val reprs = summonReprs[m.MirroredElemTypes]
  inline m match
    case _: Mirror.ProductOf[T] =>
      val argNames = constValueTuple[m.MirroredElemLabels].toList.map(_.toString)
      productRepr[T](label, argNames, reprs)
    case s: Mirror.SumOf[T] =>
      sumRepr[T](label, s, reprs)
```

Think of the core pieces like this:
- `label`: the type name (e.g., `"Bar"`, `"Option"`)
- `argNames`: field names known at compile time (e.g., `["n", "m"]`)
- `reprs`: derived `Repr` instances for fields/subtypes, kept lazy to avoid recursive loops
- `inline match`: routes to product logic or sum logic based on type shape

Everything else in this step-by-step story is about making those two branches generic, recursive, and reliable.

## Warming up: `inline`-s

What is `inline def` doing here in plain language?
It tells the compiler: "Do not treat this like a normal function call. Expand it right where it is used."

```scala
object Debug {

  inline def included(enabled: Boolean)(inline code: => Unit): Unit =
    if enabled then { code; () } else ()
}
```

Usages of `included` are now expanded at compile time:

```scala
Debug.included(false) {
  println("Nothing to be compiled. No output.")
}

Debug.included(true) {
  val x = 10
  val y = 5
  val z = x + y
  println("This code compiles and outputs: " + z)
}
```

If we add this to `build.sbt`:

```scala
scalacOptions ++= Seq(
  "-Vprint:postInlining",
  "-Xmax-inlines:100000",
  "-Xcheck-macros",
)
```

Then in sbt interactive mode, `~console` keeps printing post-inlining output as sources change:

```bash
[info]   @SourceFile("src/main/scala/progmeta/Main.scala") final module class Main()
[info]      extends Object() { this: progmeta.Main.type =>
...
[info]     @main def run(): Unit =
[info]       {
[info]         ():Unit
[info]         {
[info]           {
[info]             val x: Int = 10
[info]             val y: Int = 5
[info]             val z: Int = (x + y)
[info]             println("This code compiles and outputs: " + z)
[info]           }
[info]           ()
[info]         }:Unit
[info]       }
[info]   }
```

In plain terms, `Debug.included` is a compile-time switch.
Set it to `true`, and the block stays in the generated code (`x`, `y`, `z`, `println` are visible).
Set it to `false`, and the compiler erases that block into `():Unit`.
So there is no runtime branch cost to pay later.

## Background

Before we list the tools, one key idea first: `Mirror` is the compiler's structural view of a type.
It tells us the type shape: product (fields, like a case class) or sum (alternatives, like an enum/sealed hierarchy).
That shape is exactly what lets derivation choose the right strategy.

Here is a quick map of the compile-time tools we use.

| Primitive | What it does                                                                                                                                                                                                                                         |
|---|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `inline def` | Forces the compiler to expand the body at every call site                                                                                                                                                                                            |
| `constValue[T]` | Extracts a compile-time constant from a singleton type (e.g. `"Bar"` from `m.MirroredLabel`)                                                                                                                                                         |
| `constValueTuple[T]` | Like `constValue`, but for a tuple of singleton types (e.g. field name labels)                                                                                                                                                                       |
| `erasedValue[T]` | Returns a compile-time placeholder of type `T`, a phantom value of the type; e.g. in `inline erasedValue[(Int *: String *: EmptyTuple)] match`, the compiler picks the `case _: (h *: t)` branch during inlining, and no value is created at runtime |
| `summonInline[T]` | Resolves an implicit of type `T` at compile time; e.g. `summonInline[Repr[Int]]` succeeds if `given Repr[Int]` exists, while `summonInline[Repr[Foo]]` fails at compile time if no given can be found                                                |
| `summonFrom { ... }` | Tries implicit strategies in order; e.g. `summonFrom { case r: Repr[Elem] => r; case m: Mirror.Of[Elem] => Repr.derived[Elem] }` uses an existing `given Repr[Elem]` if present, otherwise falls back to mirror-based derivation                     |
| `inline match` | A match expression resolved entirely at compile time; each branch is specialised for the matched type shape                                                                                                                                          |

These are the building blocks used throughout the article.
If they feel abstract now, that is okay; each one becomes concrete in the steps from this section onward.

### Case 1: The simplest product type: `Foo()` (bootstrap)

Start with the smallest possible goal:

```scala
case class Foo() derives Repr
```

should compile.


The first change is intentionally tiny, just enough for the compiler to accept derivation:

```scala
trait Repr[T] 

object Repr {
  inline def derived[T](using m: Mirror.Of[T]): Repr[T] = ???
}
```

Next, make it usable with a basic implementation and an extension method:

```scala
test("Repr for Foo()") {
  import Repr.*
  Foo().repr shouldBe "Foo()"
}
```
I wanted to write `foo.repr`, not `summon[Repr[Foo]].repr(foo)`, so an extension method made the API feel natural.

```scala
import scala.compiletime.constValue

trait Repr[T] {
  def repr(t: T): String
}

object Repr {
  extension [T](t: T)
    def repr(using r: Repr[T]): String = r.repr(t)

  inline def derived[T](using m: Mirror.Of[T]): Repr[T] =
    val typeLabel = constValue[m.MirroredLabel]
    new Repr[T] {
      override def repr(t: T): String = s"$typeLabel()"
    }
}
```
I used `constValue` to read the type label at compile time instead of hardcoding it.
That was the smallest implementation that made the first test pass, and the first real metaprogramming moment in the article.

### Case 2: The simplest sum type `Option` (sums)

Sum types are the next interesting case.
They have multiple alternatives, and we want output that clearly shows which branch is active:

```scala
  test("Repr for Sum type") {
    given R: Repr[Option[Boolean]] = Repr.derived
    R.label shouldBe "Option"

    val a: Option[Boolean] = Some(true)
    val b: Option[Boolean] = None
  
    a.repr shouldBe "Some(value: Boolean = true)"
    b.repr shouldBe "None()"
  }
```

To pass this test, we only need one thing right now: explicit output for the active case.
So the first implementation is intentionally concrete:


```scala
trait Repr[T] {
  def repr(t: T): String
  def label: String
}

...

private def sumRepr[T](typeLabel: String): Repr[T] =
  new Repr[T] {
    override def repr(t: T): String = t match
      case Some(v) => s"Some(value: Boolean = $v)"
      case None    => "None()"
    override def label: String = s"$typeLabel"
  }
```

This matches the test exactly.
I pattern-matched on `Some` and `None` directly because it was the smallest path to green.
I also hardcoded `Boolean` on purpose, because at this step the test only cares about `Option[Boolean]`.

The key design change from Case 1 happens in `derived[T]`.
Before `Option`, `derived[T]` was basically one trivial product path.
Now it branches by mirror shape and delegates to separate product/sum handlers:

```scala
inline def derived[T](using m: Mirror.Of[T]): Repr[T] =
  val label = constValue[m.MirroredLabel]
  inline m match
    case _: Mirror.ProductOf[T] => productRepr[T](label)
    case _: Mirror.SumOf[T]     => sumRepr[T](label)
```

where
```scala
private def productRepr[T](typeLabel: String): Repr[T] = new Repr[T] {
  override def repr(t: T): String = new Repr[T] {
    override def repr(t: T): String = s"$typeLabel()"
  }
  override def label: String = typeLabel
}
```

At this point both branches are still hardcoded.
That is okay.
The real progress is that `derived[T]` now understands the difference between product and sum types.

From here on, we are no longer bootstrapping.
Now we are iterating toward general derivation.


### Development process at a glance (after the bootstrap step)

1. **Product: several parameters**


```scala
case class Bar(n: Int, m: Int) derives Repr
Bar(1, 2).repr shouldBe "Bar(n: Int = 1, m: Int = 2)"
```

After sums, we return to products.
Now we want labeled fields instead of just `()`.
For this first step, assume every field type is `Int`:

```scala
private def productRepr[T](typeLabel: String): Repr[T] = new Repr[T] {
  override def repr(t: T): String =
    // Hardcoded for Bar specifically: two Int fields named "n" and "m"
    val argValues = t.asInstanceOf[Product].productIterator.toList
    val n = argValues(0)
    val m = argValues(1)
    s"$typeLabel(n: Int = $n, m: Int = $m)"
  override def label: String = typeLabel
}
```

I hardcoded field names (`n`, `m`) and field type (`Int`) intentionally.
Not as the final design, but as the fastest way to validate the output format.

Then `derived[T]` evolves again.
It already distinguishes products from sums; now it must also gather product metadata and pass it to `productRepr` instead of relying on hardcoded names.

For that we use two compile-time tools: `m.MirroredElemLabels` and `constValueTuple`.
`constValue` gives one singleton constant.
`constValueTuple` gives a whole tuple of constants, which is exactly what we need for field names.

```scala
inline def derived[T](using m: Mirror.Of[T]): Repr[T] =
  val label     = constValue[m.MirroredLabel]
  val argNames  = constValueTuple[m.MirroredElemLabels].toList.map(_.toString)
  val agrLabels = argNames.map(_ => "Int")
  inline m match
    case _: Mirror.ProductOf[T] => productRepr[T](label, argNames, agrLabels)
    case _: Mirror.SumOf[T]     => sumRepr[T](label)

private def productRepr[T](typeLabel: String, argNames: List[String], agrLabels: List[String]): Repr[T] =
  new Repr[T] {
    override def repr(t: T): String =
      val argValues = t.asInstanceOf[Product].productIterator.toList
      val args = argNames.lazyZip(agrLabels).lazyZip(argValues)
        .map((name, label, value) => s"$name: $label = $value").mkString(", ")
      s"$typeLabel($args)"
      
    override def label: String = typeLabel
  }
```

There was one failed attempt before this version.
I first tried to extract type labels directly from `m.MirroredElemTypes`:

```scala
val agrLabels = constValueTuple[m.MirroredElemTypes].toList.map(_.toString)
```

That fails for `Bar(n: Int, m: Int)` because `Int` is not a singleton constant value.
So I used the simplest working approximation: every field label is `"Int"`.
It is temporary, but enough to move forward.

2. **Nested products: `Baz(n: Int, bar: Bar)` — replace hardcoded `"Int"` with `summonReprs`**

```scala
case class Baz(n: Int, bar: Bar) derives Repr

test("Repr for Baz(1, Bar(2, 3))") {
  val baz = Baz(1, Bar(2, 3))
  baz.repr shouldBe "Baz(n: Int = 1, bar: Bar = Bar(n: Int = 2, m: Int = 3))"
}
```

`Baz` breaks the previous shortcut.
It has `bar: Bar`, so hardcoding every label as `"Int"` is now wrong.
We need real per-field type information, resolved at compile time.

So I replaced hardcoded labels with `summonReprs[m.MirroredElemTypes]` and added primitive `Repr` givens as a base:

```scala
given Repr[Int] with
  override def repr(t: Int): String = t.toString
  override def label: String = "Int"

private inline def summonReprs[T <: Tuple]: List[Repr[?]] =
  inline erasedValue[T] match
    case _: EmptyTuple          => Nil
    case _: (elem *: elems)     => summonInline[Repr[elem]] :: summonReprs[elems]
```
This walks field types at compile time and summons `Repr` for each element.
`Int` resolves from the primitive given.
`Bar` resolves from the synthesized given produced by `Bar derives Repr`.
That is why `Baz` finally prints `bar: Bar = ...` instead of `bar: Int = ...`.


```scala
inline def derived[T](using m: Mirror.Of[T]): Repr[T] =
  val label = constValue[m.MirroredLabel]
  inline m match
    case _: Mirror.ProductOf[T] =>
      val argNames = constValueTuple[m.MirroredElemLabels].toList.map(_.toString)
      val reprs    = summonReprs[m.MirroredElemTypes]   // replaces argNames.map(_ => "Int")
      productRepr[T](label, argNames, reprs)
    case _: Mirror.SumOf[T] => sumRepr[T](label)
```

3. **Sums**

##### Step 1: concrete first, generic next
The concrete `Option` match is already green, so I keep it as a baseline and generalize in small moves.
Instead of hardcoding `Some`/`None`, I precompute subtype `Repr`s with `summonReprs[m.MirroredElemTypes]`.
Then `Mirror.SumOf` gives the active case index via `ordinal`, and we dispatch through `reprs`.
One important wiring detail: compute `reprs` once in `derived[T]` and share it across product and sum branches.


```scala
// concrete green
case Some(v) => s"Some(value: Boolean = $v)"
case None    => "None()"

// generic sum dispatch
reprs(sum.ordinal(t)).asInstanceOf[Repr[Any]].repr(t)
```
The heart of this generic approach is `s.ordinal(t)`.
`sum: Mirror.SumOf[T]` describes the sum type shape at compile time.
At runtime, `ordinal` tells which case is active.
For `Option[Boolean]`, `Some(true)` maps to `0`, `None` maps to `1`.
That index picks the right subtype `Repr`.

At this stage, `sumRepr` is implemented as:

```scala
private def sumRepr[T](
    typeLabel: String,
    sum: Mirror.SumOf[T],
    reprs: List[Repr[?]]
  ): Repr[T] = new Repr[T] {
  override def repr(t: T): String =
    reprs(s.ordinal(t)).asInstanceOf[Repr[Any]].repr(t)
  override def label: String = typeLabel
}
```

We can use it for derivation:

```scala
inline def derived[T](using m: Mirror.Of[T]): Repr[T] =
  val label = constValue[m.MirroredLabel]
  val reprs = summonReprs[m.MirroredElemTypes]   // moved here from inside the product branch
  inline m match
    case _: Mirror.ProductOf[T] =>
      val argNames = constValueTuple[m.MirroredElemLabels].toList.map(_.toString)
      productRepr[T](label, argNames, reprs)
    case s: Mirror.SumOf[T] =>
      sumRepr[T](label, s, reprs)               // reprs now passed to sumRepr too
```

The first attempt after this refactor failed.
Moving `reprs` to the top of `derived[T]` exposed this compiler error for sums:

```
No given instance of type progmeta.Repr[Some[Boolean]] was found
```

At that time, `summonReprs` still used single-pass `summonInline`.
That works for primitives and many product cases, but not for sum subtypes like `Some[Boolean]` and `None.type` in this path.

So we need recursive resolution per subtype.
`Some[Boolean]` and `None.type` are derivable too; they just need the same derivation pipeline reused recursively.
That is exactly why the next step introduces `summonFrom` fallback.

### Step 2 — add Mirror fallback; challenge: ambiguous given instances

The natural fix: if an explicit `Repr[elem]` is not found, fall back to mirror-based derivation.
In recursive form:

```scala
private inline def summonReprs[T <: Tuple]: List[Repr[?]] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (elem *: elems) =>
      val head = summonFrom {
        case repr: Repr[elem]   => repr // <- elem is abstract here
        case m: Mirror.Of[elem] => derived(using m)
      }
      head :: summonReprs[elems]
```

This solves the missing-instance error, but introduces a new failure for:

```scala
  test("Repr for Some") {
    given R: Repr[Some[Boolean]] = Repr.derived
    given N: Repr[None.type ] = Repr.derived

    R.label shouldBe "Some"
    N.label shouldBe "None"
  }
```
with a compiler error:

```
[E172] Type Error: ReprSpec.scala:...
Ambiguous given instances: both given instance N and given instance R
match type progmeta.Repr[elem]
```

Root cause: inside `case repr: Repr[elem]`, `elem` is still abstract.
So multiple `Repr[...]` values in scope can look compatible, and implicit search becomes ambiguous.

### Step 3 — final solution: resolve each element with a concrete type

The fix is subtle but clean: move element resolution to a separate `private inline def` that is instantiated with a concrete type at each inline site.

Each element is resolved through `sumRepr[Elem]` with a concrete type:

```scala
private inline def summonReprs[T <: Tuple]: List[Repr[?]] =
  inline erasedValue[T] match
    case _: EmptyTuple      => Nil
    case _: (elem *: elems) => sumRepr[elem] :: summonReprs[elems]

private inline def sumRepr[Elem]: Repr[Elem] =
  summonFrom {
    case r: Repr[Elem]      => r  // <- Elem is concrete here
    case m: Mirror.Of[Elem] => Repr.derived[Elem]
  }
```

Because `sumRepr[elem]` is inlined separately for each concrete `elem`, implicit search runs with a concrete target type each time.
That removes the ambiguity.

This closes the missing-instance gap for `Some[Boolean]` and `None.type`.

In short, this method builds a `List[Repr[?]]` for every element in `MirroredElemTypes`.
For products, those are field types.
For sums, those are subtype cases.
And that list is exactly what `s.ordinal(t)` indexes at runtime.



## Recursion and Refactoring

### Challenge: infinite recursion with Lst

The most revealing step was adding a recursive enum:

```scala
enum Lst[+T] derives Repr:
  case Cns(t: T, ts: Lst[T])
  case Nl

test("Repr for recursive type") {
  val empty: Lst[Int] = Lst.Nl
  val unit : Lst[Int] = Lst.Cns(1, Lst.Nl)
  val list : Lst[Int] = Lst.Cns(1, Lst.Cns(2, Lst.Nl))

  empty.repr shouldBe "Nl()"
  unit.repr shouldBe "Cns(t: Int = 1, ts: Lst = Nl())"
  list.repr shouldBe "Cns(t: Int = 1, ts: Lst = Cns(t: Int = 2, ts: Lst = Nl()))"
}
```

The compiler warns us:

```bash
[warn] -- Warning: ... ReprSpec.scala: ...
[warn] 13 |  enum Lst[+T] derives Repr:
[warn]    |                       ^
[warn]    |Infinite loop in function body
[warn]    |{
```

And the test crashes with a stack overflow:

```bash
An exception or error caused a run to abort. 
java.lang.StackOverflowError
	at progmeta.ReprSpec$Lst$.derived$Repr(ReprSpec.scala:13)
	at progmeta.ReprSpec$Lst$.derived$Repr(ReprSpec.scala:13)
	...
```

The root cause is eager evaluation.
`summonReprs` tries to resolve all element `Repr`s immediately.
For `Cns[T]`, one element is `Lst[T]` itself.
So deriving `Repr[Lst[T]]` re-enters `Repr.derived[Lst[T]]` before the first run finishes, and we loop forever.

### Solution: make reprs lazy

The first fix is one keyword:

```scala
inline def derived[T](using m: Mirror.Of[T]): Repr[T] =
  ...
  lazy val reprs = summonReprs[m.MirroredElemTypes]
  ...
```

Making `reprs` lazy breaks the derivation-time cycle.
Placement matters: `lazy val reprs` must stay in `derived`, right where recursion starts.
But one more detail is still missing, so the test still fails.

Key idea: `lazy val` and by-name (`=> T`) must work together.
`lazy val` delays creation.
By-name delays usage.
If you keep only one side lazy, evaluation gets forced too early.

```scala
private def productRepr[T]( ..., reprs: => List[Repr[?]]): Repr[T] = ...
private def sumRepr[T]( ..., reprs: => List[Repr[?]]): Repr[T] = ...   
```

Now the recursion case passes.


The same trick also works for `List`, which has the same recursive sum-of-products shape.

```scala
  test("Repr for List") {
    given R: Repr[List[Int]] = Repr.derived
    R.label shouldBe "List"
    R.repr(Nil) shouldBe "Nil()"
    R.repr(List(1)) shouldBe "::(head: Int = 1, next: List = Nil())"
    R.repr(List(1, 2)) shouldBe "::(head: Int = 1, next: List = ::(head: Int = 2, next: List = Nil()))"
  }
```

This is a very common metaprogramming pattern: types can look correct while evaluation order is still wrong.
Compiler warnings were not noise here; they were signals pointing to the real issue.

## Summary

The `Repr` journey starts as a tiny experiment and grows into a recursive representation utility.
Every major move is triggered by a failing test or compiler feedback, not by heavy up-front design.

| Stage | Challenge | Solution |
|---|---|---|
| Bootstrap | `Foo derives Repr` must compile, then `Foo().repr` must be callable in a test | Add a minimal `derived` hook first, then a trivial implementation plus the `repr` extension |
| Sums — first pass | `Option[Boolean]` needs explicit `Some(...)` / `None()` output | Split `derived` into product/sum branches; use a concrete hardcoded `sumRepr` first |
| Products — `Bar` | Fields need names and type labels in the output | Hardcode field names and `"Int"` first, then generalize with `MirroredElemLabels` and `constValueTuple` |
| Nested products — `Baz` | `bar: Bar` field has a non-primitive type; hardcoded `"Int"` label is wrong | Replace hardcoded type labels with `summonReprs[m.MirroredElemTypes]`; add primitive `Repr` givens |
| Sums — step 1 | Hardcoded `sumRepr` only worked for `Option[Boolean]` | Move to ordinal dispatch with precomputed subtype reprs via `s.ordinal(t)` |
| Sums — step 2 | `No given instance of type Repr[Some[Boolean]] was found` | Add `Mirror`-based fallback in `summonFrom` |
| Sums — step 3 | `[E172] Ambiguous given instances` — abstract `elem` matched too broadly | Extract `private inline def sumRepr[Elem]` so implicit search sees a concrete type |
| Recursion | Infinite recursion and compiler warning when deriving `Lst` | Make `reprs` a `lazy val` in `derived`; pass it by-name (`=> List[Repr[?]]`) to `productRepr` and `sumRepr` |

The main lesson is simple: each new test reveals the next design requirement.
In this workflow, tests are not just checks at the end; they are the steering wheel.

---

Note: this article is maintained as a dedicated topic file under `docs/articles/` and follows the plan in `docs/plans/repr.plan.md`.
