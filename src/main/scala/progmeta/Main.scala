package progmeta

import cats.Id

import scala.compiletime.{constValue, constValueTuple}

object Main {

  @main def run(): Unit = {
    Debug.included(false) {
      val x = 10
      val y = 5
      Debug.debug(x + y)
      Debug.debug(x * y)
      Debug.debug(x / y)
      Debug.debug(math.pow(x, y))
      Debug.debug(4 + 7)
      Debug.debug(Foo(42))
      Debug.debug(Foo(42) + Foo(1))
    }

    Debug.included(false) {
      val x = 2.0
      square(x)
      qube(x)
    }

    Debug.included(false) {
      type Record = (Int, String, Boolean)

      type T[x] = x match
        case Int => 0
        case String => 1
        case Boolean => 2

      type TupleElems[t] = t match
        case EmptyTuple => EmptyTuple
        case h *: t => T[h] *: TupleElems[t]

      val ordinals = constValueTuple[TupleElems[Record]]
      println(ordinals)
    }

    Debug.included(false) {
      import scala.deriving.Mirror
      import scala.compiletime.*

      case class Baz(n: Int, s: String, b: Boolean)

      inline def names[T](using m: Mirror.ProductOf[T]): Unit = {
        // println(constValue[m.MirroredLabel])
        println(constValueTuple[m.MirroredElemLabels])
      }

      names[Baz]
    }

  }
}
