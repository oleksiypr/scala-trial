package progmeta

import scala.compiletime.{constValue, constValueTuple, summonFrom, erasedValue, summonInline}
import scala.deriving.Mirror

trait Repr[T] {
  def repr(t: T): String
  def label: String
}

object Repr {

  def apply[T](using repr: Repr[T]): Repr[T] = repr

  extension [T] (t: T) {
    def repr(using r: Repr[T]): String = r.repr(t)
  }

  inline given derived[T <: Product](
      using m: Mirror.ProductOf[T]
    ): Repr[T] = new Repr[T] {
    override def repr(t: T): String = Repr.repr(t)
    override def label: String = Repr.label
  }

  given Repr[Int] with
    override def repr(t: Int): String = t.toString
    override def label: String = "Int"

  given Repr[Double] with
    override def repr(t: Double): String = t.toString
    override def label: String = "Double"
  
  given Repr[String] with
    override def repr(t: String): String = t
    override def label: String = "String"
    
  given Repr[Boolean] with 
    override def repr(t: Boolean): String = t.toString
    override def label: String = "Boolean"

  inline def repr[T <: Product](t: T)(using m: Mirror.ProductOf[T]): String =
    val className = label[T]
    val argNames  = elementLabels[T]
    val agrValues = t.productIterator.toList
    val agrReprs  = summonReprs[m.MirroredElemTypes]

    val argRepr = argNames.lazyZip(agrValues).lazyZip(agrReprs).map {
      (name, value, repr) =>
        s"$name: ${repr.label} = ${repr.asInstanceOf[Repr[Any]].repr(value)}"
    }
    s"$className(${argRepr.mkString(", ")})"

  inline def label[T](using m: Mirror.Of[T]): String =
    constValue[m.MirroredLabel]

  inline def summonReprs[Tup <: Tuple]: List[Repr[?]] =
    inline erasedValue[Tup] match
      case _: EmptyTuple      => Nil
      case _: (elem *: elems) => summonInline[Repr[elem]] :: summonReprs[elems]
  
  inline def elementLabels[T](using m: Mirror.Of[T]): List[String] =
    constValueTuple[m.MirroredElemLabels].toList.map(_.toString)

}
