package catigory

import scala.language.{higherKinds, reflectiveCalls}

object Functors extends App {

  object Theory {

    trait Functor {
      type F0[A]
      def F1[A, B](arrow: A => B): F0[A] => F0[B]
    }

    object optionFunctor extends Functor {
      type F0[A] = Option[A]
      def F1[A, B](arrow: A => B): F0[A] => F0[B] = oa => oa.map(arrow)
    }

    object IntFunctor extends Functor {
      type F0[A] = Int
      def F1[A, B](f: A => B): F0[A] => F0[B] = x => x
    }

    val g: Int => Int = IntFunctor.F1[Double, String](x => x.toString)

    optionFunctor.F1[Int, Double](n => n.toDouble)(Some(5))
  }

  object Cats {

    trait Functor[F[_]] { self =>

      def map[A, B](fa: F[A])(f: A => B): F[B]

      def lift[A, B](f: A => B): F[A] => F[B] = fa => map(fa)(f)

      def compose[G[_]](g: Functor[G]): Functor[
        ({type C[X] = F[G[X]]})#C
        ]	= {
        new Functor[({type C[X] = F[G[X]]})#C] {
          override def map[A, B](fa: F[G[A]])(f: A => B): F[G[B]] = {
            self.map(fa)(a => g.map(a)(f))
          }
        }
      }

      def fproduct[A, B](
          fa: F[A])(
          f: A => B): F[(A, B)] = map(fa)(a => (a, f(a)))
    }

    object Functor {

      def apply[F[_]: Functor]: Functor[F] = {
        implicitly[Functor[F]]
      }
    }

    implicit val optionFunctor: Functor[Option] = new Functor[Option] {
      def map[A, B](fa: Option[A])(f: A => B): Option[B] = fa map f
    }

    implicit val listFunctor: Functor[List] = new Functor[List] {
      def map[A, B](fa: List[A])(f: A => B): List[B] = fa map f
    }

    implicit def function1Functor[In]: Functor[
      ({type F[X] = In => X})#F
      ] =
      new Functor[({type F[X] = In => X})#F] {
        def map[A, B](fa: In => A)(f: A => B): In => B = fa andThen f
      }

    val f: Int => Int = x => x + 1
    (Functor[List] compose Functor[Option]).map(List(Some(1), None, Some(3)))(f)
    List(Some(2), None, Some(4))
  }

  import Cats._

  println(Functor[Option].map(Some("abd"))(s => s.length))

  val lenOption: Option[String] ⇒ Option[Int] = Functor[Option].lift(_.length)
  println(lenOption(Some("Hello")))
  println(lenOption(Option(null)))

  case class Foo(i: Int)
  case class Bar(s: String)

  type FuctionInt[X] = Int => X

  val f = Functor[FuctionInt].map(Foo)(foo => Bar("hello: " + foo.i.toString))
  println(f(100))

  println(List(Some(2), None, Some(4)) + " === " +
    (Functor[List] compose Functor[Option]).map(List(Some(1), None, Some(3)))(_ + 1))
}
