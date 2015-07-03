import java.io.IOException;

class RunServer {

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
			ChatServer ns = new ChatServer(port, 10);
			ns.run();
		} catch (IOException e) {
			System.out.println(e);
		}
	}

}
