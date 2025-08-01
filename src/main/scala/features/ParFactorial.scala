package features

object ParFactorial {

  def factorial(num: BigInt): BigInt = {
    def factImp(num: BigInt, fact: BigInt): BigInt = {
      if (num == 0) fact
      else
        factImp(num - 1, num * fact)
    }
    factImp(num, 1)
  }

  /*  def factorialUsingSpark(num: BigInt): BigInt = {
      if (num == 0) BigInt(1)
      else {
        val list = (BigInt(1) to num).toList
        sc.parallelize(list).reduce(_ * _)
      }
    }*/
  def factorialPar(num: BigInt): BigInt = (BigInt(1) to num).product

  var t0 = System.currentTimeMillis()
  //factorial(200000)
  (BigInt(1) to 200000).product
  println(System.currentTimeMillis() - t0)

  t0 = System.currentTimeMillis()
  //factorialPar(200000)
  (BigInt(1) to 200000).product
  println(System.currentTimeMillis() - t0)
}
