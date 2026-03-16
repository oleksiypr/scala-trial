package progmeta

import scala.compiletime.{constValue, constValueTuple, erasedValue, summonFrom}
import scala.deriving.Mirror

trait Repr[T] {
  def repr(t: T): String
  def label: String
}

object Repr {

  extension [T](t: T) {
    def repr(using r: Repr[T]): String = r.repr(t)
  }

  given Repr[Int] with
    override def repr(t: Int): String = t.toString
    override def label: String = "Int"

  given Repr[Double] with
    override def repr(t: Double): String = t.toString
    override def label: String = "Double"

  given Repr[Boolean] with
    override def repr(t: Boolean): String = t.toString
    override def label: String = "Boolean"
    
  given Repr[String] with
    override def repr(t: String): String = t
    override def label: String = "String"

  inline def derived[T](using m: Mirror.Of[T]): Repr[T] =
    val label    = constValue[m.MirroredLabel]
    val reprs = summonReprs[m.MirroredElemTypes]
    inline m match
      case _: Mirror.ProductOf[T] =>
        val argNames = constValueTuple[m.MirroredElemLabels].toList.map(_.toString)
        productRepr[T](label, argNames, reprs)
      case s: Mirror.SumOf[T] =>
        sumRepr[T](label, s, reprs)

  private def productRepr[T](typeLabel: String, argNames: List[String], reprs: => List[Repr[?]]): Repr[T] =
    new Repr[T] {
      override def repr(t: T): String =
        val argValues = t.asInstanceOf[Product].productIterator.toList
        val args = argNames.lazyZip(reprs).lazyZip(argValues)
          .map { (name, repr, value) =>
            s"$name: ${repr.label} = ${repr.asInstanceOf[Repr[Any]].repr(value)}"
          }
        s"$typeLabel(${args.mkString(", ")})"
      override def label: String = typeLabel
    }

  private def sumRepr[T](typeLabel: String, s: Mirror.SumOf[T], reprs: => List[Repr[?]]): Repr[T] =
    new Repr[T] {
      override def repr(t: T): String =
        reprs(s.ordinal(t)).asInstanceOf[Repr[Any]].repr(t)

      override def label: String = typeLabel
    }

  private inline def summonReprs[T <: Tuple]: List[Repr[?]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (elem *: elems)  => sumRepr[elem] :: summonReprs[elems]

  private inline def sumRepr[Elem]: Repr[Elem] =
    summonFrom {
      case r: Repr[Elem]      => r
      case m: Mirror.Of[Elem] => derived[Elem]
    }
}
