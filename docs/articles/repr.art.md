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

At that stage both branches were still hardcoded. The product branch returned the old `"Foo()"` behavior, while the sum branch returned the new `Option`-specific string. The key step was not generality yet — it was teaching `derived[T]` to distinguish products from sums.

After bootstrap, the next stages are no longer bootstrap:
- sum-type development (`Option`) comes next and is described in `## Sum Derivation`
- product evolution (`Bar`, then nested products) follows and is described in `## Product Derivation`


### Development process at a glance (after the bootstrap step)

1. **Product: several parameters**


```scala
case class Bar(n: Int, m: Int) derives Repr
Bar(1, 2).repr shouldBe "Bar(n: Int = 1, m: Int = 2)"
```

After sums, let's back to products. Now we want a labeled product parameter list instead of  `()`. Assume all parameters are of `Int` type:

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

I hardcoded the field names (`n`, `m`) and field type (`Int`) because that was the smallest change that turned the test green. It was not meant to be final — it was just the fastest way to confirm the output shape I wanted.

The next important change from Case 2 is further evolution of `derived[T]`. It already knew how to distinguish products from sums.
Now, let's gether product metadata and pass it into `productRepr` instead of hardcoded (`n`, `m`).

To do this, we need two more meta-features that provide compile-type infornation: `m.MirroredElemLabels` and `constValueTuple`.
Unlike `constValue` which is apply-time constant of a singleton type, `constValueTuple` can extract a tuple of constant values from a tuple type. This allows us to get the field names as a tuple of strings at compile time.

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

There was one failed attempt before this green version. I first tried to get the argument type labels from `m.MirroredElemTypes`:

```scala
val agrLabels = constValueTuple[m.MirroredElemTypes].toList.map(_.toString)
```

That did not compile for `Bar(n: Int, m: Int)`: `Int` is not a constant type, so it cannot be extracted with `constValueTuple` that way. To keep moving, I replaced it with the smallest working approximation: treat every field label as `"Int"`. That was enough for the current test and made the first product step green.

2. **Nested products: `Baz(n: Int, bar: Bar)` — replace hardcoded `"Int"` with `summonReprs`**

```scala
case class Baz(n: Int, bar: Bar) derives Repr

test("Repr for Baz(1, Bar(2, 3))") {
  val baz = Baz(1, Bar(2, 3))
  baz.repr shouldBe "Baz(n: Int = 1, bar: Bar = Bar(n: Int = 2, m: Int = 3))"
}
```

`Baz` has a `bar: Bar` field. The hardcoded `agrLabels = argNames.map(_ => "Int")` approach could not handle this — it would produce `bar: Int = ...` instead of `bar: Bar = Bar(...)`. I needed to resolve the actual `Repr` for each field type at compile time.

I replaced the hardcoded type labels with `summonReprs[m.MirroredElemTypes]` inside the product branch, and added primitive `Repr` givens so the implicit search had something to find:

```scala
given Repr[Int] with
  override def repr(t: Int): String = t.toString
  override def label: String = "Int"

private inline def summonReprs[T <: Tuple]: List[Repr[?]] =
  inline erasedValue[T] match
    case _: EmptyTuple          => Nil
    case _: (elem *: elems)     => summonInline[Repr[elem]] :: summonReprs[elems]
```
It walks the field type tuple at compile time using `summonInline[Repr[elem]]` for each element. For `Int` it finds `given Repr[Int]`. For `Bar` it finds `Repr[Bar]` because `Bar derives Repr` causes the compiler to synthesize a `given Repr[Bar]` via `Repr.derived` — so `summonInline` can locate it. This is what made `Baz` work: the `bar: Bar` field gets its own recursively derived `Repr`, and the label comes from `Repr[Bar].label` rather than a hardcoded string.


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
At this point, the concrete `Option` pattern match is already green, so I keep it as a safe baseline and then generalize step by step. Instead of hardcoding `Some` and `None`, I precompute subtype representations with `summonReprs[m.MirroredElemTypes]` and let `Mirror.SumOf` tell me which subtype is active via `ordinal`. That way, a runtime value maps to the correct subtype index, and I can dispatch through `reprs` without writing case-by-case pattern matches. The final wiring is to compute `reprs` once in `derived[T]` and share it between both product and sum branches.


```scala
// concrete green
case Some(v) => s"Some(value: Boolean = $v)"
case None    => "None()"

// generic sum dispatch
reprs(sum.ordinal(t)).asInstanceOf[Repr[Any]].repr(t)
```
The key to the generic solution is `s.ordinal(t)`, `sum: Mirror.SumOf[T]` — a compile-time description of a sum type. Its `ordinal` method is the runtime side of that description: given a value `t`, it returns the index of the active case in the sum. For `Option[Boolean]`, `Some(true)` has ordinal `0` and `None` has ordinal `1`. That index selects the right `Repr` from `reprs`. No pattern match on `Some` or `None` is needed — the mirror knows the structure, and `ordinal` maps any value to its position in it.

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

The first attempt failure at this stage: moving `reprs` to the top of `derived[T]` exposed a compiler error for sums :

```
No given instance of type progmeta.Repr[Some[Boolean]] was found
```

Here `summonReprs` still used a single-pass `summonInline`, which could resolve primitives and prducts for `Mirror.ProductOf` givens but could not derive sum subtypes like `Some[Boolean]` / `None.type` on the spot becasue we are in `Mirror.SumOf[T]` case while `Some` is a product.

Thus, we need to resolve the `Repr` for each subtype case recursively.

In other words `Some[Boolean]` and `None.type` are themselves derivable types of `Mirror.ProductOf`, hence we need to reuse the dervication we already did for `Bar` and `Baz`.

The next step introduces a `summonFrom` fallback to close this gap.

### Step 2 — add Mirror fallback; challenge: ambiguous given instances

The natural fix was to fall back to `Mirror`-based derivation when no explicit `Repr[elem]` was in scope. In full recursive form:

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

This resolved the missing-instance problem. The test, however, fails for:

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

The fundamental issue: `case repr: Repr[elem]` inside an inline lambda sees the full surrounding scope, and when `elem` is still abstract, any `Repr[X]` in scope is a candidate match.

### Step 3 — final solution: resolve each element with a concrete type

The fix is to extract element resolution into a separate `private inline def` that is called with a concrete type at each inline expansion site.

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

Because `sumRepr[elem]` is inlined independently for each concrete `elem` (e.g., `Some[Boolean]`, then `None.type`, then `Boolean`), the implicit search inside `sumRepr` sees only one matching `Repr` per concrete type — the ambiguity disappears.

This closed the missing-instance gap (`Some[Boolean]` / `None.type` can now be derived). The following step then refines this further to avoid ambiguous givens by resolving each element through a concrete helper type parameter.

The method recursively resolves a `Repr` instance for every element type in the mirror's element tuple. For a product like `Bar`, those are the field types (`Int`, `Int`). For a sum like `Option[Boolean]`, those are the subtype cases (`Some[Boolean]`, `None.type`). The result is a `List[Repr[?]]` indexed by position — exactly what `s.ordinal(t)` indexes into at runtime.



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

Compiler produces a warning:

```bash
[warn] -- Warning: ... ReprSpec.scala: ...
[warn] 13 |  enum Lst[+T] derives Repr:
[warn]    |                       ^
[warn]    |Infinite loop in function body
[warn]    |{
```

The test, however, fails with stack overflow due to infinite recursion in the derivation process:

```bash
An exception or error caused a run to abort. 
java.lang.StackOverflowError
	at progmeta.ReprSpec$Lst$.derived$Repr(ReprSpec.scala:13)
	at progmeta.ReprSpec$Lst$.derived$Repr(ReprSpec.scala:13)
	...
```

The root cause was that `summonReprs` resolved all element `Repr` instances eagerly at derivation time. For `Cns[T]`, one of the elements is `Lst[T]` — the same type being derived. Resolving `Repr[Lst[T]]` triggered `Repr.derived[Lst[T]]` again before the first derivation had finished, looping indefinitely.

### Solution: make reprs lazy

The fix was a single keyword change:

```scala
inline def derived[T](using m: Mirror.Of[T]): Repr[T] =
  ...
  lazy val reprs = summonReprs[m.MirroredElemTypes]
  ...
```

Making `reprs` lazy broke the derivation-time recursion cycle. The warning was resolved by finding the correct placement for the lazy evaluation — not at the call site where it was originally moved, but at the boundary where recursion was actually triggered. The final shape kept `lazy val reprs` in `derived`, which is the point closest to where cyclic resolution can occur.
Unfortunately, the test still fails.

The key point: `lazy val` + `=> T` (by-name) form a pair. `lazy val` defers construction, by-name defers consumption. Both are needed: if `reprs` is lazy but passed eagerly, the laziness is immediately collapsed at the call site. The cycle is only truly broken when both sides defer.

```scala
private def productRepr[T]( ..., reprs: => List[Repr[?]]): Repr[T] = ...
private def sumRepr[T]( ..., reprs: => List[Repr[?]]): Repr[T] = ...   
```

Now it passes!


The same approach immediately workes for `List`, which follows the same recursive sum-of-products structure.

```scala
  test("Repr for List") {
    given R: Repr[List[Int]] = Repr.derived
    R.label shouldBe "List"
    R.repr(Nil) shouldBe "Nil()"
    R.repr(List(1)) shouldBe "::(head: Int = 1, next: List = Nil())"
    R.repr(List(1, 2)) shouldBe "::(head: Int = 1, next: List = ::(head: Int = 2, next: List = Nil()))"
  }
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
