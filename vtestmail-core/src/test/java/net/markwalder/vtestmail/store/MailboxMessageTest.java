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

package net.markwalder.vtestmail.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MailboxMessageTest {

	@Test
	void getUID() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");

		// test
		int uid = message.getUID();

		// assert
		assertThat(uid).isEqualTo(1611570434);
	}

	@Test
	void getSize() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");

		// test
		int size = message.getSize();

		// assert
		assertThat(size).isEqualTo(23);
	}


	@Test
	void getFlags() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");
		message.setFlag(MailboxMessage.FLAG_SEEN);
		message.setFlag(MailboxMessage.FLAG_ANSWERED);
		message.setFlag(MailboxMessage.KEYWORD_NOTJUNK);

		// test
		List<String> flags = message.getFlags();

		// assert
		assertThat(flags).containsExactly(MailboxMessage.KEYWORD_NOTJUNK, MailboxMessage.FLAG_ANSWERED, MailboxMessage.FLAG_SEEN);

	}

	@Test
	void hasFlag() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");
		message.setFlag(MailboxMessage.FLAG_FLAGGED);

		// test & assert
		assertTrue(message.hasFlag(MailboxMessage.FLAG_FLAGGED));
		assertFalse(message.hasFlag(MailboxMessage.FLAG_DELETED));

	}

	@Test
	void setFlag() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");

		// assume
		assertFalse(message.hasFlag(MailboxMessage.FLAG_DELETED));

		// test
		message.setFlag(MailboxMessage.FLAG_DELETED);

		// assert
		assertTrue(message.hasFlag(MailboxMessage.FLAG_DELETED));

	}

	@Test
	void setFlag_Junk_NotJunk() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");

		// assume
		assertFalse(message.hasFlag(MailboxMessage.KEYWORD_JUNK));
		assertFalse(message.hasFlag(MailboxMessage.KEYWORD_NOTJUNK));

		// test
		message.setFlag(MailboxMessage.KEYWORD_JUNK);

		// assert
		assertTrue(message.hasFlag(MailboxMessage.KEYWORD_JUNK));
		assertFalse(message.hasFlag(MailboxMessage.KEYWORD_NOTJUNK));

		// test
		message.setFlag(MailboxMessage.KEYWORD_NOTJUNK);

		// assert
		assertFalse(message.hasFlag(MailboxMessage.KEYWORD_JUNK));
		assertTrue(message.hasFlag(MailboxMessage.KEYWORD_NOTJUNK));

	}

	@Test
	void removeFlag() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");
		message.setFlag(MailboxMessage.FLAG_RECENT);

		// assume
		assertTrue(message.hasFlag(MailboxMessage.FLAG_RECENT));

		// test
		message.removeFlag(MailboxMessage.FLAG_RECENT);

		// assert
		assertFalse(message.hasFlag(MailboxMessage.FLAG_RECENT));

	}

	@Test
	void isSeen() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");

		// test & assert
		assertFalse(message.isSeen());

		// prepare
		message.setFlag(MailboxMessage.FLAG_SEEN);

		// test & assert
		assertTrue(message.isSeen());
	}

	@Test
	void setSeen() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");

		// assume
		assertFalse(message.isSeen());

		// test
		message.setSeen(true);

		// assert
		assertTrue(message.isSeen());

		// test
		message.setSeen(false);

		// assert
		assertFalse(message.isSeen());
	}

	@Test
	void isAnswered() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");

		// test & assert
		assertFalse(message.isAnswered());

		// prepare
		message.setFlag(MailboxMessage.FLAG_ANSWERED);

		// test & assert
		assertTrue(message.isAnswered());
	}

	@Test
	void setAnswered() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");

		// assume
		assertFalse(message.isAnswered());

		// test
		message.setAnswered(true);

		// assert
		assertTrue(message.isAnswered());

		// test
		message.setAnswered(false);

		// assert
		assertFalse(message.isAnswered());
	}

	@Test
	void isFlagged() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");

		// test & assert
		assertFalse(message.isFlagged());

		// prepare
		message.setFlag(MailboxMessage.FLAG_FLAGGED);

		// test & assert
		assertTrue(message.isFlagged());
	}

	@Test
	void setFlagged() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");

		// assume
		assertFalse(message.isFlagged());

		// test
		message.setFlagged(true);

		// assert
		assertTrue(message.isFlagged());

		// test
		message.setFlagged(false);

		// assert
		assertFalse(message.isFlagged());
	}

	@Test
	void isDeleted() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");

		// test & assert
		assertFalse(message.isDeleted());

		// prepare
		message.setFlag(MailboxMessage.FLAG_DELETED);

		// test & assert
		assertTrue(message.isDeleted());
	}

	@Test
	void setDeleted() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");

		// assume
		assertFalse(message.isDeleted());

		// test
		message.setDeleted(true);

		// assert
		assertTrue(message.isDeleted());

		// test
		message.setDeleted(false);

		// assert
		assertFalse(message.isDeleted());
	}

	@Test
	void isDraft() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");

		// test & assert
		assertFalse(message.isDraft());

		// prepare
		message.setFlag(MailboxMessage.FLAG_DRAFT);

		// test & assert
		assertTrue(message.isDraft());
	}

	@Test
	void setDraft() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");

		// assume
		assertFalse(message.isDraft());

		// test
		message.setDraft(true);

		// assert
		assertTrue(message.isDraft());

		// test
		message.setDraft(false);

		// assert
		assertFalse(message.isDraft());
	}

	@Test
	void isRecent() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");

		// test & assert
		assertFalse(message.isRecent());

		// prepare
		message.setFlag(MailboxMessage.FLAG_RECENT);

		// test & assert
		assertTrue(message.isRecent());
	}

	@Test
	void setRecent() {

		// prepare
		MailboxMessage message = new MailboxMessage("This is a test message.");

		// assume
		assertFalse(message.isRecent());

		// test
		message.setRecent(true);

		// assert
		assertTrue(message.isRecent());

		// test
		message.setRecent(false);

		// assert
		assertFalse(message.isRecent());
	}

	@Test
	void getTop_onlyHeaders() {

		// prepare
		MailboxMessage message = new MailboxMessage("From: X\r\nTo: Y\r\nSubject: Test\r\n\r\nThis is a\r\ntest message.");

		// test
		String top = message.getTop(0);

		// assert
		assertThat(top).isEqualTo("From: X\r\nTo: Y\r\nSubject: Test");
	}

	@Test
	void getTop_ifMessageIsShorter() {

		// prepare
		MailboxMessage message = new MailboxMessage("From: X\r\nTo: Y\r\nSubject: Test\r\n\r\nThis is a\r\ntest message.");

		// test
		String top = message.getTop(3);

		// assert
		assertThat(top).isEqualTo("From: X\r\nTo: Y\r\nSubject: Test\r\n\r\nThis is a\r\ntest message.");
	}

	@Test
	void getTop_ifMessageIsLonger() {

		// prepare
		MailboxMessage message = new MailboxMessage("From: X\r\nTo: Y\r\nSubject: Test\r\n\r\nThis is a\r\ntest message.");

		// test
		String top = message.getTop(1);

		// assert
		assertThat(top).isEqualTo("From: X\r\nTo: Y\r\nSubject: Test\r\n\r\nThis is a");
	}

}