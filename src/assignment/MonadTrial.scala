package assignment

import scala.language.higherKinds

object MonadTrial extends App {

  trait Monad[M[_]] {
    def unit[A](a: => A): M[A]
    def flatMap[X, Y](mx: M[X])(f: X => M[Y]): M[Y]
    def map[X, Y](mx: M[X])(f: X => Y): M[Y] = flatMap(mx) { x => unit(f(x)) }
  }

  implicit class MonadOps[X, M[_]](val c: M[X]) extends AnyVal {
    def flatMap[Y](f: X => M[Y])(implicit mc: Monad[M]): M[Y] = mc.flatMap(c)(f)
    def map[Y](f: X => Y)(implicit mc: Monad[M]): M[Y] = mc.map(c)(f)
  }

  implicit object optionMonad extends Monad[Option] {
    def unit[A](a: => A): Option[A] = Some(a)
    def flatMap[X, Y](m: Option[X])(f: X => Option[Y]): Option[Y] = m flatMap f
  }

  implicit object listMonad extends Monad[List] {
    def unit[A](a: => A): List[A] = List(a)
    def flatMap[X, Y](m: List[X])(f: X => List[Y]): List[Y] = m flatMap f
  }

  def aggregate[A, B, C,
      M1[_]: Monad, M2[_]: Monad
    ](
      s1: M1[A], s2: M2[B],
      f: (A, B) => C
    ): Unit = {

    val g: A => M2[C] = (a: A) => s2.map(b => f(a, b))
    //s1.flatMap((a: A) => g(a))
  }
}
