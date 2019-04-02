import scala.language.higherKinds


sealed trait HList {
  def ::[H](h: H): H :: this.type = new ::(h, this)
}

object HNil extends HList

case class ::[+H, +T <: HList](head: H, tail: T) extends HList

object HListTrial extends App {

  val x: String :: Int :: Boolean :: HNil.type = "hello" :: 1 :: true :: HNil

  println(x.head)
  println(x.tail.head)
  println(x.tail.tail.head)

  x match {
    case str :: int :: bool :: HNil => println(s"string: $str, int: $int, bool: $bool")
  }

  case class Foo(x: Int)
  case class Bar(f: Boolean)

  def mkString(hlist: HList): String = {
    hlist match {
      case HNil => ""
      case h :: HNil => h.toString
      case h :: tail => h.toString + ", " + mkString(tail)
    }
  }

  val foox: Foo :: String :: Int :: Boolean :: HNil.type = Foo(-3) :: x

  println("!!!!" + mkString(foox))


  case class Data(value: String, data: HList)


  val data = Data(
    "level1", Foo(1) :: Data(
      "level2", Bar(false) :: Data(
        "level3", HNil) :: HNil
    ) :: HNil
  )

  println(data)

  def acceptStringIntAndWhatEver(v :  String :: Int :: HList): Unit = {
    val string: String = v.head
    val int: Int = v.tail.head

    println(s"string: $string, int: $int")
    println(s"And whatever: ${mkString(v.tail.tail)}")
  }

  acceptStringIntAndWhatEver(x)
}
