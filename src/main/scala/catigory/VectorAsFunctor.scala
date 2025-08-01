package catigory

import scala.annotation.implicitNotFound

object VectorAsFunctor {

  case class Lin[V: Field](as: List[V]) {
    import Field._

    def apply(xs: List[V]): V = {
      val zero = implicitly[Field[V]].Zero
      if (xs.size == as.size) (xs zip as).foldLeft(zero) { (s, ax) =>
        val (a, x) = ax
        s + a * x
      }
      else if (xs.size < as.size) apply(xs ::: List.fill(as.size - xs.size)(zero))
      else apply(xs.take(as.size))
    }
  }

  @implicitNotFound("No member of type class Field in scope for ${V}")
  trait Field[V] {
    val Zero: V
    val Identity: V

    def +(a: V, b: V): V
    def -(a: V, b: V): V
    def *(a: V, b: V): V
    def /(a: V, b: V): V
    def -(a: V): V
  }

  object Field {
    implicit class FieldValue[V](val v: V) extends AnyVal {
      def +(x: V)(implicit field: Field[V]): V = field.+(v, x)
      def -(x: V)(implicit field: Field[V]): V = field.-(v, x)
      def *(x: V)(implicit field: Field[V]): V = field.*(v, x)
      def /(x: V)(implicit field: Field[V]): V = field./(v, x)
      def unary_-(implicit field: Field[V]): V = field.-(v)
    }
  }

  implicit object FieldOfReal extends Field[Double] {
    val Zero: Double = 0.0
    val Identity: Double = 1.0

    def +(a: Double, b: Double): Double = a + b
    def -(a: Double, b: Double): Double = a - b
    def *(a: Double, b: Double): Double = a * b
    def /(a: Double, b: Double): Double = a / b
    def -(a: Double): Double = -a
  }

  trait Matrix[R, C, V] extends (Vector[C, V] => Vector[R, V]) with ((R, C) => V) {
    self =>
    import Field._

    implicit val field: Field[V]

    val rows: Set[R]
    val columns: Set[C]

    def apply(x: Vector[C, V]): Vector[R, V] =
      if (x.domain != columns) throw new IllegalArgumentException
      else new Vector[R, V] {
        val domain: Set[R] = rows
        def apply(r: R): V = columns.foldLeft[V](field.Zero) {
          (y, c) => y + self(r, c) * x(c)
        }
      }

    def andThen[R1](m: Matrix[R1, R, V]): Vector[C, V] => Vector[R1, V] =
      super.andThen[Vector[R1, V]] {
        m.apply(_: Vector[R, V])
      }
  }

  trait Vector[D, V] extends (D => V) {
    val domain: Set[D]
    def apply(d: D): V
    def map[D1](m: Matrix[D1, D, V]): Vector[D1, V] = m(this)
    def map[D1](m: Vector[D, V] => Vector[D1, V]): Vector[D1, V] = m(this)
    override def toString: String = domain.map(d => s"$d: ${apply(d)}") mkString "\n"
  }

  case class RealVector(xs: IndexedSeq[Double]) extends Vector[Int, Double] {
    val domain: Set[Int] = xs.indices.map(_ + 1).toSet // 1..n
    def apply(d: Int): Double = xs(d - 1)
  }
  case class RealMatrix(
                         elems: Array[Array[Double]]
                       )(implicit val field: Field[Double]) extends Matrix[Int, Int, Double] {
    val rows: Set[Int] = elems.indices.map(_ + 1).toSet
    val columns: Set[Int] = elems(0).indices.map(_ + 1).toSet
    def apply(r: Int, c: Int): Double = elems(r - 1)(c - 1)
  }

  val v: Vector[Int, Double] = RealVector(IndexedSeq(3.0, 2.1, 5.7))
  val A: Matrix[Int, Int, Double] = RealMatrix(
    Array(
      Array(1.0, 2.0, 4.6),
      Array(3.4, 1.0, 0.0),
      Array(2.0, 0.2, 1.0)
    )
  )
  val B: Matrix[Int, Int, Double] = RealMatrix(
    Array(
      Array(1.0, 0.2, 0.1),
      Array(3.4, 1.0, 5.0),
      Array(0.3, 2.0, 1.0)
    )
  )
  val E: Matrix[Int, Int, Double] = RealMatrix(
    Array(
      Array(1.0, 0.0, 0.0),
      Array(0.0, 1.0, 0.0),
      Array(0.0, 0.0, 1.0)
    )
  )

  println("Lets consider vector as a functor")
  println()

  println("Identity Law")
  val a = v map E
  val b = E(a)
  println(a)
  println()
  println(b)

  println()

  println("Composition Law")
  val u = v map A map B
  val w = v map (A andThen B)

  println(u)
  println()
  println(w)
}
