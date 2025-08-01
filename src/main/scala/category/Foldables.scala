package catigory

import catigory.MonoidTrial.Monoid

import scala.language.higherKinds

trait Foldable[F[_]] {

  def foldLeft[A](fa: F[A])(implicit M: Monoid[A]): A = ???
}


object Foldables extends App {


}
