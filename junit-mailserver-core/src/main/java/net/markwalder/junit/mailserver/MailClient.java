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

package net.markwalder.junit.mailserver;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import net.markwalder.junit.mailserver.utils.Assert;
import net.markwalder.junit.mailserver.utils.LineReader;

/**
 * Mail client connection.
 */
public abstract class MailClient {

	private static final Charset CHARSET = StandardCharsets.ISO_8859_1;

	private static final String CRLF = "\r\n";
	private static final String LF = "\n";

	private final LineReader reader;
	private final BufferedWriter writer;
	private final StringBuilder log;
	private final String continuation;

	protected MailClient(Socket socket, StringBuilder log, String continuation) throws IOException {
		Assert.isNotNull(socket, "socket");
		Assert.isNotNull(log, "log");
		Assert.isNotEmpty(continuation, "continuation");

		this.continuation = continuation;

		InputStream inputStream = socket.getInputStream();
		this.reader = new LineReader(new InputStreamReader(inputStream, CHARSET));

		OutputStream outputStream = socket.getOutputStream();
		this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, CHARSET));

		this.log = log;
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
		System.out.println("Client: " + line);
		log.append(line).append(LF);
		return line;
	}

	/**
	 * Send a line of text to the client, followed by a CRLF line break.
	 *
	 * @param line Line of text to send.
	 * @throws IOException If an I/O error occurs.
	 */
	public void writeLine(String line) throws IOException {
		Assert.isNotNull(line, "line");
		System.out.println("Server: " + line);
		log.append(line).append(LF);
		writer.write(line);
		writer.write(CRLF);
		writer.flush();
	}

	public void writeContinue(String message) throws IOException {
		if (message == null || message.isEmpty()) {
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
