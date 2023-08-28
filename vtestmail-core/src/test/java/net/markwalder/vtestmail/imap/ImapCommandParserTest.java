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

package net.markwalder.vtestmail.imap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ImapCommandParserTest {

	@Test
	void readMailbox() throws ImapException {
		// prepare
		String mailbox = "INBOX";
		ImapCommandParser parser = new ImapCommandParser(mailbox);
		// test
		String result = parser.readMailbox();
		// assert
		assertEquals(mailbox, result);
		parser.assertNoMoreArguments();
	}

	@Test
	void readMailbox_quotedString() throws ImapException {
		// prepare
		String mailbox = "Client \\\"Demo\\\"";
		ImapCommandParser parser = new ImapCommandParser("\"" + mailbox + "\"");
		// test
		String result = parser.readMailbox();
		// assert
		assertEquals("Client \"Demo\"", result);
		parser.assertNoMoreArguments();
	}

	@Test
	void readMailbox_quotedString_Utf8() throws ImapException {
		// prepare
		String mailbox = "\u5DE5\u4F5C"; // "Work" in Chinese (simplified)
		ImapCommandParser parser = new ImapCommandParser("\"" + mailbox + "\"");
		// test
		String result = parser.readMailbox();
		// assert
		assertEquals(mailbox, result);
		parser.assertNoMoreArguments();
	}

	@Test
	void read_literal_synchronizing() throws ImapException {
		// prepare
		String parameters = "{26}\r\nThis is some example text.";
		ImapCommandParser parser = new ImapCommandParser(parameters);
		// test
		String result = parser.readMailbox();
		// assert
		assertEquals("This is some example text.", result);
		parser.assertNoMoreArguments();
	}

	@Test
	void read_literal_non_synchronizing() throws ImapException {
		// prepare
		String parameters = "{27+}\r\nThis is some\r\nexample text.";
		ImapCommandParser parser = new ImapCommandParser(parameters);
		// test
		String result = parser.readMailbox();
		// assert
		assertEquals("This is some\r\nexample text.", result);
		parser.assertNoMoreArguments();
	}

	@Test
	void read_literal_minValue() throws ImapException {
		// prepare
		String parameters = "{0}\r\n";
		ImapCommandParser parser = new ImapCommandParser(parameters);
		// test
		String result = parser.readMailbox();
		// assert
		assertEquals("", result);
		parser.assertNoMoreArguments();
	}

	@Test
	void read_literal_minValue_non_synchronized() throws ImapException {
		// prepare
		String parameters = "{0+}\r\n";
		ImapCommandParser parser = new ImapCommandParser(parameters);
		// test
		String result = parser.readMailbox();
		// assert
		assertEquals("", result);
		parser.assertNoMoreArguments();
	}

	@Test
	void assertNoMoreArguments() throws ImapException {
		// prepare
		ImapCommandParser parser = new ImapCommandParser("");
		// test
		parser.assertNoMoreArguments();
	}

	@Test
	void assertNoMoreArguments_throwsImapException_ifMoreArguments() throws ImapException {
		// prepare
		ImapCommandParser parser = new ImapCommandParser(" INBOX");
		// test
		ImapException exception = assertThrows(ImapException.class, parser::assertNoMoreArguments);
		// assert
		assertEquals("BAD Syntax error", exception.getMessage());
	}

	@Test
	void rename_command() throws ImapException {
		// prepare
		String parameters = "\"Old Name\" \"New Name\"";
		ImapCommandParser parser = new ImapCommandParser(parameters);
		// test
		String result = parser.readMailbox();
		// assert
		assertEquals("Old Name", result);
		parser.assertMoreArguments();
		// test
		result = parser.readMailbox();
		// assert
		assertEquals("New Name", result);
		parser.assertNoMoreArguments();
	}

	@Test
	void login_command() throws ImapException {
		// prepare
		String parameters = "{5}\r\n\u00E4li\u00E7\u00E9 {12}\r\np\u00E4ssw\u00F6rd!123";
		ImapCommandParser parser = new ImapCommandParser(parameters);
		// test
		String userId = parser.readUserId();
		// assert
		assertEquals("\u00E4li\u00E7\u00E9", userId);
		parser.assertMoreArguments();
		// test
		String password = parser.readPassword();
		// assert
		assertEquals("p\u00E4ssw\u00F6rd!123", password);
		parser.assertNoMoreArguments();
	}

}