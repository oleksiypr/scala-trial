package progmeta

import scala.quoted.*

object Debug {
  
  inline def debug[A](inline expr: A): A = ${ debugImpl('expr) }

  inline def included(enabled: Boolean)(inline code: => Unit): Unit =
    if enabled then {code; ()} else ()

  private def debugImpl[A : Type](expr: Expr[A])(using Quotes): Expr[A] = {
    '{
      val value = $expr
      println(${Expr(expr.show)} + " = " + value)
      value
    }
  }
}
