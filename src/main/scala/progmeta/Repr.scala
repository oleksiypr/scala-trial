package progmeta

import scala.compiletime.{constValue, constValueTuple, summonFrom, erasedValue, summonInline}
import scala.deriving.Mirror

trait Repr[T] {
  def repr(t: T): String
}

object Repr {
  def apply[T](using repr: Repr[T]): Repr[T] = repr

  inline given derived[T](using m: Mirror.Of[T]): Repr[T] =
    (t: T) => "Foo(x: Int = 1)"
}
