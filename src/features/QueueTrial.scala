package features

object QueueTrial extends App {
  import scala.collection.mutable
  implicit val ordering: Ordering[(Int, Int)] = Ordering.fromLessThan((c1, c2) => c1._2 > c2._2)
  
  println(ordering.compare((0, 3), (1, 9)))
                                                  
  val q = new mutable.PriorityQueue[(Int, Int)] 
  q.enqueue((0, 3))
  q.enqueue((1, 9))
  q.enqueue((2, 6))
  
  println(q.dequeue())
  println(q.dequeue())
  println(q.dequeue())
  
  q += ((0, 3))
  q += ((1, 9))
  q += ((2, 6))
  
  println(q.dequeue())
  println(q.dequeue())
  println(q.dequeue())
}