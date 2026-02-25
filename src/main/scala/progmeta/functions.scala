package progmeta

import scala.quoted.*

inline def square(x: Double): Double = ${ unrolledPowerCode('x, 2) }
inline def qube(x: Double): Double = ${ unrolledPowerCode('x, 3) }

def unrolledPowerCode(x: Expr[Double], n: Int)(using Quotes): Expr[Double] =
  if n == 0 then '{ 1.0 }
  else if n == 1 then x
  else '{ $x * ${ unrolledPowerCode(x, n - 1) } }
