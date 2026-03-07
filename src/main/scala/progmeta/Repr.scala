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

  inline def repr[T <: Product](t: T)(using m: Mirror.ProductOf[T]): String =
    val className = label[T]
    val argName   = elementLabels[T]
    val agrValue  = t.productIterator.toList
    val argRepr = argName.zip(agrValue).map {
      (name, value) => value match
        case i: Int => 
          val repr = Repr[Int]
          s"$name: ${repr.label} = ${i.repr}"
        case d: Double =>
          val repr = Repr[Double]
          s"$name: ${repr.label} = ${d.repr}"
    }

    s"$className(${argRepr.mkString(", ")})"


  inline def label[T](using m: Mirror.Of[T]): String =
    constValue[m.MirroredLabel]

  inline def elementLabels[T <: Product](using m: Mirror.ProductOf[T]): List[String] =
    constValueTuple[m.MirroredElemLabels].toList.map(_.toString)

}
