package patterns

object Design extends App {

  trait NeedLubricant { this: Vehicles =>
    def lubrication: String = stateOf("normal", "needs maintenance")
  }

  trait NeedFuel { this: Vehicles =>
    def tank: String = stateOf("full", "empty")
  }

  class Vehicles(val sound: String) {

    private var latched = false

    protected def stateOf(
        initial: String,
        eventual: String
      ): String = {
      if (!latched) initial else eventual
    }

    def accelerate(): Unit = {
      println(sound)
      latched = true
    }
  }

  val bike = new Vehicles("swoosh") with NeedLubricant with NeedFuel
  val car = new Vehicles("vrooom") with NeedFuel

  println("initial state of bike:" + bike.lubrication)
  println("initial state of bike tank:" + bike.tank)
  bike.accelerate()
  println("eventual state of bike:" + bike.lubrication)
  println("eventual state of bike tank:" + bike.tank)

  println("\ninitial state of car:" + car.tank)
  car.accelerate()
  println("eventual state of car:" + car.tank)
}
