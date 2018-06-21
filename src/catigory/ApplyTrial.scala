package catigory

import catigory.Functors.Cats.Functor
import scala.language.higherKinds

object ApplyTrial {

  trait Apply[F[_]] extends Functor[F] {
    def ap[A, B](f: F[A => B])(fa: F[A]): F[B]

    def product[A, B](fa: F[A], fb: F[B]): F[(A, B)] = ???
  }

  object Apply {

    def apply[F[_]: Functor]: Functor[F] = {
      implicitly[Functor[F]]
    }

    implicit object optionApply extends Apply[Option] {

      override def ap[A, B](f: Option[A => B])(fa: Option[A]): Option[B] = {
        for {
          a <- fa
          ff <- f
        } yield ff(a)
      }

      override def map[A, B](fa: Option[A])(f: A => B): Option[B] = {
        Functor[Option].map(fa)(f)
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
        Functor[List].map(fa)(f)
      }
    }
  }

}
