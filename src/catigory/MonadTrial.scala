package catigory

import scala.collection.mutable
import scala.language.higherKinds

object MonadTrial extends App {

  trait Monad[M[_]] {
    def unit[A](a: => A): M[A]
    def flatMap[X, Y](m: M[X])(f: X => M[Y]): M[Y]
  }

  implicit class MonadOps[X, C[_]](val c: C[X]) extends AnyVal {
    def unit(implicit m: Monad[C]): C[X] = c
    def flatMap[Y](f: X => C[Y])(implicit m: Monad[C]): C[Y] = m.flatMap(c)(f)
  }

  implicit object optionMonad extends Monad[Option] {
    def unit[A](a: => A): Option[A] = Some(a)
    def flatMap[X, Y](m: Option[X])(f: X => Option[Y]): Option[Y] = m flatMap f
  }

  implicit object listMonad extends Monad[List] {
    def unit[A](a: => A): List[A] = List(a)
    def flatMap[X, Y](m: List[X])(f: X => List[Y]): List[Y] = m flatMap f
  }

  trait Repo[E, C[_]] {
    def add(e: E): C[Unit]
    def get(id: Int): C[E]
  }

  def transform[X, Y, C[_]: Monad](id: Int)(
      repo: Repo[X, C])(
      f: X => C[Y]
    ): C[Y] = {
    repo.get(id).flatMap(f)
  }

  val repoList = new Repo[String, List] {
    private[this] var id: Int = -1
    private[this] val data = mutable.Map[Int, String]()

    def add(e: String): List[Unit] = {
      println(s"Value to add: $e")
      id += 1
      List(data += (id -> e))
    }

    def get(id: Int): List[String] = {
      data.filter(kv => {
        val (k, v) = kv
        k % id == 0
      }).values.toList
    }
  }

  val repoOption = new Repo[String, Option] {
    private[this] var id: Int = -1
    private[this] val data = mutable.Map[Int, String]()

    def add(e: String): Option[Unit] = {
      println(s"Value to add: $e")
      id += 1
      Option(data += (id -> e))
    }

    def get(id: Int): Option[String] = {
      data.get(id)
    }
  }

  println("\nrepoList:")
  repoList.add("hello, hey")
  repoList.add("Hi")
  repoList.add("bar, baz")
  repoList.add("foo")

  println("\nall even:")
  transform(2)(repoList) {
    e => e.split(",").map(_.trim).toList
  } foreach println


  println("\nrepoOption:")
  repoOption.add("hello, hey")
  repoOption.add("Hi")
  repoOption.add("bar, baz")
  repoOption.add("foo")

  println("\nwords:")
  transform(0)(repoOption) {
    e => {
      val words = e.split(",")
      if (words.size > 1) Some(words mkString " ") else None
    }
  } foreach println
}

