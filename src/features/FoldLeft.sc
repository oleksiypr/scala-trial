import java.util.PriorityQueue

object FoldLeft {
	  0 until 3                               //> res0: scala.collection.immutable.Range = Range(0, 1, 2)

  val (_, s, t) = Array((0, 3), (1, 9), (2, 6)).foldLeft((0, 0, 0)) {
  	(wst: (Int, Int, Int), c: (Int, Int)) => (wst._1 + c._2, wst._2 + wst._1 + c._2, wst._3 + c._1)
  }                                               //> s  : Int = 33
                                                  //| t  : Int = 3
                                                  
  (s - t) / 3                                     //> res1: Int = 10

  
}