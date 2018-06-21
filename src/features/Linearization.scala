package features

import java.util.Date

object Linearization {

  case class Message(body: String)

  trait Behaviour {
    def receive(m: Message): Unit
  }

  trait AppendBehaviour extends Behaviour {
    val prefix: String
    val suffix: String
    override abstract def receive(m: Message): Unit = super.receive(Message(s"$prefix${m.body}$suffix"))
  }

  trait TimedBehaviour extends Behaviour {
    override abstract def receive(m: Message): Unit = super.receive(Message(s"${new Date}: ${m.body}"))
  }

  trait ConsoleBehaviour extends Behaviour {
    val prefix: String = "<<"
    val suffix: String = ">>"
    override def receive(m: Message): Unit = println(m.body)
  }

  val b1 = new ConsoleBehaviour with TimedBehaviour with AppendBehaviour
  val b2 = new ConsoleBehaviour with AppendBehaviour with TimedBehaviour

  b1.receive(Message("Bye, losers, I always hate you"))
  b2.receive(Message("Bye, losers, I always hate you"))

  class C0 { def c0 = "c0" }
  trait T1 { lazy val t: String = "t1" }
  trait T2 extends T1 { override lazy val t: String = "t2" }

  class C extends C0 with T1
  val c = new C with T1 with T2
  println(c.t)

  trait Foo {
    def x: String = "foo.x"
  }

  trait Bar {
    def x: String = "bar.x"
  }


  val x = new Foo with Bar {
    override def x: String = super[Foo].x
  }
  println(x.x)

  trait A {
    def children: List[A] = List(new A {
      override def children: List[A] = Nil
    })
  }
  trait B {
    def children: List[B] =  List(new B {
      override def children: List[B] = Nil
    })
  }
  class AB extends A with B {
    override def children: List[A with B] = ???
  }
}
