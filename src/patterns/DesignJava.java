package patterns;

public class DesignJava {

	static class Vehicles {

		final String sound;
		private final String initialState;
		private final String eventualState;		
		private boolean latched = false;		
		
		public Vehicles(String sound, String initialState, String eventualState) {
			this.sound = sound;
			this.initialState = initialState;
			this.eventualState = eventualState;
		}
		
		String state() {
			if (!latched) return initialState;
			else return eventualState;
		}		
		
		void accelerate() {
			latched = true;
			System.out.println(sound);
		}	
	}
	
	static class Car extends Vehicles {

		public Car() {
			super("vrooom", "full", "empty");
		}
		
		String tank() {
			return state();
		}
	}
	
	static class Bike extends Vehicles {

		public Bike() {
			super("swoosh", "normal", "needs maintenance");
		}
		
		String lubrication() {
			return state();
		}
	}

	public static void main(String[] args) {
		Bike bike = new Bike();
		Car car = new Car();

		System.out.println("initial state of bike:" + bike.lubrication());
		bike.accelerate();
		System.out.println("eventual state of bike:" + bike.lubrication());
		
		System.out.println("\ninitial state of car:" + car.tank());
		car.accelerate();
		System.out.println("eventual state of car:" + car.tank());
	}
}
