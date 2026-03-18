package progmeta

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers


object ReprSpec {
  case class Foo() derives Repr
  case class Bar(n: Int, m: Int) derives Repr
  case class Baz(n: Int, bar: Bar) derives Repr
  case class Qux(s: Option[String]) derives Repr

  enum Lst[+T] derives Repr:
    case Cns(t: T, ts: Lst[T])
    case Nl
}

class ReprSpec extends AnyFunSuite with Matchers {
  import ReprSpec.*
  import Repr.*

  test("Repr for Foo()") {
    val foo = Foo()
    foo.repr shouldBe "Foo()"
  }

  test("Repr for Sum type") {
    val a: Option[Boolean] = Some(true)
    given R: Repr[Option[Boolean]] = Repr.derived

    a.repr shouldBe "Some(value: Boolean = true)"
    R.label shouldBe "Option"

    val b: Option[Boolean] = None
    b.repr shouldBe "None()"
  }

  test("Repr for Bar(1, 2)") {
    val bar = Bar(1, 2)
    bar.repr shouldBe "Bar(n: Int = 1, m: Int = 2)"
  }

  test("Repr for Some") {
    given R: Repr[Some[Boolean]] = Repr.derived
    given N: Repr[None.type ] = Repr.derived

    R.label shouldBe "Some"
    N.label shouldBe "None"
  }

  test("Repr for Baz(1, Bar(2, 3))") {
    val baz = Baz(1, Bar(2, 3))
    baz.repr shouldBe "Baz(n: Int = 1, bar: Bar = Bar(n: Int = 2, m: Int = 3))"
  }

  test("Repr for Qux(maybeString))") {
    Qux(Some("foo")).repr shouldBe "Qux(s: Option = Some(value: String = foo))"
    Qux(None).repr shouldBe "Qux(s: Option = None())"
  }

  test("Repr for Lst") {
    val empty: Lst[Int] = Lst.Nl
    val unit : Lst[Int] = Lst.Cns(1, Lst.Nl)
    val list : Lst[Int] = Lst.Cns(1, Lst.Cns(2, Lst.Nl))

    empty.repr shouldBe "Nl()"
    unit.repr shouldBe "Cns(t: Int = 1, ts: Lst = Nl())"
    list.repr shouldBe "Cns(t: Int = 1, ts: Lst = Cns(t: Int = 2, ts: Lst = Nl()))"
  }

  test("Repr for List") {
    given R: Repr[List[Int]] = Repr.derived
    R.repr(Nil) shouldBe "Nil()"
    R.repr(List(1)) shouldBe "::(head: Int = 1, next: List = Nil())"
    R.repr(List(1, 2)) shouldBe "::(head: Int = 1, next: List = ::(head: Int = 2, next: List = Nil()))"
  }

  test("Tuple mapping") {
    case class TupMap[T](value: T)

    type Mapped[F[_], T <: Tuple] = T match
      case EmptyTuple => EmptyTuple
      case h *: t     => F[h] *: Mapped[F, t]

    type MyTuple = (Int, String, Boolean)
    type MappedTuple = Mapped[TupMap, MyTuple]

    val tuple: MyTuple = (1, "hi", true)
    val mapped: MappedTuple = tuple.map([t] => t => TupMap[t](t))
    mapped shouldBe (TupMap(1), TupMap("hi"), TupMap(true))
    mapped(0) shouldBe TupMap(1)
    mapped(1) shouldBe TupMap("hi")
    mapped(2) shouldBe TupMap(true)
    def ord = 1
    mapped(ord) shouldBe TupMap("hi")
  }

  test("Typce class mapping") {
    trait Ev[T] {
      def foo(t: T): String
    }

    val evInt : Ev[Int]     = (t: Int)     => s"ev($t: Int)"
    val evStr : Ev[String]  = (t: String)  => s"ev($t: String)"
    val evBool: Ev[Boolean] = (t: Boolean) => s"ev($t: Boolean)"

    type MyTuple = (Int, String, Boolean)

    type Evs[Tup <: Tuple]     = Tuple.Map[Tup, Ev]
    type WithEvs[Tup <: Tuple] = Tuple.Zip[Tup, Evs[Tup]]

    val tuple: MyTuple = (1, "hi", true)
    val evs: Evs[MyTuple] = (evInt, evStr, evBool)

    val zipped: WithEvs[MyTuple] = tuple.zip(evs)

    def check[Tup1 <: Tuple, Tup2](using Tup1 =:= Tup2): Unit = ()

    check[
      Tuple.Map[MyTuple, [t] =>> (t, String)],
      ((Int, String), (String, String), (Boolean, String))
    ]

    check[
      WithEvs[MyTuple],
      ((Int, Ev[Int]), (String, Ev[String]), (Boolean, Ev[Boolean]))
    ]

    inline def eval[Tup <: Tuple](zipped: WithEvs[Tup]): Tuple.Map[Tup, [t] =>> (t, String)] =
      inline scala.compiletime.erasedValue[Tup] match
        case _: EmptyTuple => EmptyTuple
        case _: (h *: t) =>
          val head = zipped.asInstanceOf[(h, Ev[h]) *: WithEvs[t]].head
          val tail = zipped.asInstanceOf[(h, Ev[h]) *: WithEvs[t]].tail
          val (v, ev) = head
          (v, ev.foo(v)) *: eval[t](tail)

    eval[MyTuple](zipped) shouldBe
      ((1, "ev(1: Int)"), ("hi", "ev(hi: String)"), (true, "ev(true: Boolean)"))
  }
}
