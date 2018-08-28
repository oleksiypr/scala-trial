package catigory

import catigory.Functors.Cats.Functor
import scala.language.higherKinds

object ApplyTrial extends App {

  trait Apply[F[_]] extends Functor[F] {

    def ap[A, B](f: F[A => B])(fa: F[A]): F[B]

    def product[A, B](fa: F[A], fb: F[B]): F[(A, B)] = {
      ap(map(fa)((a: A) => (b: B) => (a, b)))(fb)
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

  println(Apply[Option].product(Some(1), Some("A")))
  println(Apply[Option].product(Some(1), None))
  println(Apply[List].product(List(1, 2 ,3), List("A", "B")))
}
