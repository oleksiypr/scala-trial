package progmeta

import scala.compiletime.{constValue, constValueTuple, summonFrom, erasedValue, summonInline}
import scala.deriving.Mirror

trait Repr[T] {
  def repr(t: T): String
}

object Repr {
  
  def apply[T](using repr: Repr[T]): Repr[T] = repr
  
  extension [T] (t: T) {
    def repr(using r: Repr[T]): String = r.repr(t)
  }
  
  inline given derived[T <: Product](
      using m: Mirror.ProductOf[T]
    ): Repr[T] = repr[T]
  
  inline def repr[T <: Product](t: T)(using m: Mirror.ProductOf[T]): String =
    val className = label[T]
    s"$className()"
    
  
  inline def label[T](using m: Mirror.Of[T]): String =
    constValue[m.MirroredLabel]


}
