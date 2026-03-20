# Repr Derivation (Products + Sums): TDD Process Description

This article documents the iterative, test-driven development (TDD) process for building `Repr` derivation for product and sum types.

## Table of Contents
- [The Solution](#the-solution)
- [Background](#background)
- [Product Derivation](#product-derivation)
- [Sum Derivation](#sum-derivation)
- [Recursion and Refactoring](#recursion-and-refactoring)
- [Summary](#summary)

## The Solution

At a high level, the final solution does two things:

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

The key elements:
- `label`: the type name (e.g., `"Bar"`, `"Option"`)
- `argNames`: field labels extracted at compile time (e.g., `["n", "m"]`)
- `reprs`: recursively derived `Repr` instances for all fields/subtypes, deferred lazily to break cycles
- The inline match dispatches to either product or sum logic

Everything else in the TDD story is about making these two branches correct, recursive, and testable.

## Background

I built this feature in small, practical steps.

**Case 1: The simplest product type: `Foo()` (bootstrap).**

At first, we want one simple thing: 

```scala
case class Foo() derives Repr
```

should compile.


The minimum change was just enough for the compiler to accept derivation:

```scala
trait Repr[T] 

object Repr {
  inline def derived[T](using m: Mirror.Of[T]): Repr[T] = ???
}
```

Let's make it usable by adding a basic implementation and the extension method for the case:

```scala
test("Repr for Foo()") {
  import Repr.*
  Foo().repr shouldBe "Foo()"
}
```
I wanted to write `foo.repr`, not `summon[Repr[Foo]].repr(foo)`, so I added the extension method.

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
I used `constValue` to get type label in compile time insted of hardocing it. This was the smallest implementation that made the first behavior test pass.
Here is the first meta-related feature used!

**Case 2: The simplest sum type `Option` (sums).**

Sum types are a bit more interesting. They have several cases, and we want to be able to represent all of them together with their basic product type:

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

To do this, we need the output to be explicit about which case was active. The first version that made these `Option` expectations pass was also deliberately concrete:


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

This implementation corresponds exactly to the `Option[Boolean]` assertions above. I matched directly on `Some` and `None` because that was the smallest way to make the first sum test pass. I also hardcoded `Boolean` in the output because the test only asked for `Option[Boolean]` at that point.

The important change from Case 1 was in `derived[T]`. Before `Option`, `derived[T]` just wrapped a trivial case. To support sums, I changed it so that it branches on the mirror shape and delegates to a separate sum implementation:

```scala
inline def derived[T](using m: Mirror.Of[T]): Repr[T] =
  val label = constValue[m.MirroredLabel]
  inline m match
    case mp: Mirror.ProductOf[T] => productRepr[T](label)
    case _: Mirror.Of[T]         => sumRepr[T](label)
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

At that stage both branches were still hardcoded. The product branch returned the old `"Foo()"` behavior, while the sum branch returned the new `Option`-specific string. The key step was not generality yet — it was teaching `derived[T]` to distinguish products from sums.

Once that behavior was confirmed, I replaced the special-case implementation with generic sum dispatch:

```scala
private def sumRepr[T](
    typeLabel: String,
    s: Mirror.SumOf[T],
    reprs: => List[Repr[?]]
  ): Repr[T] = new Repr[T] {
  override def repr(t: T): String =
    reprs(s.ordinal(t)).asInstanceOf[Repr[Any]].repr(t)
  override def label: String = typeLabel
}
```

At that point the implementation no longer knew anything specific about `Option`: it worked by selecting the active subtype representation through the sum ordinal.

**Case 3: `Bar` (products).**
```scala
case class Bar(n: Int, m: Int) derives Repr
Bar(1, 2).repr shouldBe "Bar(n: Int = 1, m: Int = 2)"
```

After sums, I came back to products and wanted labeled product output instead of plain `toString`. The first version that made this kind of test pass was deliberately hardcoded:

```scala
private def productRepr[T](typeLabel: String): Repr[T] = new Repr[T] {
  override def repr(t: T): String =
    val argValues = t.asInstanceOf[Product].productIterator.toList
    s"$typeLabel(n: Int = ${argValues(0)}, m: Int = ${argValues(1)})"
  override def label: String = typeLabel
}
```

This implementation corresponds to the `Bar` expectation above. I hardcoded the field names (`n`, `m`) and field type (`Int`) because that was the smallest change that turned the test green. It was not meant to be final — it was just the fastest way to confirm the output shape I wanted.

The important change from Case 2 was another change in `derived[T]`. It already knew how to distinguish products from sums. For `Bar`, I changed the product path so it started gathering product metadata and passing it into `productRepr`:

```scala
inline def derived[T](using m: Mirror.Of[T]): Repr[T] =
  val label     = constValue[m.MirroredLabel]
  val argNames  = constValueTuple[m.MirroredElemLabels].toList.map(_.toString)
  val agrLabels = argNames.map(_ => "Int")
  inline m match
    case _: Mirror.ProductOf[T] => productRepr[T](label, argNames, agrLabels)
    case _: Mirror.Of[T]        => sumRepr[T](label)
```

There was one failed attempt before this green version. I first tried to get the argument type labels from `MirroredElemTypes`:

```scala
val agrLabels = constValueTuple[m.MirroredElemTypes].toList.map(_.toString)
```

That did not compile for `Bar(n: Int)`: `Int` is not a constant type, so it cannot be extracted with `constValueTuple` that way. To keep moving, I replaced it with the smallest working approximation: treat every field label as `"Int"`. That was enough for the current test and made the first product step green.

Once that worked, I made it generic by removing the hardcoded field names and types:

```scala
private def productRepr[T](
    typeLabel: String,
    argNames: List[String],
    reprs: => List[Repr[?]]
  ): Repr[T] = new Repr[T] {
  override def repr(t: T): String =
    val argValues = t.asInstanceOf[Product].productIterator.toList
    val args = argNames.lazyZip(reprs).lazyZip(argValues)
      .map { (name, repr, value) =>
        s"$name: ${repr.label} = ${repr.asInstanceOf[Repr[Any]].repr(value)}"
      }
    s"$typeLabel(${args.mkString(", ")})"
  override def label: String = typeLabel
}
```

Later, I updated the product branch of `derived[T]` again so that it could pass both field names and recursively derived field representations into the generalized `productRepr`:

```scala
inline def derived[T](using m: Mirror.Of[T]): Repr[T] =
  val label = constValue[m.MirroredLabel]
  lazy val reprs = summonReprs[m.MirroredElemTypes]
  inline m match
    case _: Mirror.ProductOf[T] =>
      val argNames = constValueTuple[m.MirroredElemLabels].toList.map(_.toString)
      productRepr[T](label, argNames, reprs)
```

This is the point where generic product derivation becomes real: `derived[T]` stops building product output from hardcoded `"Int"` labels and starts building a product-specific representation from compile-time field metadata plus recursively derived field type classes.

At that point `Bar` was no longer special: any product type with supported field representations could be derived the same way.

Finally, when I introduced recursive types (`Lst`, then `List`), derivation started to recurse too early and produced recursion-related failures/warnings. I fixed that by deferring recursive evidence at the right place: `lazy val reprs` in `derived`.

### Development process at a glance (after the bootstrap step)

1. **Sums: concrete first, generic next**

```scala
// concrete green
case Some(v) => s"Some(value: Boolean = $v)"
case None    => "None()"

// generic sum dispatch
reprs(s.ordinal(t)).asInstanceOf[Repr[Any]].repr(t)
```

2. **Products: first make it pass, then make it generic**

```scala
// hardcoded green for Bar
s"$typeLabel(n: Int = ${argValues(0)}, m: Int = ${argValues(1)})"

// refactored generic product formatting
val args = argNames.lazyZip(reprs).lazyZip(argValues).map { (name, repr, value) =>
  s"$name: ${repr.label} = ${repr.asInstanceOf[Repr[Any]].repr(value)}"
}
```

3. **Recursion: defer evaluation at the right boundary**

```scala
// fix recursion/evaluation-order problems
lazy val reprs = summonReprs[m.MirroredElemTypes]
```


## Product Derivation

With `Foo()` working, the first meaningful test raised the bar:

```scala
bar.repr shouldBe "Bar(n: Int = 1, m: Int = 2)"
```

### Challenge: compiler error when Bar derives Repr

To get there, `Bar` was added to the test companion object:

```scala
case class Bar(n: Int, m: Int) derives Repr
```

This immediately produced a compiler error:

```
[E182] Type Error: ReprSpec.scala:9:33
```

The message indicated the `derived` implementation could not handle a multi-element product. At that point `derived` only handled the simplest case — it could not yet enumerate field types and resolve a `Repr` per element. The test did not even run; it did not compile.

### Hardcoding: first pass at productRepr

The immediate goal was to get the test to compile and pass. The approach was to hardcode the exact shape needed for `Bar(n, m)`:

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

This hardcoded version worked: `Bar(1, 2).repr` returned `"Bar(n: Int = 1, m: Int = 2)"`. The test passed. But the approach was brittle — it only worked for `Bar`, and only when both fields were `Int`. The duplication between the hardcoded field names and the actual case class definition was the signal to refactor.

The first step toward green was deliberate and honest: field labels were hardcoded as `Int` to satisfy the compiler and make one test pass. The commit message was explicit — "hardcoded agrLabels as Int to make the test green". This is not a shortcut to hide; it is a TDD technique. Get green, understand the shape of the problem, then refactor.

The test then pushed further by adding a second field to `Bar(n, m)`. The hardcoded approach immediately broke, which was the correct signal to refactor.

The refactored solution introduced `Repr[Int]`, `Repr[Double]`, and `Repr[Boolean]` as primitive instances, and rewired `productRepr` to work for any product type:

```scala
private def productRepr[T](
    typeLabel: String,
    argNames: List[String],
    reprs: => List[Repr[?]]
  ): Repr[T] = new Repr[T] {
  override def repr(t: T): String =
    val argValues = t.asInstanceOf[Product].productIterator.toList
    val args = argNames.lazyZip(reprs).lazyZip(argValues)
      .map { (name, repr, value) =>
        s"$name: ${repr.label} = ${repr.asInstanceOf[Repr[Any]].repr(value)}"
      }
    s"$typeLabel(${args.mkString(", ")})"
  override def label: String = typeLabel
}
```

Now the mechanism:
- Extracts field labels from `MirroredElemLabels` at compile time (not hardcoded strings)
- Resolves a `Repr` instance per element type via `summonReprs` (not assuming `Int`)
- Zips names, instances, and runtime values together to format the output

Now `Bar(1, 2)` works, `Baz(1, Bar(2, 3))` works, and any product type works. Nested products — where one field is itself a case class — work for free: the same mechanism simply resolves the inner type's derived `Repr` instead of a primitive one.

## Sum Derivation

The first sum test introduced `Option[Boolean]`:

```scala
given Repr[Option[Boolean]] = Repr.derived
Some(true).repr shouldBe "Some(value: Boolean = true)"
None.repr shouldBe "None()"
```

### Step 1 — trivial hardcoded solution

Both product and sum derivation started with hardcoded, type-specific implementations. This phase established the pattern before generalizing.

**For products**, the hardcoded approach handled only `Bar`:

```scala
private def productRepr[T](typeLabel: String): Repr[T] = new Repr[T] {
  override def repr(t: T): String =
    // Hardcoded: Bar(n, m) with Int fields
    val argValues = t.asInstanceOf[Product].productIterator.toList
    s"$typeLabel(n: Int = ${argValues(0)}, m: Int = ${argValues(1)})"
  override def label: String = typeLabel
}
```

**For sums**, the hardcoded approach handled only `Option[Boolean]`:

```scala
private def sumRepr[T](typeLabel: String): Repr[T] =
  new Repr[T] {
    override def repr(t: T): String = t match
      case Some(v) => s"Some(value: Boolean = $v)"
      case None    => "None()"
    override def label: String = typeLabel
  }
```

Both implementations were deliberately concrete. The product version hardcoded field names and assumed `Int` types. The sum version hardcoded variant names and assumed `Boolean` values. Neither could handle variations — adding a second field to `Bar`, or deriving `Some[String]`, would require editing the code itself.

This brittleness was the signal to generalize. The commit messages show the pattern: "hardcoded agrLabels as Int to make the test green" and "Trivial implementation: sumRepr to correctly handle Option cases". The duplication between test expectations and code was the intentional redundancy that TDD uses to drive refactoring.

Once both tests passed with their hardcoded implementations, the next steps moved toward generalization — one at a time.

### Step 2 — move to ordinal dispatch; challenge: missing Repr for subtypes

The design was evolved so that `sumRepr` would work generically for any sum type by dispatching through the ordinal of the active variant:

```scala
private def sumRepr[T](
    typeLabel: String,
    s: Mirror.SumOf[T],
    reprs: => List[Repr[?]]
  ): Repr[T] = new Repr[T] {
  override def repr(t: T): String =
    reprs(s.ordinal(t)).asInstanceOf[Repr[Any]].repr(t)
  override def label: String = typeLabel
}
```

For this to work, `summonReprs` had to recursively produce a `Repr` for every element in the tuple of subtypes (for `Option[Boolean]`: `Some[Boolean]` and `None.type`). The first attempt looked like this:

```scala
private inline def summonReprs[T <: Tuple]: List[Repr[?]] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (elem *: elems) =>
      summonInline[Repr[elem]] :: summonReprs[elems]
```

This compiled for `Bar` (field types are primitives with explicit instances), but failed when deriving `Repr[Option[Boolean]]`:

```
No given instance of type progmeta.Repr[Some[Boolean]] was found
```

`Some[Boolean]` and `None.type` are not primitives. They are themselves derivable types, but no one had derived them explicitly. The single-pass `summonInline` had no way to derive them on the spot.

### Step 3 — add Mirror fallback; challenge: ambiguous given instances

The natural fix was to fall back to `Mirror`-based derivation when no explicit `Repr[elem]` was in scope. In full recursive form:

```scala
private inline def summonReprs[T <: Tuple]: List[Repr[?]] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (elem *: elems) =>
      val head = summonFrom {
        case repr: Repr[elem]   => repr
        case m: Mirror.Of[elem] => derived(using m)
      }
      head :: summonReprs[elems]
```

This resolved the missing-instance problem. But a new test was then added that derived `Repr[Some[Boolean]]` and `Repr[None.type]` as explicit local givens:

```scala
test("Repr for Some") {
  given R: Repr[Some[Boolean]] = Repr.derived
  given N: Repr[None.type]     = Repr.derived
  R.label shouldBe "Some"
  N.label shouldBe "None"
}
```

When the compiler tried to compile `given R: Repr[Some[Boolean]] = Repr.derived`, it expanded `summonReprs` inline and reached the `summonFrom` branch. At that point `elem` was still abstract — a type variable, not yet resolved to a concrete type. With both `R: Repr[Some[Boolean]]` and `N: Repr[None.type]` visible in the outer scope, the implicit search found two candidates for `Repr[elem]` and could not choose:

```
[E172] Type Error: ReprSpec.scala:39:35
Ambiguous given instances: both given instance N and given instance R
match type progmeta.Repr[elem]
```

The fundamental issue: `case repr: Repr[elem]` inside an inline lambda sees the full surrounding scope, and when `elem` is still abstract, any `Repr[X]` in scope is a candidate match.

### Step 4 — final solution: resolve each element with a concrete type

The fix was to extract element resolution into a separate `private inline def` that is called with a concrete type at each inline expansion site.

Before (ambiguous because `elem` is still abstract):

```scala
private inline def summonReprs[T <: Tuple]: List[Repr[?]] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (elem *: elems) =>
      val head = summonFrom {
        case repr: Repr[elem]   => repr     // <- elem still abstract here
        case m: Mirror.Of[elem] => derived(using m)
      }
      head :: summonReprs[elems]
```

After (each element is resolved through `sumRepr[Elem]` with a concrete type):

```scala
private inline def summonReprs[T <: Tuple]: List[Repr[?]] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (elem *: elems) =>
      sumRepr[elem] :: summonReprs[elems]

private inline def sumRepr[Elem]: Repr[Elem] =
  summonFrom {
    case r: Repr[Elem]      => r          // <- Elem is concrete here
    case m: Mirror.Of[Elem] => Repr.derived[Elem]
  }
```

Because `sumRepr[elem]` is inlined independently for each concrete `elem` (e.g., `Some[Boolean]`, then `None.type`, then `Boolean`), the implicit search inside `sumRepr` sees only one matching `Repr` per concrete type — the ambiguity disappears.

### Refactor: label was added, removed, then added back

During this phase, a `label` method was added to the `Repr` trait, then removed after it appeared unused, then added back when a test checking `R.label shouldBe "Option"` proved it was in fact needed. That cycle — add, remove, re-add — is not noise. It shows the design being honest about what is actually required versus what was assumed to be required. The tests were the authority, not the designer's intuition.

## Recursion and Refactoring

### Challenge: infinite recursion with Lst

The most revealing step was adding a recursive enum:

```scala
enum Lst[+T] derives Repr:
  case Cns(t: T, ts: Lst[T])
  case Nl
```

The test failed — not with a wrong assertion, but with what appeared to be infinite recursion during derivation. The commit message was explicit: "Test fails. This looks like infinite recursion (how to terminate recursion?)".

The root cause was that `summonReprs` resolved all element `Repr` instances eagerly at derivation time. For `Cns[T]`, one of the elements is `Lst[T]` — the same type being derived. Resolving `Repr[Lst[T]]` triggered `Repr.derived[Lst[T]]` again before the first derivation had finished, looping indefinitely.

### Solution: make reprs lazy

The fix was a single keyword change:

```scala
// before: eager — triggers infinite recursion for recursive types
val reprs = summonReprs[m.MirroredElemTypes]

// after: deferred until repr() is actually called at runtime
lazy val reprs = summonReprs[m.MirroredElemTypes]
```

Making `reprs` lazy broke the derivation-time recursion cycle. The `Lst` tests passed. The same approach immediately worked for `List`, which follows the same recursive sum-of-products structure.

### Challenge: removing lazy introduced a compiler warning

After the recursion fix, a refactoring step tried to remove laziness from a different part of the evaluation chain. The commit message captured the problem as it happened: "Remove lazy evaluation for reprs in derived. This causes warning". The warning was a sign that the compiler could see unnecessary lazy wrapping that would never be evaluated in the same way as expected.

### Solution: fix warning placement

The warning was resolved by finding the correct placement for the lazy evaluation — not at the call site where it was originally moved, but at the boundary where recursion was actually triggered. The final shape kept `lazy val reprs` in `derived`, which is the point closest to where cyclic resolution can occur.

```scala
// after: keep laziness at the derivation boundary where recursive graphs are built
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

This back-and-forth is typical of metaprogramming work: evaluation order matters in ways that are not immediately visible from the types alone. Compiler feedback — warnings included — was part of finding the right answer.

## Summary

The `Repr` TDD story is a progression from a tiny derivation experiment to a recursive representation utility. Each major step was driven by a failing test or a compiler error, not by up-front design.

| Stage | Challenge | Solution |
|---|---|---|
| Bootstrap | `Foo derives Repr` must compile, then `Foo().repr` must be callable in a test | Add a minimal `derived` hook first, then a trivial implementation plus the `repr` extension |
| Sums — first pass | `Option[Boolean]` needs explicit `Some(...)` / `None()` output | Split `derived` into product/sum branches and use a concrete `sumRepr` first |
| Products | `[E182]` compiler error when `Bar derives Repr` | Hardcode first, then generalize with `MirroredElemLabels` and primitive instances |
| Sums — step 1 | Hardcoded `sumRepr` only worked for `Option[Boolean]` | Move to ordinal dispatch with precomputed subtype reprs |
| Sums — step 2 | `No given instance of type Repr[Some[Boolean]] was found` | Add `Mirror`-based fallback in `summonFrom` |
| Sums — step 3 | `[E172] Ambiguous given instances` — abstract `elem` matched too broadly | Extract `private inline def sumRepr[Elem]` so implicit search sees a concrete type |
| Labels | `label` removed as unused, tests later proved it required | Re-add only when a test makes the requirement explicit |
| Recursion | Infinite recursion when deriving `Lst` | Make `reprs` lazy to defer resolution until runtime |
| Warnings | Moving laziness caused a compiler warning | Keep `lazy val reprs` at the correct boundary in `derived` |

The most important lesson is that each challenge surfaced only when a test tried to express new behavior. The design was never ahead of the tests — it was always catching up to them.

---

Note: this article is maintained as a dedicated topic file under `docs/articles/` and follows the plan in `docs/plans/repr.plan.md`.

