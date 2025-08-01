package progfun

object Excercise extends App {

  type FunSet[A] = A => Boolean

  def empty[A]: FunSet[A] = (x: A) => false

  def pure[A](a: A): FunSet[A] = (x: A) => ???

  def union[A](s1: FunSet[A], s2: FunSet[A]): FunSet[A] = ???

  def intersect[A](s1: FunSet[A], s2: FunSet[A]): FunSet[A] = ???

  val twoItems = union(pure(1), pure(2))
  val S = union(twoItems, pure(3))

  println("Set as a function: ")
  println("Is 1 in {1, 2, 3} ? " + S(1))
  println("Is 2 in {1, 2, 3} ? " + S(2))
  println("Is 3 in {1, 2, 3} ? " + S(3))
  println("Is 0 in {1, 2, 3} ? " + S(0))
  println("Is 4 in {1, 2, 3} ? " + S(4))
  println("x belong to empty: " + empty[Int](3))
  println()

}
