package catigory

import catigory.Functors.Cats.Functor
import scala.language.higherKinds

object ApplyTrial extends App {

  trait Apply[F[_]] extends Functor[F] { self =>

    def ap[A, B](f: F[A => B])(fa: F[A]): F[B]

    def compose[G[_]](g: Apply[G]): Apply[
      ({type C[X] = F[G[X]]})#C
      ] = {
      new Apply[({type C[X] = F[G[X]]})#C] {

        override def ap[A, B](f: F[G[A => B]])(fa: F[G[A]]): F[G[B]] =
          self.map2(f, fa){ (v, va) =>
            g.ap(v)(va)
          }

        override def map[A, B](fa: F[G[A]])(f: A => B): F[G[B]] =
          self.map(fa)(a => g.map(a)(f))
      }
    }

    def product2[A, B](fa: F[A], fb: F[B]): F[(A, B)] = {
      ap(map(fa)((a: A) => (b: B) => (a, b)))(fb)
    }

    def product3[A, B, C](fa: F[A], fb: F[B], fc: F[C]): F[(A, B, C)] = {
      val p2: F[(A, B)] = ap(map(fa)((a: A) => (b: B) => (a, b)))(fb)
      ap(map(p2)(r => (c: C) => (r._1, r._2, c)))(fc)
    }

    def ap2[A1, A2, B](f: F[(A1, A2) => B])(
      fa1: F[A1], fa2: F[A2]
    ): F[B] = ap(
      map(f)(fa => (a: (A1, A2)) => fa(a._1, a._2))
    ) {
      product2(fa1, fa2)
    }

    def ap3[A1, A2, A3, B](f: F[(A1, A2, A3) => B])(
      fa1: F[A1], fa2: F[A2], fa3: F[A3]
    ): F[B] = ap(
      map(f)(fa => (a: (A1, A2, A3)) => fa(a._1, a._2, a._3))
    ) {
      product3(fa1, fa2, fa3)
    }

    def map2[A1, A2, B](fa1: F[A1], fa2: F[A2])(
      f: (A1, A2) => B
    ): F[B] = map(product2(fa1, fa2)) {
      case (a1, a2) => f(a1, a2)
    }

  }

  object Apply {

    def apply[F[_]: Apply]: Apply[F] = {
      implicitly[Apply[F]]
    }

    implicit object optionApply extends Apply[Option] {

      override def ap[A, B](f: Option[A => B])(fa: Option[A]): Option[B] = {
        for {
          a <- fa
          ff <- f
        } yield ff(a)
      }

      override def map[A, B](fa: Option[A])(f: A => B): Option[B] = {
        fa.map(f)
      }
    }

    implicit object listApply extends Apply[List] {

      override def ap[A, B](f: List[A => B])(fa: List[A]): List[B] = {
        for {
          a <- fa
          ff <- f
        } yield ff(a)
      }

      override def map[A, B](fa: List[A])(f: A => B): List[B] = {
        fa map f
      }
    }
  }

  println(Apply[Option].product2(Some(1), Some("A")))
  println(Apply[Option].product2(Some(1), None))
  println(Apply[List].product2(List(1, 2 ,3), List("A", "B")))
  println(Apply[List].product3(List(1, 2 ,3), List("A", "B"), List(true, false)))


  val addArity2 = (a: Int, b: Int) => a + b
  println(Apply[Option].ap2(Some(addArity2))(Some(1), Some(2)))

  println(Apply[Option].map2(Some(3), Some(4))(addArity2))

  val applyListOpt = Apply[List].compose(Apply[Option])
  println(applyListOpt.map(List(Some(1), None))(e => e + 1))

}
