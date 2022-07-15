package net.markwalder.junit.mailserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Mail client connection.
 */
public class Client {

	private static final String CRLF = "\r\n";
	private static final String LF = "\n";

	private final BufferedReader reader;
	private final BufferedWriter writer;
	private final StringBuilder log;

	Client(Socket socket, StringBuilder log) throws IOException {

		InputStream inputStream = socket.getInputStream();
		this.reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.US_ASCII));

		OutputStream outputStream = socket.getOutputStream();
		this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.US_ASCII));

		this.log = log;
	}

	/**
	 * Read the next line sent by the client.
	 *
	 * @return Line of text sent by client, or {@code null} if the client has
	 * closed the connection.
	 */
	public String readLine() throws IOException {
		String line = reader.readLine(); // TODO: accept only CRLF as line separator?
		if (line == null) return null;
		System.out.println("Client: " + line);
		log.append(line).append(LF);
		return line;
	}

	/**
	 * Send a line of text to the client, followed by a CRLF line break.
	 *
	 * @param line Line of text to send.
	 */
	public void writeLine(String line) throws IOException {
		System.out.println("Server: " + line);
		log.append(line).append(LF);
		writer.write(line);
		writer.write(CRLF);
		writer.flush();
	}

}
