import java.io.*;

import javax.net.ssl.*;

import com.sun.net.ssl.*;

import java.security.Security;

import com.sun.net.ssl.internal.ssl.Provider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Vector;


/**
 * 
 * @author nikolai
 * 
 * This is a simple ChatService over SSL using the Java JSEE library.
 * 
 * Some ideas taken from: https://www.owasp.org/index.php/Using_the_Java_Secure_Socket_Extensions
 * 
 * 
 * 
 * A keystore contains private keys, and the certificates with their corresponding public keys.
 * A truststore contains certificates from other parties that you expect to communicate with, or from Certificate Authorities that you trust to identify other parties.
 * 
 * If you are going to test the client and server on your machines:
 *
 * Generate keystore  with:
 * 	keytool -genkey -alias server-alias -keyalg RSA -keypass changeit -storepass changeit -keystore keystore.jks
 * 
 * Note: You must specify a fully qualified domain for the “first and last name” question. The reason for this use is that some CAs such as VeriSign expect this properties to be a fully qualified domain name.
 * 
 * Export the certificate for the client:
 * 	keytool -export -alias server-alias -storepass changeit -file server.cer -keystore keystore.jks
 * 
 * Copy the client certificate over a secure channel to your client machine and import it in a trusstore:
 * 
 * 	keytool -import -v -trustcacerts -alias server-alias -file server.cer -keystore cacerts.jks -keypass changeit -storepass changeit
 * 
 */

public class ChatServer implements Runnable {
	// change this if you want to allow more clients
	public static final int MAX_CLIENTS = 10;
	
	private final SSLServerSocket sslServerSocket;
	private final ExecutorService pool;

	public ChatServer(int port, int poolSize) throws IOException {
		
		// Registering the JSSE provider
		Security.addProvider(new Provider());
		//Specifying the Keystore details
		System.setProperty("javax.net.ssl.keyStore","/home/nikolai/workspace/ChatService/keys/server/keystore.jks");
		System.setProperty("javax.net.ssl.keyStorePassword","changeit");
		System.setProperty("javax.net.debug","all");
		
		SSLServerSocketFactory sslServerSocketfactory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
		sslServerSocket = (SSLServerSocket)sslServerSocketfactory.createServerSocket(port);
		
		pool = Executors.newFixedThreadPool(poolSize);
	}

	public void run() { // run the service
		try {
			int i = 1;
			for (;;) {
				pool.execute(new Handler((SSLSocket)sslServerSocket.accept()));
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
	private final SSLSocket sslSocket;
	private BufferedReader in;
	private PrintWriter out;
	private String chatId;
	private Useradmin ua;
	static Vector handlers = new Vector(10);

	Handler(SSLSocket sslSocket) {
		ua = new Useradmin();
		this.sslSocket = sslSocket;
		this.chatId = "Anonymous";

		// read and service request on socket
		try {
			// the input stream of the client
			in = new BufferedReader(new InputStreamReader(
					sslSocket.getInputStream()));
			// the output stream to the chat server
			out = new PrintWriter(sslSocket.getOutputStream(), true);

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
				sslSocket.close();
			} catch (IOException ioe) {
			} finally {
				synchronized (handlers) {
					handlers.removeElement(this);
				}
			}

		}
	}
}
