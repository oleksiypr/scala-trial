package progfun

object WhatIsFunction extends App {

  // 1. What is Function
  // 1.1 hello world

  val res: Unit = println("Hello world")
  println("res = " + res)

  val x: Int = 1

  // 1.2 what data println operate with?
  // 1.3 Side effect and Unit, what is the value of the unit

  // 1.4 Some function: `twice` double and `size` of string

  def twice(x: Double): Double = 2*x
  def size(s: String): Int = s.size

  var n = 0
  def change(dn: Int): Int = {
    n = +dn
    n
  }

  // 1.5 Some method: `var` and `change`

  // 2. Data and Data types
  // 2.1 Values: val vs var vs def
  // 2.1 Foo isA Bar
  // 2.2 What about Nothing? error, foo = error

  def error(): Nothing = throw new Error

  def f(a: Int): Int = error()
}

object Foo extends App {
  WhatIsFunction.error()
}
