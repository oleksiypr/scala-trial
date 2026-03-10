package progmeta

import scala.compiletime.{constValue, constValueTuple, summonFrom, erasedValue, summonInline}
import scala.deriving.Mirror

trait ReprOld[T] {
  def repr(t: T): String
  def label: String
}

object ReprOld {

  def apply[T](using repr: ReprOld[T]): ReprOld[T] = repr

  extension [T] (t: T) {
    def repr(using r: ReprOld[T]): String = r.repr(t)
  }

  inline def derived[T](using m: Mirror.Of[T]): ReprOld[T] =
    inline m match 
      case p: Mirror.ProductOf[T] => new ReprOld[T] {
        override def repr(t: T): String = reprProduct(t)(using p)
        override def label: String = ReprOld.label[T]
      }
      case s: Mirror.SumOf[T] => new ReprOld[T] {
        override def repr(t: T): String = "Some(value: Boolean = true)"
        override def label: String = ReprOld.label[T]
      }

  given ReprOld[Int] with
    override def repr(t: Int): String = t.toString
    override def label: String = "Int"

  given ReprOld[Double] with
    override def repr(t: Double): String = t.toString
    override def label: String = "Double"
  
  given ReprOld[String] with
    override def repr(t: String): String = t
    override def label: String = "String"
    
  given ReprOld[Boolean] with 
    override def repr(t: Boolean): String = t.toString
    override def label: String = "Boolean"

  inline def reprProduct[T](t: T)(using m: Mirror.ProductOf[T]): String =
    val className = label[T]
    val argNames  = elementLabels[T]
    val agrValues = t.asInstanceOf[Product].productIterator.toList
    val agrReprs  = summonReprs[m.MirroredElemTypes]

    val argRepr = argNames.lazyZip(agrValues).lazyZip(agrReprs).map {
      (name, value, repr) =>
        s"$name: ${repr.label} = ${repr.asInstanceOf[ReprOld[Any]].repr(value)}"
    }
    s"$className(${argRepr.mkString(", ")})"

  inline def label[T](using m: Mirror.Of[T]): String =
    constValue[m.MirroredLabel]

  inline def summonReprs[Tup <: Tuple]: List[ReprOld[?]] =
    inline erasedValue[Tup] match
      case _: EmptyTuple      => Nil
      case _: (elem *: elems) => summonInline[ReprOld[elem]] :: summonReprs[elems]
  
  inline def elementLabels[T](using m: Mirror.Of[T]): List[String] =
    constValueTuple[m.MirroredElemLabels].toList.map(_.toString)

}
