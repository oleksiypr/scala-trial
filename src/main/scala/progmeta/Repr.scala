package progmeta

import scala.deriving.Mirror
import scala.compiletime.{constValue, constValueTuple, summonFrom, erasedValue, summonInline}

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

  inline def derived[T](using m: Mirror.Of[T]): Repr[T] =
    val label     = constValue[m.MirroredLabel]
    val argNames  = constValueTuple[m.MirroredElemLabels].toList.map(_.toString)
    val agrLabels = argNames.map(_ => "Int")
    inline m match
      case _: Mirror.ProductOf[T] => productRepr[T](label, argNames, agrLabels)
      case _: Mirror.Of[T]        => sumRepr[T](label)

  private def productRepr[T](typeLabel: String, argNames: List[String], agrLabels: List[String]): Repr[T] =
    new Repr[T] {
      override def repr(t: T): String = 
        val argValues = t.asInstanceOf[Product].productIterator.toList
        val args = argNames.lazyZip(agrLabels).lazyZip(argValues)
          .map((name, label, value) => s"$name: $label = $value").mkString(", ")
        s"$typeLabel($args)"
      override def label: String = typeLabel
    }

  private def sumRepr[T](typeLabel: String): Repr[T] =
    new Repr[T] {
      override def repr(t: T): String = t match
        case Some(v) => s"Some(value: Boolean = $v)"
        case None    => "None()"
      override def label: String = typeLabel
    }
}
