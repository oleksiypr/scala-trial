package progmeta

case class Foo(x: Int) {
  infix def +(y: Foo): Foo = Foo(x + y.x)
}

object Foo {
  
  import JsonPrettifier.*
  
  given JsonPrettifier[Foo] = foo =>
    s"""|{
       |  "x": ${foo.x.pretty}
       |}""".stripMargin
}