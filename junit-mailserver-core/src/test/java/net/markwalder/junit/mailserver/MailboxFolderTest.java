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

class MailboxFolderTest {

	private final Mailbox.Folder folder = new Mailbox.Folder("INBOX");

	@Test
	void getName() {
		assertThat(folder.getName()).isEqualTo("INBOX");
	}

	@Test
	void getMessages() {

		// test: empty mailbox
		List<Mailbox.Message> messages = folder.getMessages();
		assertThat(messages).isEmpty();

		// prepare: add first message
		folder.addMessage("This is test message 1.");

		// test
		messages = folder.getMessages();

		// assert: one message in mailbox
		assertThat(messages).hasSize(1);

		// prepare: add second message
		folder.addMessage("This is test message 2.");

		// test
		messages = folder.getMessages();

		// assert: two messages in mailbox
		assertThat(messages).hasSize(2);

	}

	@Test
	void addMessage() {

		// prepare
		folder.addMessage("This is test message 1.");
		folder.addMessage("This is test message 2.");

		// test
		List<Mailbox.Message> messages = folder.getMessages();

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
		folder.addMessage("This is test message 1.");
		folder.addMessage("This is test message 2.");

		// assume
		List<Mailbox.Message> messages = folder.getMessages();
		assumeThat(messages).hasSize(2);

		// test
		folder.removeMessage(2);

		// assert: only message 1 is left in mailbox
		messages = folder.getMessages();
		assertThat(messages).hasSize(1);
		Mailbox.Message message = messages.get(0);
		assertThat(message.getContent()).isEqualTo("This is test message 1.");

		// test
		folder.removeMessage(1);

		// assert: mailbox is empty
		messages = folder.getMessages();
		assertThat(messages).isEmpty();

	}

	@Test
	void removeDeletedMessages() {

		// prepare
		folder.addMessage("This is test message 1.");
		folder.addMessage("This is test message 2.");

		// assume
		List<Mailbox.Message> messages = folder.getMessages();
		assumeThat(messages).hasSize(2);

		// prepare: mark first message as deleted
		Mailbox.Message message = messages.get(0);
		message.setDeleted(true);

		// test
		folder.removeDeletedMessages();

		// assert: only message 2 is left in mailbox
		messages = folder.getMessages();
		assertThat(messages).hasSize(1);
		message = messages.get(0);
		assertThat(message.getContent()).isEqualTo("This is test message 2.");

		// prepare: mark last message as deleted
		message.setDeleted(true);

		// test
		folder.removeDeletedMessages();

		// assert: mailbox is empty
		messages = folder.getMessages();
		assertThat(messages).isEmpty();

	}

	// TODO: add tests for uidnext and uidvalidity

}