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
import java.nio.charset.StandardCharsets;
import net.markwalder.junit.mailserver.utils.LineReader;

/**
 * Mail client connection.
 */
public class Client {

	private static final String CRLF = "\r\n";
	private static final String LF = "\n";

	private final LineReader reader;
	private final BufferedWriter writer;
	private final StringBuilder log;

	Client(Socket socket, StringBuilder log) throws IOException {

		InputStream inputStream = socket.getInputStream();
		this.reader = new LineReader(new InputStreamReader(inputStream, StandardCharsets.US_ASCII));

		OutputStream outputStream = socket.getOutputStream();
		this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.US_ASCII));

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
		System.out.println("Server: " + line);
		log.append(line).append(LF);
		writer.write(line);
		writer.write(CRLF);
		writer.flush();
	}

}
