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
import static org.assertj.core.api.Assumptions.assumeThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class MailboxTest {

	private final Mailbox mailbox = new Mailbox("alice", "password123", "alice@localhost");

	@Test
	void getUsername() {
		assertThat(mailbox.getUsername()).isEqualTo("alice");
	}

	@Test
	void getSecret() {
		assertThat(mailbox.getSecret()).isEqualTo("password123");
	}

	@Test
	void getEmail() {
		assertThat(mailbox.getEmail()).isEqualTo("alice@localhost");
	}

	@Test
	void getMessages() {

		// test: empty mailbox
		List<Mailbox.Message> messages = mailbox.getMessages();
		assertThat(messages).isEmpty();

		// prepare: add first message
		mailbox.addMessage("This is test message 1.");

		// test
		messages = mailbox.getMessages();

		// assert: one message in mailbox
		assertThat(messages).hasSize(1);

		// prepare: add second message
		mailbox.addMessage("This is test message 2.");

		// test
		messages = mailbox.getMessages();

		// assert: two messages in mailbox
		assertThat(messages).hasSize(2);

	}

	@Test
	void addMessage() {

		// prepare
		mailbox.addMessage("This is test message 1.");
		mailbox.addMessage("This is test message 2.");

		// test
		List<Mailbox.Message> messages = mailbox.getMessages();

		// assert
		assertThat(messages).hasSize(2);
		Mailbox.Message message = messages.get(0);
		assertThat(message.getContent()).isEqualTo("This is test message 1.");
		message = messages.get(1);
		assertThat(message.getContent()).isEqualTo("This is test message 2.");
	}

	@Test
	void removeMessages() {

		// prepare
		mailbox.addMessage("This is test message 1.");
		mailbox.addMessage("This is test message 2.");

		// assume
		List<Mailbox.Message> messages = mailbox.getMessages();
		assumeThat(messages).hasSize(2);

		// test
		mailbox.removeMessage(2);

		// assert: only message 1 is left in mailbox
		messages = mailbox.getMessages();
		assertThat(messages).hasSize(1);
		Mailbox.Message message = messages.get(0);
		assertThat(message.getContent()).isEqualTo("This is test message 1.");

		// test
		mailbox.removeMessage(1);

		// assert: mailbox is empty
		messages = mailbox.getMessages();
		assertThat(messages).isEmpty();

	}

	@Test
	void removeDeletedMessages() {

		// prepare
		mailbox.addMessage("This is test message 1.");
		mailbox.addMessage("This is test message 2.");

		// assume
		List<Mailbox.Message> messages = mailbox.getMessages();
		assumeThat(messages).hasSize(2);

		// prepare: mark first message as deleted
		Mailbox.Message message = messages.get(0);
		message.setDeleted(true);

		// test
		mailbox.removeDeletedMessages();

		// assert: only message 2 is left in mailbox
		messages = mailbox.getMessages();
		assertThat(messages).hasSize(1);
		message = messages.get(0);
		assertThat(message.getContent()).isEqualTo("This is test message 2.");

		// prepare: mark last message as deleted
		message.setDeleted(true);

		// test
		mailbox.removeDeletedMessages();

		// assert: mailbox is empty
		messages = mailbox.getMessages();
		assertThat(messages).isEmpty();

	}

}