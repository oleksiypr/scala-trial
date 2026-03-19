# Repr Derivation (Products + Sums): TDD Process Description

This article documents the iterative, test-driven development (TDD) process for building `Repr` derivation for product and sum types, based strictly on local git history of `src/main/scala/progmeta/Repr.scala` and `src/test/scala/progmeta/ReprSpec.scala`.

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
test("Repr for Sum type") {
  Bar(1, 2).repr shouldBe "Bar(n: Int = 1, m: Int = 2)"

  given Repr[Option[Boolean]] = Repr.derived
  Some(true).repr shouldBe "Some(value: Boolean = true)"
  None.repr shouldBe "None()"
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

The older `Repr` implementation had grown organically as part of earlier metaprogramming experiments. Sum type support was attempted on that old implementation, but the approach did not hold up. The commit message said simply "Reverted Sum type implementation" — a clear signal that something fundamental was wrong, not just a minor fix away. Rather than continue patching, the decision was made to rename the existing code to `ReprOld` and start fresh from scratch on Scala 3 `Mirror`.

The starting point was deliberately minimal. The only goal of the first step was to make this compile:

```scala
case class Foo() derives Repr
```

Nothing more. No output format, no field labels, no recursive support. Just a type class that the compiler could accept as a derivation target. Once that compiled, a `repr` extension method was added so that `foo.repr` was actually callable — turning the compiler experiment into something a test could assert against.

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

For this to work, `summonReprs` had to produce a `Repr` for each subtype of `Option[Boolean]` — meaning it needed `Repr[Some[Boolean]]` and `Repr[None.type]`. The first attempt used `summonInline` directly:

```scala
case _: (elem *: elems) => summonInline[Repr[elem]] :: summonReprs[elems]
```

This compiled for `Bar` (field types are primitives with explicit instances), but failed when deriving `Repr[Option[Boolean]]`:

```
No given instance of type progmeta.Repr[Some[Boolean]] was found
```

`Some[Boolean]` and `None.type` are not primitives. They are themselves derivable types, but no one had derived them explicitly. The single-pass `summonInline` had no way to derive them on the spot.

### Step 3 — add Mirror fallback; challenge: ambiguous given instances

The natural fix was to fall back to `Mirror`-based derivation when no explicit `Repr[elem]` was in scope:

```scala
case _: (elem *: elems) =>
  val head = summonFrom {
    case repr: Repr[elem]       => repr
    case m: Mirror.Of[elem]     => derived(using m)
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

The fix was to extract element resolution into a separate `private inline def` that is called with a concrete type at each inline expansion site:

```scala
// before: elem is abstract inside the lambda — implicit search is too broad
case _: (elem *: elems) =>
  val head = summonFrom {
    case repr: Repr[elem]   => repr     // ← elem still abstract here
    case m: Mirror.Of[elem] => derived(using m)
  }
  head :: summonReprs[elems]

// after: sumRepr[elem] is inlined separately for each concrete elem
case _: (elem *: elems) => sumRepr[elem] :: summonReprs[elems]

private inline def sumRepr[Elem]: Repr[Elem] =
  summonFrom {
    case r: Repr[Elem]      => r          // ← Elem is concrete here
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

This back-and-forth is typical of metaprogramming work: evaluation order matters in ways that are not immediately visible from the types alone. Compiler feedback — warnings included — was part of finding the right answer.

## Summary

The `Repr` TDD story is a progression from a tiny derivation experiment to a recursive representation utility. Each major step was driven by a failing test or a compiler error, not by up-front design.

| Stage | Challenge | Solution |
|---|---|---|
| Bootstrap | Old sum type attempt reverted; clean restart needed | Rename to `ReprOld`, derive from scratch |
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

