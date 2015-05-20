//http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.Vector;

public class NetworkService implements Runnable {
	public static final int MAX_CLIENTS = 10;
	private final ServerSocket serverSocket;
	private final ExecutorService pool;

	public NetworkService(int port, int poolSize) throws IOException {
		serverSocket = new ServerSocket(port);
		pool = Executors.newFixedThreadPool(poolSize);
	}

	public void run() { // run the service
		try {
			int i = 1;
			for (;;) {
				pool.execute(new Handler(serverSocket.accept()));
				i++;
			}
		} catch (IOException ex) {
			pool.shutdown();
		}
	}

	void shutdownAndAwaitTermination(ExecutorService pool) {
		pool.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
				pool.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(60, TimeUnit.SECONDS))
					System.err.println("Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

	public String retStr() {
		return "sample for Future Object";
	}
}

class Handler implements Runnable {
	public static final int MAX_LOGINS_PER_SECOND = 1;
	private final Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	private String chatId;
	private Useradmin ua;
	static Vector handlers = new Vector(10);

	Handler(Socket socket) {
		ua = new Useradmin();
		this.socket = socket;
		this.chatId = "Anonymous";

		// read and service request on socket
		try {
			// the input stream of the client
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			// the output stream to the chat server
			out = new PrintWriter(socket.getOutputStream(), true);

			boolean loggedIn = false;

			// Handle the login process

			while (!loggedIn) {

				out.println("Please provide a username and password to authenticate!\n User: ");
				long startTime = System.currentTimeMillis();
				String username = in.readLine();
				out.println("Now enter your password! \n Pass: ");
				String password = in.readLine();
				long estimatedTime = System.currentTimeMillis() - startTime;

				// sleep for MAX_LOGINS_PER_SECOND
				Thread.sleep((1000 / MAX_LOGINS_PER_SECOND)
						- (estimatedTime % (1000 / MAX_LOGINS_PER_SECOND)));

				if (ua.checkUser(username, password) == true) {
					out.println("Dear " + username + ", you are logged in now.");
					chatId = username;
					loggedIn = true;
				} else {
					out.println("Login failed. Try again! \n");
				}

			}

		} catch (IOException e) {
			System.out.println("Read failed");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Implements a chat client.
	 * 
	 * Each client should be sent all messages that were written from other
	 * clients. Each client must have an unique ID given by the server to
	 * distinguish the individual participants.
	 * 
	 * It is important to synchronize on the list, otherwise other threads that
	 * are accessing the list concurrently may miss any newly added clients
	 * during a broadcast.
	 */
	public void run() {
		String line;

		synchronized (handlers) {
			handlers.addElement(this);
		}
		try {
			while (!(line = in.readLine()).equalsIgnoreCase("/quit")) {
				for (int i = 0; i < handlers.size(); i++) {
					synchronized (handlers) {
						Handler handler = (Handler) handlers.elementAt(i);
						handler.out.println(chatId + " says: " + line + "\r");
						handler.out.flush();
					}
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			try {
				in.close();
				out.close();
				socket.close();
			} catch (IOException ioe) {
			} finally {
				synchronized (handlers) {
					handlers.removeElement(this);
				}
			}

		}
	}
}
