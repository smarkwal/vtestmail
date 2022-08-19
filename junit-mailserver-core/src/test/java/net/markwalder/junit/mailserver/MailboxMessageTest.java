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
		int uid = message.getUID();

		// assert
		assertThat(uid).isEqualTo(1611570434);
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
	void getTop_onlyHeaders() {

		// prepare
		Mailbox.Message message = new Mailbox.Message("From: X\r\nTo: Y\r\nSubject: Test\r\n\r\nThis is a\r\ntest message.");

		// test
		String top = message.getTop(0);

		// assert
		assertThat(top).isEqualTo("From: X\r\nTo: Y\r\nSubject: Test");
	}

	@Test
	void getTop_ifMessageIsShorter() {

		// prepare
		Mailbox.Message message = new Mailbox.Message("From: X\r\nTo: Y\r\nSubject: Test\r\n\r\nThis is a\r\ntest message.");

		// test
		String top = message.getTop(3);

		// assert
		assertThat(top).isEqualTo("From: X\r\nTo: Y\r\nSubject: Test\r\n\r\nThis is a\r\ntest message.");
	}

	@Test
	void getTop_ifMessageIsLonger() {

		// prepare
		Mailbox.Message message = new Mailbox.Message("From: X\r\nTo: Y\r\nSubject: Test\r\n\r\nThis is a\r\ntest message.");

		// test
		String top = message.getTop(1);

		// assert
		assertThat(top).isEqualTo("From: X\r\nTo: Y\r\nSubject: Test\r\n\r\nThis is a");
	}


}