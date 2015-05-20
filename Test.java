import java.io.IOException;

class Test {

	public static void main(String args[]) {
		int port = 8888;

		try {
			if (args.length > 0)
				port = Integer.parseInt(args[0]);
		} catch (NumberFormatException nfe) {
			System.err.println("Benutzung: java Test port");
			System.exit(0);
		}

		try {
			NetworkService ns = new NetworkService(8888, 10);
			ns.run();
		} catch (IOException e) {
			System.out.println(e);
		}
	}

}
