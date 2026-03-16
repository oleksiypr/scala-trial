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
    val reprs    = summonReprs[m.MirroredElemTypes]
    inline m match
      case _: Mirror.ProductOf[T] => productRepr[T](label, argNames, reprs)
      case _: Mirror.Of[T]        => sumRepr[T](label)

  private def productRepr[T](typeLabel: String, argNames: List[String], reprs: List[Repr[?]]): Repr[T] =
    new Repr[T] {
      override def repr(t: T): String = 
        val argValues = t.asInstanceOf[Product].productIterator.toList
        val args = argNames.lazyZip(reprs).lazyZip(argValues)
          .map { (name, repr, value) =>
            s"$name: ${repr.label} = ${repr.asInstanceOf[ReprOld[Any]].repr(value)}"
          }
        s"$typeLabel(${args.mkString(", ")})"
      override def label: String = typeLabel
    }

  private def sumRepr[T](typeLabel: String): Repr[T] =
    new Repr[T] {
      override def repr(t: T): String = t match
        case Some(v) => s"Some(value: Boolean = $v)"
        case None    => "None()"
      override def label: String = typeLabel
    }

  private inline def summonReprs[T <: Tuple]: List[Repr[?]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (elem *: elems)  => summonInline[Repr[elem]] :: summonReprs[elems]
}
