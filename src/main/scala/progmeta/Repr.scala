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
      case mp: Mirror.ProductOf[T] => productRepr[T](using mp)
      case _: Mirror.Of[T]         => sumRepr[T]
  
  private def productRepr[T](using mp: Mirror.ProductOf[T]): Repr[T] =
    new Repr[T] {
      override def repr(t: T): String = "Foo()"
      override def label: String = "Foo"
    }

  private def sumRepr[T]: Repr[T] =
    new Repr[T] {
      override def repr(t: T): String = "Some(value: Boolean = true)"
      override def label: String = "Option"
    }
}
