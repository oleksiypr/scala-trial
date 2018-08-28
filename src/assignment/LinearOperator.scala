package assignment

import scala.annotation.implicitNotFound

object LinearOperator extends App {

  @implicitNotFound("No member of type class Ring in scope for ${V}")
  trait Ring[V] {
    val Zero: V
    val One: V

    def +(a: V, b: V): V
    def *(a: V, b: V): V
  }
  object Ring {
    implicit class RingValue[V](val v: V) extends AnyVal {
      // ???
    }
  }

  case class Lin[WhatThis](as: Array[WhatThis]) {
    /**
      * Hint: you may want to use [[Ring.RingValue]] implicit class.
      * How and why?
      */
    import Ring._

    def apply(xs: Array[WhatThis]): WhatThis = {
      /* uncomment
      (xs zip as).foldLeft(???) { (s, ax) =>
        val (a, x) = ???
          s + a * x
      }*/
      ???
    }
  }

  val as = Array(1.0, 2.0, 3.0)
  val xs = Array(3.0, 2.0, 1.0)

  /**
    * You need some preparation to be done here.
    * Hint: [[Ring]] trait might be in use
    */
  ???

  val Lr = Lin(as)
  println(Lr(xs))
}
