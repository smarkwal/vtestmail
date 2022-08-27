/*
 * Copyright 2022 Stephan Markwalder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.markwalder.vtestmail.core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import net.markwalder.vtestmail.utils.Assert;
import net.markwalder.vtestmail.utils.LineReader;

/**
 * Mail client connection.
 */
public abstract class MailClient {

	private static final String CRLF_MARKER = "<CRLF>";

	protected static final String CRLF = "\r\n";
	protected static final String LF = "\n";

	private final Charset charset;
	private final String continuation;
	private final StringBuilder log;

	private Socket socket;
	private LineReader reader;
	private BufferedWriter writer;

	protected MailClient(Socket socket, Charset charset, String continuation, StringBuilder log) throws IOException {
		Assert.isNotNull(socket, "socket");
		Assert.isNotNull(charset, "charset");
		Assert.isNotEmpty(continuation, "continuation");
		Assert.isNotNull(log, "log");

		this.charset = charset;
		this.continuation = continuation;
		this.log = log;

		// open reader and writer
		useSocket(socket);
	}

	public void startTLS(String protocol, MailSession session) throws IOException {
		Assert.isNotEmpty(protocol, "protocol");
		Assert.isNotNull(session, "session");

		if (socket instanceof SSLSocket) {
			throw new IOException("TLS already started");
		}

		// get server address and port
		String address = socket.getInetAddress().getHostAddress();
		int port = socket.getPort();

		// create a new SSL socket wrapping the existing socket
		SSLSocketFactory sslSocketFactory = SSLUtils.createSSLSocketFactory(protocol);
		SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(socket, address, port, true);

		// initiate handshake
		System.out.println("[SSL/TLS handshake]");
		sslSocket.setUseClientMode(false);
		sslSocket.startHandshake();

		// continue using SSL socket
		useSocket(sslSocket);

		// update socket data in session
		session.setSocketData(sslSocket);
	}

	private void useSocket(Socket socket) throws IOException {

		// remember socket
		this.socket = socket;

		// create reader to read commands from client
		InputStream inputStream = socket.getInputStream();
		this.reader = new LineReader(new InputStreamReader(inputStream, charset));

		// create writer to write responses to client
		OutputStream outputStream = socket.getOutputStream();
		this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, charset));

	}

	/**
	 * Read the next line sent by the client.
	 *
	 * @return Line of text sent by client, or {@code null} if the client has
	 * closed the connection.
	 * @throws IOException If an I/O error occurs.
	 */
	public String readLine() throws IOException {
		String line = reader.readLine();
		if (line == null) return null;
		System.out.println("Client: " + line + CRLF_MARKER);
		log.append(line).append(LF);
		return line;
	}

	/**
	 * Read the given amount of characters from the client.
	 *
	 * @param len Number of characters to read.
	 * @return Characters read from client.
	 * @throws IOException If an I/O error occurs.
	 */
	public String readChars(long len) throws IOException {
		String chars = reader.readChars(len);
		System.out.println("Client: " + chars);
		log.append(chars);
		return chars;
	}

	/**
	 * Send a line of text to the client, followed by a CRLF line break.
	 *
	 * @param line Line of text to send.
	 * @throws IOException If an I/O error occurs.
	 */
	public void writeLine(String line) throws IOException {
		Assert.isNotNull(line, "line");
		System.out.println("Server: " + line + CRLF_MARKER);
		log.append(line).append(LF);
		writer.write(line);
		writer.write(CRLF);
		writer.flush();
	}

	public void writeContinue(String message) throws IOException {
		if (message == null) {
			writeLine(continuation);
		} else {
			writeLine(continuation + " " + message);
		}
	}

	public void writeError(String message) throws IOException {
		Assert.isNotEmpty(message, "message");
		writeLine(message);
	}

}
