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

package net.markwalder.junit.mailserver.pop3;

import java.io.IOException;
import java.net.Socket;
import net.markwalder.junit.mailserver.MailClient;
import net.markwalder.junit.mailserver.utils.Assert;

public class Pop3Client extends MailClient {

	protected Pop3Client(Socket socket, StringBuilder log) throws IOException {
		super(socket, log, "+OK");
	}

	public void writeMultiLines(String message) throws IOException {

		// split message into lines
		String[] lines = message.split(CRLF, -1);

		// send every line separately
		for (String line : lines) {

			// see "byte-stuffed" in https://www.ietf.org/rfc/rfc1939.html#section-3
			if (line.startsWith(".")) {
				line = "." + line;
			}

			writeLine(line);
		}

		// send termination octet on last line (CRLF.CRLF)
		writeLine(".");
	}

	@Override
	public void writeError(String message) throws IOException {
		Assert.isNotEmpty(message, "message");
		writeLine("-ERR " + message);
	}

}
