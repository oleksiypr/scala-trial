package features;

public class LinearizationJavaImposible {
	static class Message {
		final String body;
		Message(String body) { this.body = body; }
	}
	
	static interface Behaiour {
		default void receive(Message m) {};
	}
	static interface AppendBehaviour extends Behaiour {
		String prefix = "Fields not available for Java interfaces. This is a static constant";
		String suffix = prefix();
		static String prefix() {
			return "Should be static static";
		}
		String suffix();
	    default void receive(Message m) {
	    	//super reference illegal for interfaces
	    	//super.receive(null);
	    }
	}
	
	String prefix = AppendBehaviour.prefix;
}
