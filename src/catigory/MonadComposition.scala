package catigory

import catigory.MonadTrial.Monad

import scala.language.{higherKinds, reflectiveCalls}

object MonadComposition {


  case class OptionT[F[_], A](value: F[Option[A]])

  implicit def optionTMonad[F[_]](
      implicit F: Monad[F]
    ): Monad[({type C[A] = OptionT[F, A]})#C] = {

    type OptionTF[A] = OptionT[F, A]

    val xs = List(Option(2))

    new Monad[OptionTF] {
      override def unit[A](a: => A): OptionTF[A] = OptionT(F.unit(Some(a)))

      override def flatMap[A, B](
          fa: OptionTF[A])(
          f: A => OptionTF[B]
        ): OptionTF[B] = OptionT {

        F.flatMap(fa.value) {
          case None => F.unit(None)
          case Some(a: A) => f(a).value
        }
      }
    }
  }
}
