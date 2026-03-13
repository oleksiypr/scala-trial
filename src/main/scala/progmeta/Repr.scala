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
    val label = constValue[m.MirroredLabel]
    inline m match
      case _: Mirror.ProductOf[T] => productRepr[T](label)
      case _: Mirror.Of[T]        => sumRepr[T](label)

  private def productRepr[T](typeLabel: String): Repr[T] =
    new Repr[T] {
      override def repr(t: T): String = s"$typeLabel()"
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
