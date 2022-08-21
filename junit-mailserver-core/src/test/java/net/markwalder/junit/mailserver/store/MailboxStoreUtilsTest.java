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

package net.markwalder.junit.mailserver.store;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.markwalder.junit.mailserver.testutils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MailboxStoreUtilsTest {

	@Test
	void load() throws IOException {

		// prepare
		InputStream stream = TestUtils.openResource("mailbox-store.xml");

		// test
		MailboxStore store = MailboxStoreUtils.load(stream);

		// assert
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		MailboxStoreUtils.store(store, out);
		String xml = out.toString(StandardCharsets.UTF_8);
		String expectedXml = TestUtils.readResource("mailbox-store.xml");
		Assertions.assertEquals(expectedXml, xml);

	}

	@Test
	void store() throws IOException {

		// prepare
		MailboxStore store = new MailboxStore();

		{
			Mailbox mailbox = store.createMailbox("user1", "secret1", "user1@localhost");
			MailboxFolder inbox = mailbox.getInbox();
			MailboxMessage message1 = inbox.addMessage("Subject: Test 1\r\n\r\nThis is test message 1.");
			message1.setFlag(MailboxMessage.FLAG_SEEN);
			message1.setFlag(MailboxMessage.FLAG_FLAGGED);
			inbox.addMessage("Subject: Test 2\r\n\r\nThis is test message 2.");
			MailboxFolder folder = mailbox.createFolder("Drafts");
			MailboxMessage message3 = folder.addMessage("Subject: Test 3\r\n\r\nThis is a draft.");
			message3.setFlag(MailboxMessage.FLAG_DRAFT);
		}

		{
			Mailbox mailbox = store.createMailbox("user2", "secret2", "user2@localhost");
			MailboxFolder inbox = mailbox.getInbox();
			MailboxMessage message1 = inbox.addMessage("Subject: Test 1\r\n\r\nThis is test message 1.");
			message1.setFlag(MailboxMessage.FLAG_SEEN);
			message1.setFlag(MailboxMessage.FLAG_ANSWERED);
			inbox.addMessage("Subject: Test 2\r\n\r\nThis is test message 2.");
			mailbox.createFolder("Drafts"); // empty folder
			MailboxFolder folder = mailbox.createFolder("Trash");
			MailboxMessage message3 = folder.addMessage("Subject: Test 3\r\n\r\nThis is spam.");
			message3.setFlag(MailboxMessage.FLAG_DELETED);
			message3.setFlag(MailboxMessage.KEYWORD_JUNK);
		}

		{
			Mailbox mailbox = new Mailbox("user3", "secret3", "user3@localhost");
			store.addMailbox(mailbox); // empty mailbox
		}

		// test
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		MailboxStoreUtils.store(store, stream);

		// assert
		String xml = stream.toString(StandardCharsets.UTF_8);
		String expectedXml = TestUtils.readResource("mailbox-store.xml");
		Assertions.assertEquals(expectedXml, xml);

	}

}