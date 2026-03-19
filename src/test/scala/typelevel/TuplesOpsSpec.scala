package typelevel

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class TuplesOpsSpec extends AnyFunSuite with Matchers {

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

  test("Type class mapping") {
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
          val head = zipped.head.asInstanceOf[(h, Ev[h])]
          val tail = zipped.tail.asInstanceOf[WithEvs[t]]
          val (v, ev) = head
          (v, ev.foo(v)) *: eval[t](tail)

    eval[MyTuple](zipped) shouldBe
      ((1, "ev(1: Int)"), ("hi", "ev(hi: String)"), (true, "ev(true: Boolean)"))
  }

}
