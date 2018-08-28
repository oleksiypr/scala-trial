object Clojure {

  var x: Int = 5
  val z = (y: Int) => x + y                       //> z  : Int => Int = <function1>
  z(1)                                            //> res0: Int = 6

  x = 2
  z(1)                                            //> res1: Int = 3

  def sum(x: Int)(y: Int): Int = x + y
  val incr = sum(1)_

  incr(2)

  def sumCurr(x: Int): Int => Int = (y: Int) => x + y
  val incrCurr: Int => Int = sumCurr(1)

  incrCurr(2)

}