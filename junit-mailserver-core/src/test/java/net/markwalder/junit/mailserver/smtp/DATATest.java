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

package net.markwalder.junit.mailserver.smtp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import net.markwalder.junit.mailserver.Mailbox;
import net.markwalder.junit.mailserver.MailboxStore;
import net.markwalder.junit.mailserver.testutils.TestUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DATATest extends CommandTest {

	private final MailboxStore store = Mockito.mock(MailboxStore.class);
	private final Mailbox mailbox = Mockito.mock(Mailbox.class);
	private final Mailbox.Folder folder = Mockito.mock(Mailbox.Folder.class);

	@Test
	void execute() throws SmtpException, IOException {

		// mock
		Mockito.doReturn(false).when(server).isAuthenticationRequired();
		Mockito.doReturn(
				"Subject: Test",
				"",
				"Hello World!",
				"..",
				"."
		).when(client).readLine();
		Mockito.doReturn("client").when(session).getClientAddress();
		Mockito.doReturn("server").when(session).getServerAddress();
		Mockito.doReturn(TestUtils.createTestClock()).when(server).getClock();
		Mockito.doReturn(store).when(server).getStore();
		Mockito.doReturn(Collections.singletonList("alice@localhost")).when(session).getRecipients();
		Mockito.doReturn(mailbox).when(store).findMailbox("alice@localhost");
		Mockito.doReturn(folder).when(mailbox).getInbox();

		// prepare
		SmtpCommand command = new DATA();

		// test
		command.execute(server, session, client);

		// verify
		Mockito.verify(server).isAuthenticationRequired();
		Mockito.verify(client).writeLine("354 Send message, end with <CRLF>.<CRLF>");
		Mockito.verify(client, Mockito.times(5)).readLine();
		Mockito.verify(session).getClientAddress();
		Mockito.verify(session).getServerAddress();
		Mockito.verify(server).getClock();
		Mockito.verify(server).getStore();
		Mockito.verify(session).getRecipients();
		Mockito.verify(store).findMailbox("alice@localhost");
		Mockito.verify(mailbox).getInbox();
		Mockito.verify(folder).addMessage("Received: from client by server; Wed, 1 Jan 2020 00:00:00 +0000\r\nSubject: Test\r\n\r\nHello World!\r\n.");
		Mockito.verify(session).endTransaction("Received: from client by server; Wed, 1 Jan 2020 00:00:00 +0000\r\nSubject: Test\r\n\r\nHello World!\r\n.");
		Mockito.verify(client).writeLine("250 2.6.0 Message accepted");

		Mockito.verifyNoMoreInteractions(server, session, client, store, mailbox);
	}

	@Test
	void execute_notAuthenticated() {

		// mock
		Mockito.doReturn(true).when(server).isAuthenticationRequired();

		// prepare
		SmtpCommand command = new DATA();

		// test
		Exception exception = assertThrows(SmtpException.class, () -> command.execute(server, session, client));

		// assert
		assertThat(exception).hasMessage("530 5.7.0 Authentication required");

		// verify
		Mockito.verify(server).isAuthenticationRequired();

		Mockito.verifyNoMoreInteractions(server, session, client, store, mailbox);
	}

	@Test
	void formatDateTime() {

		Clock clock = TestUtils.createTestClock();
		String result = DATA.formatDateTime(clock);
		assertThat(result).isEqualTo("Wed, 1 Jan 2020 00:00:00 +0000");

		clock = TestUtils.createTestClock(1977, 2, 24, 13, 17, 34);
		result = DATA.formatDateTime(clock);
		assertThat(result).isEqualTo("Thu, 24 Feb 1977 13:17:34 +0000");

		clock = Clock.fixed(Instant.EPOCH, ZoneId.of("UTC"));
		result = DATA.formatDateTime(clock);
		assertThat(result).isEqualTo("Thu, 1 Jan 1970 00:00:00 +0000");

		clock = Clock.fixed(Instant.ofEpochMilli(1234567890123L), ZoneId.of("CET"));
		result = DATA.formatDateTime(clock);
		assertThat(result).isEqualTo("Sat, 14 Feb 2009 00:31:30 +0100");
	}

}