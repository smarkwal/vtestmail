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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MailboxMessageTest {

	@Test
	void getUID() {

		// prepare
		Mailbox.Message message = new Mailbox.Message("This is a test message.");

		// test
		String uid = message.getUID();

		// assert
		assertThat(uid).isEqualTo("f8900247f0d5874f453318549411c6fa");
	}

	@Test
	void getSize() {

		// prepare
		Mailbox.Message message = new Mailbox.Message("This is a test message.");

		// test
		int size = message.getSize();

		// assert
		assertThat(size).isEqualTo(23);
	}

	@Test
	void getTop_ifMessageIsShorter() {

		// prepare
		Mailbox.Message message = new Mailbox.Message("This is a test message.");

		// test
		String top = message.getTop(3);

		// assert
		assertThat(top).isEqualTo("This is a test message.");
	}

	@Test
	void getTop_ifMessageIsLonger() {

		// prepare
		Mailbox.Message message = new Mailbox.Message("This\r\nis\r\ntest\r\nmessage.");

		// test
		String top = message.getTop(2);

		// assert
		assertThat(top).isEqualTo("This\r\nis");
	}

	@Test
	void getTop_withMultipleEmptyLines() {

		// prepare
		Mailbox.Message message = new Mailbox.Message("\r\n\r\nThis\r\n\r\nis a\rtest\nmessage.\r\nBest, Ste\r\n\r\n");

		// test
		String top = message.getTop(1);

		// assert
		assertThat(top).isEqualTo("");

		// test
		top = message.getTop(2);

		// assert
		assertThat(top).isEqualTo("\r\n");

		// test
		top = message.getTop(3);

		// assert
		assertThat(top).isEqualTo("\r\n\r\nThis");

		// test
		top = message.getTop(4);

		// assert
		assertThat(top).isEqualTo("\r\n\r\nThis\r\n");

		// test
		top = message.getTop(5);

		// assert
		assertThat(top).isEqualTo("\r\n\r\nThis\r\n\r\nis a\rtest\nmessage.");

		// test
		top = message.getTop(6);

		// assert
		assertThat(top).isEqualTo("\r\n\r\nThis\r\n\r\nis a\rtest\nmessage.\r\nBest, Ste");

		// test
		top = message.getTop(7);

		// assert
		assertThat(top).isEqualTo("\r\n\r\nThis\r\n\r\nis a\rtest\nmessage.\r\nBest, Ste\r\n");

		// test
		top = message.getTop(8);

		// assert
		assertThat(top).isEqualTo("\r\n\r\nThis\r\n\r\nis a\rtest\nmessage.\r\nBest, Ste\r\n\r\n");

		// test
		top = message.getTop(9);

		// assert
		assertThat(top).isEqualTo("\r\n\r\nThis\r\n\r\nis a\rtest\nmessage.\r\nBest, Ste\r\n\r\n");

	}

}