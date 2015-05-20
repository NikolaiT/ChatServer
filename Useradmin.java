import java.security.MessageDigest;
import java.io.*;
import java.security.NoSuchAlgorithmException;

public class Useradmin {

	FileWriter writer;
	File file;
	final String PASSWORD_FILE = "passwords.txt";
	final static String SALT = "somesalt";

	public static void main(String args[]) {
		Useradmin ua = new Useradmin();
		try {
			ua.addUser("root", "toor");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addUser(String username, String password) throws Exception {

		if (checkUser(username, password))
			return;

		String hash = hashPasswort(password);

		file = new File(PASSWORD_FILE);

		try {
			// new FileWriter(file ,true) - falls die Datei bereits existiert
			// werden die Bytes an das Ende der Datei geschrieben

			// new FileWriter(file) - falls die Datei bereits existiert
			// wird diese überschrieben
			writer = new FileWriter(file, true);

			// Text wird in den Stream geschrieben
			writer.write(username + ":" + hash);

			// Platformunabhängiger Zeilenumbruch wird in den Stream geschrieben
			writer.write(System.getProperty("line.separator"));

			// Schreibt den Stream in die Datei
			// Sollte immer am Ende ausgeführt werden, sodass der Stream
			// leer ist und alles in der Datei steht.
			writer.flush();

			// Schließt den Stream
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	public boolean checkUser(String username, String password) throws Exception {
		String hash = hashPasswort(password);

		// This will reference one line at a time
		String line = null;

		try {
			// FileReader reads text files in the default encoding.
			FileReader fileReader = new FileReader(PASSWORD_FILE);

			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			while ((line = bufferedReader.readLine()) != null) {

				if (line.trim().equals(username + ":" + hash))
					return true;
			}

			// Always close files.
			bufferedReader.close();
		} catch (FileNotFoundException ex) {
			return false;
			/*
			 * System.out.println( "Unable to open file '" + PASSWORD_FILE +
			 * "'");
			 */
		} catch (IOException ex) {
			System.out.println("Error reading file '" + PASSWORD_FILE + "'");
			// Or we could just do this:
			// ex.printStackTrace();
		}

		return false;
	}

	private static String convertByteToHex(byte data[]) {
		StringBuffer hexData = new StringBuffer();
		for (int byteIndex = 0; byteIndex < data.length; byteIndex++)
			hexData.append(Integer.toString((data[byteIndex] & 0xff) + 0x100,
					16).substring(1));

		return hexData.toString();
	}

	private static String hashText(String textToHash) throws Exception {
		final MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
		sha512.update(textToHash.getBytes());

		return convertByteToHex(sha512.digest());
	}

	public static String generateHash(String textToHash) {
		MessageDigest md = null;
		byte[] hash = null;
		try {
			md = MessageDigest.getInstance("SHA-512");
			hash = md.digest(textToHash.getBytes("UTF-8"));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return convertByteToHex(hash);
	}

	private static String hashPasswort(String password) throws Exception {
		String hash = generateHash(password.toString() + SALT);

		for (int i = 0; i < 1000; i++) {
			hash = generateHash(hash);
		}

		return hash;
	}

}