package progmeta

import scala.deriving.Mirror

trait Repr[T] {
  def repr(t: T): String
  def label: String
}


object Repr {
  inline def derived[T](using m: Mirror.Of[T]): Repr[T] = ???
}
