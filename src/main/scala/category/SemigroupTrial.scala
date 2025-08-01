package catigory

object SemigroupTrial extends App {

  trait Semigroup[A] {
    def combine(a1: A, a2: A): A
  }

  object Semigroup {
    def apply[T: Semigroup]: Semigroup[T] = {
      implicitly[Semigroup[T]]
    }
  }

  implicit object intSemigroup extends Semigroup[Int] {
    def combine(a1: Int, a2: Int): Int = a1 + a2
  }

  class OptionSemigroup[T: Semigroup] extends Semigroup[Option[T]] {
    def combine(a1: Option[T], a2: Option[T]): Option[T] = {
      (a1, a2) match {
        case (None, None) => None
        case (s @ Some(_), None) => s
        case (None, s @ Some(_)) => s
        case (Some(v1), Some(v2)) => Some {
          Semigroup[T].combine(v1, v2)
        }
      }
    }
  }

  class FunctionSemigruop[A: Semigroup, B: Semigroup]
    extends Semigroup[A => B] {

    def combine(f: A => B, g: A => B): A => B = {
      x => Semigroup[B].combine(f(x), g(x))
    }
  }

  class ListSemigroup[T] extends Semigroup[List[T]] {
    def combine(a1: List[T], a2: List[T]): List[T] = a1 ++ a2
  }

  class MapSemigroup[K, V: Semigroup] extends Semigroup[Map[K, V]] {
    def combine(m1: Map[K, V], m2: Map[K, V]): Map[K, V] = {
      m1.map { case (k1, v1) =>
        m2.get(k1) match {
          case Some(v2) => k1 -> Semigroup[V].combine(v1, v2)
          case None => k1 -> v1
        }
      } ++ m2.filterKeys(!m1.contains(_))
    }
  }

  implicit def optionSemigroup[A: Semigroup]: Semigroup[Option[A]] = new OptionSemigroup[A]
  implicit def functionSemigruop[A: Semigroup, B: Semigroup]: Semigroup[A => B] = new FunctionSemigruop[A, B]
  implicit def listSemigroup[A]: Semigroup[List[A]] = new ListSemigroup[A]
  implicit def mapSemigroup[K, V: Semigroup]: Semigroup[Map[K, V]] = new MapSemigroup[K, V]

  println(Semigroup[Option[Int]].combine(Some(2), Some(3)))
  println(Semigroup[Option[Int]].combine(Some(2), None))
  println(Semigroup[Option[Int]].combine(None, None))
  println(Semigroup[Int => Int].combine(x => x + 1, x => 10 * x)(6))

  println(Map("foo" -> List(1, 2)) ++ Map("foo" -> List(3, 4), "bar" -> List(42)))
  println(Semigroup[Map[String, List[Int]]].combine(
    Map("foo" -> List(1, 2)),
    Map("foo" -> List(3, 4), "bar" -> List(42))))

  println(Semigroup[Map[String, Map[String, Int]]].combine(
    Map("foo" → Map("bar" → 5)),
    Map("foo" → Map("bar" → 6))
  ))
}
