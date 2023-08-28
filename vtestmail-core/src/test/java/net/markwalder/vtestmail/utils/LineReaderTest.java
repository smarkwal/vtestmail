/*
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class LineReaderTest {

	@Test
	void readLine_fromEmptyStream() throws IOException {
		LineReader reader = createLineReader("");
		assertEndOfStream(reader);
	}

	@Test
	void readLine_fromCRLF() throws IOException {
		LineReader reader = createLineReader("\r\n");
		assertEquals("", reader.readLine());
		assertEquals("", reader.readLine());
		assertEndOfStream(reader);
	}

	@Test
	void readLine_fromCRLFCRLF() throws IOException {
		LineReader reader = createLineReader("\r\n\r\n");
		assertEquals("", reader.readLine());
		assertEquals("", reader.readLine());
		assertEquals("", reader.readLine());
		assertEndOfStream(reader);
	}

	@Test
	void readLine_fromStreamWithSingleLine() throws IOException {
		LineReader reader = createLineReader("line1");
		assertEquals("line1", reader.readLine());
		assertEndOfStream(reader);
	}

	@Test
	void readLine_fromStreamWithMultipleLines() throws IOException {
		LineReader reader = createLineReader("line1\r\nline2\r\nline3");
		assertEquals("line1", reader.readLine());
		assertEquals("line2", reader.readLine());
		assertEquals("line3", reader.readLine());
		assertEndOfStream(reader);
	}

	@Test
	void readLine_fromStreamWithSingleCR() throws IOException {
		LineReader reader = createLineReader("line1\rline2");
		assertEquals("line1\rline2", reader.readLine());
		assertEndOfStream(reader);
	}

	@Test
	void readLine_fromStreamWithSingleLF() throws IOException {
		LineReader reader = createLineReader("line1\nline2");
		assertEquals("line1\nline2", reader.readLine());
		assertEndOfStream(reader);
	}

	@Test
	void readLine_fromStreamWithDoubleCR() throws IOException {
		LineReader reader = createLineReader("line1\r\rline2");
		assertEquals("line1\r\rline2", reader.readLine());
		assertEndOfStream(reader);
	}

	@Test
	void readLine_fromStreamWithDoubleLF() throws IOException {
		LineReader reader = createLineReader("line1\n\nline2");
		assertEquals("line1\n\nline2", reader.readLine());
		assertEndOfStream(reader);
	}

	@Test
	void readLine_fromStreamWithLFCR() throws IOException {
		LineReader reader = createLineReader("line1\n\rline2");
		assertEquals("line1\n\rline2", reader.readLine());
		assertEndOfStream(reader);
	}

	@Test
	void readLine_fromStreamEndingWithCRLF() throws IOException {
		LineReader reader = createLineReader("line1\r\n");
		assertEquals("line1", reader.readLine());
		assertEquals("", reader.readLine());
		assertEndOfStream(reader);
	}

	@Test
	void readLine_fromStreamEndingWithCR() throws IOException {
		LineReader reader = createLineReader("line1\r");
		assertEquals("line1\r", reader.readLine());
		assertEndOfStream(reader);
	}

	@Test
	void readLine_fromStreamEndingWithLF() throws IOException {
		LineReader reader = createLineReader("line1\n");
		assertEquals("line1\n", reader.readLine());
		assertEndOfStream(reader);
	}

	private void assertEndOfStream(LineReader reader) throws IOException {
		assertNull(reader.readLine());
		assertNull(reader.readLine());
		assertNull(reader.readLine());
	}

	private static LineReader createLineReader(String text) {
		return new LineReader(new StringReader(text));
	}

}