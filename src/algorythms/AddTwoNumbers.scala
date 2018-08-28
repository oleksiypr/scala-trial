package algorythms

import scala.annotation.tailrec

object AddTwoNumbers extends App {

  object Solution2 {

    type ListNode = List[Int]

    def addTwoNumbers(l1: ListNode, l2: ListNode): ListNode = {
      @tailrec
      def add(e: Int)(n1: ListNode, n2: ListNode)(res: ListNode): ListNode = {
        val d1 = n1.headOption.getOrElse(0)
        val d2 = n2.headOption.getOrElse(0)
        val r = d1 + d2 + e
        (n1, n2) match {
          case (Nil, Nil) => if (e == 0) res else add(0)(Nil, Nil)(e :: res)
          case (h1 :: t1, Nil) => add(r / 10)(t1, Nil)((r % 10) :: res)
          case (Nil, h2 :: t2) => add(r / 10)(Nil, t2)((r % 10) :: res)
          case (h1 :: t1, h2 :: t2) => add(r / 10)(t1, t2)((r % 10) :: res)
        }
      }
      add(0)(l1, l2)(Nil).reverse
    }

    println(addTwoNumbers(List(5), List(3)))
    println(addTwoNumbers(List(5), List(5)))
    println(addTwoNumbers(List(2, 4, 3), List(5, 3, 4)))
    println(addTwoNumbers(List(6, 9), List(6)))
    println(addTwoNumbers(List(6, 9), List(2)))
  }

  Solution2
  Solution1

  object Solution1 {

    class ListNode(var _x: Int = 0) {
      x = _x
      var next: ListNode = null
      var x: Int = _
    }


    def addTwoNumbers(l1: ListNode, l2: ListNode): ListNode = {
      def add(
        n1: Int, n2: Int,
        next1: ListNode, next2: ListNode)(res: ListNode): ListNode = {

        val r = n1 + n2
        val e = r / 10
        res.x = r % 10
        res.next =
          if (next1 == null && next2 == null)
            if (e == 0) null
            else add(
              n1 = e, n2 = 0,
              next1 = null, next2 = null)(new ListNode())
          else if (next1 != null && next2 == null)
            add(
              next1.x + e, n2 = 0,
              next1.next, next2 = null
            )(new ListNode())
          else if (next1 == null && next2 != null)
            add(
              n1 = 0, next2.x + e,
              next1 = null, next2 = next2.next
            )(new ListNode())
          else
            add(
              next1.x + e, next2.x,
              next1.next, next2.next)(new ListNode())

        res
      }

      add(l1.x, l2.x, l1.next, l2.next)(new ListNode())
    }


    def show(res: ListNode): Unit = {
      var next = res
      while (next != null) {
        print(s"${next.x} ")
        next = next.next
      }
      println()
    }

    show(addTwoNumbers(new ListNode(3), new ListNode(5)))
    show(addTwoNumbers(new ListNode(5), new ListNode(5)))

    show(addTwoNumbers({
      val two = new ListNode(2)
      val four = new ListNode(4)
      val three = new ListNode(3)
      two.next = four
      four.next = three
      two
    }, {
      val five = new ListNode(5)
      val six = new ListNode(3)
      val four = new ListNode(4)
      five.next = six
      six.next = four
      five
    }))

    show(addTwoNumbers({
      val d1 = new ListNode(6)
      val d2 = new ListNode(9)
      d1.next = d2
      d1
    }, {
      new ListNode(6)
    }))

    show(addTwoNumbers({
      val d1 = new ListNode(6)
      val d2 = new ListNode(9)
      d1.next = d2
      d1
    }, {
      new ListNode(2)
    }))
  }
}



