package progmeta

import scala.deriving.Mirror

trait Repr[T] {
  def repr(t: T): String
  def label: String
}


object Repr {
  extension [T](t: T) {
    def repr(using r: Repr[T]): String = r.repr(t)
  }

  inline def derived[T](using m: Mirror.Of[T]): Repr[T] =
    new Repr[T] {
      override def repr(t: T): String = t.toString
      override def label: String = m.toString
    }
}
