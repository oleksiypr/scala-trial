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

  inline def derived[T](using m: Mirror.Of[T]): Repr[T] =
    val label    = constValue[m.MirroredLabel]
    val argNames = constValueTuple[m.MirroredElemLabels].toList.map(_.toString)
    inline m match
      case _: Mirror.ProductOf[T] => productRepr[T](label, argNames, "Int")
      case _: Mirror.Of[T]        => sumRepr[T](label)

  private def productRepr[T](typeLabel: String, argNames: List[String], argType: String): Repr[T] =
    new Repr[T] {
      override def repr(t: T): String = 
        val argValues = t.asInstanceOf[Product].productIterator.toList
        val args = argNames.zip(argValues).map((name, value) => s"$name: Int = $value").mkString(", ")
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
