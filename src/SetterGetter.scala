trait Setter[S, A] {

  def modify(f: A => A): S => S
  def set(a: A): S => S = modify(_ => a)
}


trait Prism[S, A] {
  def reverseGet(a: A): S
  def getOption(s: S): Option[A]
  def modifyOption(f: A => S): S => Option[S] =
    s => getOption(s).map(f)
}


object SetterGetterTrial extends App {

  case class Foo(
    bar: String = "",
    baz: Int = -1
  )

  val setBar: Setter[Foo, String] = (f: String => String) => foo => foo.copy(bar = f(foo.bar))
  val setBaz: Setter[Foo, Int] = (f: Int => Int) => foo => foo.copy(baz = f(foo.baz))

  val foo = Foo()

  val res: Foo = (setBar.set("bar-value") andThen setBaz.set(10))(foo)
  println(res)
}
