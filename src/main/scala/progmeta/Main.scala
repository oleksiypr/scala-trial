package progmeta

import cats.{Eq, Id, Show}
import dotty.tools.backend.jvm.Primitives.Primitive

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
        case Int     => 0
        case String  => 1
        case Boolean => 2

      type TupleElems[t] = t match
        case EmptyTuple => EmptyTuple
        case h *: t     => T[h] *: TupleElems[t]

      val ordinals = constValueTuple[TupleElems[Record]]
      println(ordinals)
    }

    Debug.included(true) {
      import scala.deriving.Mirror
      import scala.compiletime.*

      case class Baz(n: Int, s: String, b: Boolean)
      case class Qux(d: Double, c: Baz)

      trait GetLabel[T]:
        def label: String

      given GetLabel[Int] with
        def label: String = "Int"

      given GetLabel[String] with
        def label: String = "String"

      given GetLabel[Boolean] with
        def label: String = "Boolean"

      given GetLabel[Double] with
        def label: String = "Double"

      inline def label[T <: Product](using m: Mirror.ProductOf[T]): String =
        constValue[m.MirroredLabel]

      inline def elementLabels[T <: Product](using m: Mirror.ProductOf[T]): List[String] =
        constValueTuple[m.MirroredElemLabels].toList.map(_.toString)

      inline def typeLabelsProduct[T <: Product](
          using m: Mirror.ProductOf[T]
        ): List[String] = typeLabelsTuple[m.MirroredElemTypes]

      inline def typeLabelsTuple[T <: Tuple]: List[String] =
        inline erasedValue[T] match
          case _: EmptyTuple      => Nil
          case _: (elem *: elems) => labelElem[elem] :: typeLabelsTuple[elems]

      inline def labelElem[Elem]: String =
        summonFrom {
          case m: Mirror.ProductOf[Elem] => constValue[m.MirroredLabel]
          case _                         => summonInline[GetLabel[Elem]].label
        }


      inline def describeClass[T <: Product](using m: Mirror.ProductOf[T]): String = {
        val className        = label[T]
        val elemLabels      = elementLabels[T]
        val typeLabels      = typeLabelsProduct[T]
        val labelsWithTypes = elemLabels.zip(typeLabels).map( (e, t) => s"$e: $t").mkString(", ")
        s"$className($labelsWithTypes)"
      }

      println(describeClass[Baz])
      println(describeClass[Qux])
    }
  }
}
