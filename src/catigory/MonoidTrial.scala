package catigory

import catigory.SemigroupTrial.Semigroup

object MonoidTrial extends App {

  def sumInts(list: List[Int]): Int = list.foldRight(0)(_ + _)

  def concatStrings(list: List[String]): String = list.foldRight("")(_ ++ _)

  def unionSets[A](list: List[Set[A]]): Set[A] = list.foldRight(Set.empty[A])(_ union _)

  trait Monoid[A] extends Semigroup[A] {
    def Z: A
  }

  object Monoid {
    def apply[T: Monoid]: Monoid[T] = {
      implicitly[Monoid[T]]
    }
  }

  def collapse[T: Monoid](list: List[T]): T = {
    val m = Monoid[T]
    list.foldRight(m.Z)(m.combine)
  }

  implicit object intMonoid extends Monoid[Int] {
    override def Z: Int = 0
    override def combine(a1: Int, a2: Int): Int = a1 + a2
  }

  implicit object stringMonoid extends Monoid[String] {
    override def Z: String = ""
    override def combine(a1: String, a2: String): String = a1 + a2
  }

  class SetMonoid[A] extends Monoid[Set[A]] {
    override def Z: Set[A] = Set.empty
    override def combine(a1: Set[A], a2: Set[A]): Set[A] = a1 union a2
  }

  implicit val intSetMonoid: Monoid[Set[Int]] = new SetMonoid[Int]

  def sumIntsMon[Int: Monoid](list: List[Int]): Int = collapse(list)

  def concatStringsMon[String: Monoid](list: List[String]): String = collapse(list)

  def unionSetsMon[A](list: List[Set[A]])(implicit ev: Monoid[Set[A]]): Set[A] = collapse(list)

  println(sumIntsMon(List(1, 2, 3)))
  println(concatStrings(List("Haskell", " ", "Brooks", " ", "Curry")))
  println(unionSetsMon(List(Set(1), Set(1, 2), Set(2, 3, 4))))
}
