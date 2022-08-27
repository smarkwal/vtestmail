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

package net.markwalder.vtestmail.utils;

import java.io.IOException;
import java.io.Reader;

public class LineReader {

	private static final char CR = '\r';
	private static final char LF = '\n';

	/**
	 * Underlying reader.
	 */
	private final Reader reader;

	/**
	 * Buffer to accumulate line data.
	 */
	private final StringBuilder line = new StringBuilder(32);

	/**
	 * Did the last line end with CRLF?
	 */
	private boolean crlf = false;

	/**
	 * Create a new line reader.
	 *
	 * @param reader Underlying reader.
	 */
	public LineReader(Reader reader) {
		Assert.isNotNull(reader, "reader");
		this.reader = reader;
	}

	/**
	 * Read a line separated by CRLF.
	 *
	 * @return Line, or {@code null} if the end of the stream has been reached.
	 * @throws IOException If an I/O error occurs.
	 */
	public String readLine() throws IOException {

		// reset buffer to start
		line.setLength(0);

		// has the last seen character been a CR?
		boolean cr = false;

		while (true) {

			// TODO: implement max line length restriction

			// read next character
			int c = reader.read();

			if (c == -1) { // end of stream

				if (cr) {
					// CR before end of stream
					line.append(CR);
				}

				if (line.length() == 0) {

					if (crlf) {
						// stream ends after CRLF
						// -> return empty line
						crlf = false;
						return "";
					}

					// no more data
					return null;
				}

				// stream ends after line without CRLF
				// -> return last line
				crlf = false;
				return line.toString();
			}

			if (c == CR) {
				if (cr) { // CRCR found
					line.append(CR);
				}

				cr = true;
			} else if (c == LF) {
				if (cr) { // CRLF found
					crlf = true;
					return line.toString();
				}

				line.append((char) c);
			} else {
				if (cr) { // CR without LF found
					line.append(CR);
					cr = false;
				}

				line.append((char) c);
			}
		}

	}

	/**
	 * Read the given number of characters
	 *
	 * @param len Number of characters to read
	 * @return String containing the read characters
	 * @throws IOException If an I/O error occurs.
	 */
	public String readChars(long len) throws IOException {
		StringBuilder buffer = new StringBuilder(len > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) len);
		for (int i = 0; i < len; i++) {
			int c = reader.read();
			if (c == -1) {
				break;
			}
			buffer.append((char) c);
		}
		return buffer.toString();
	}

}