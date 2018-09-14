package assignment


trait RNG {
  def nextInt: (Int, RNG)
}

case class SimpleRNG(seed: Long) extends RNG {

  def nextInt: (Int, RNG) = {
    val newSeed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL
    val nextRNG = SimpleRNG(newSeed)
    val n = (newSeed >>> 16).toInt
    (n, nextRNG)
  }

}

object State extends App {

  def nonNegativeInt(rng: RNG): (Int, RNG) = {
    val (i, r) = rng.nextInt
    (if (i < 0) -(i + 1) else i, new RNG {
      override def nextInt: (Int, RNG) = nonNegativeInt(r)
    })
  }

  def double(rng: RNG): (Double, RNG) = {
    val (i, ri) = nonNegativeInt(rng)
    (i.toDouble / Int.MaxValue, ri)
  }

  def intDouble(rng: RNG): ((Int, Double), RNG) = {
    val (i, ri) = rng.nextInt
    val (d, rd) = double(ri)
    ((i, d), rng)
  }

  //val (x0, r1) = nonNegativeInt(SimpleRNG(42))
  val (x0, r1) = double(SimpleRNG(56))
  val (x1, r2) = double(r1)
  val (x2, r3) = double(r2)
  val (x3, r4) = double(r3)
  val (x4, _)  = double(r4)

  println(x0, x1, x2, x3, x4)
}

