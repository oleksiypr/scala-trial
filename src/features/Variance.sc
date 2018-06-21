object Variance {
	trait Fiend[-T] {
		def beFriend(someone: T) = println(s"$this is a friend of $someone")
	}
	class Person(name: String) extends Fiend[Person] {
    override def toString: String = name
  }
	class Student(name: String) extends Person(name)
	
	def makeFiends(s: Student, f: Fiend[Student]) = f.beFriend(s)

	val john = new Student("John")
  val bob = new Student("Bob")
	val susan = new Person("Susan")
	
	makeFiends(john, susan)
  makeFiends(bob, john)
}