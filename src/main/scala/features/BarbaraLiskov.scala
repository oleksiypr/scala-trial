package features

object BarbaraLiskov extends App {

  class Fruit
  class Apple extends Fruit

  class Drink(val name: String)
  class Juice extends Drink(name ="juice")

  type AppleDrinkRecipe = Apple => Drink
  type FruitJuiceRecipe = Fruit => Juice

  object AppleDrinkMaker {

    private var appleStore = Set.empty[Apple]

    private def get(n: Int): Set[Apple] = {
      val as = appleStore.take(n)
      appleStore --= as
      as
    }

    def loadApples(apples: Set[Apple]): Unit = {
      appleStore ++= apples
    }

    def apply(n: Int)(recipe: AppleDrinkRecipe): Set[Drink] = {
      println("If you can make a drink from an apple")
      val apples = get(n)
      val drinks = apples.map(recipe)

      drinks foreach {
        drink => println(s"You will make apple ${drink.name}")
      }
      drinks
    }
  }


  def fruitJuiceRecipe(fruit: Fruit): Juice = {
    println("And you know how to make a juice from any fruit ")
    new Juice
  }


   AppleDrinkMaker.loadApples(Seq.fill(10)(new Apple).toSet)
  val appleJuice = AppleDrinkMaker(1)(fruitJuiceRecipe)


  class Orange extends Fruit
  def orangeJuiceRecipe(orange: Orange): Juice = new Juice

  // it is not possible to prepare orange juice with AppleDrinkMaker
  //AppleDrinkMaker(1)(orangeJuiceRecipe)
}
