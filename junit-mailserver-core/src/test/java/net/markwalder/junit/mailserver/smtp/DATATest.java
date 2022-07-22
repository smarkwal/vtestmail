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
import java.util.Collections;
import net.markwalder.junit.mailserver.Mailbox;
import net.markwalder.junit.mailserver.MailboxStore;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DATATest extends CommandTest {

	private final MailboxStore store = Mockito.mock(MailboxStore.class);
	private final Mailbox mailbox = Mockito.mock(Mailbox.class);

	@Test
	void execute() throws ProtocolException, IOException {

		// mock
		Mockito.doReturn(false).when(server).isAuthenticationRequired();
		Mockito.doReturn(
				"Subject: Test",
				"",
				"Hello World!",
				"..",
				"."
		).when(client).readLine();
		Mockito.doReturn(store).when(server).getStore();
		Mockito.doReturn(Collections.singletonList("alice@localhost")).when(server).getRecipients();
		Mockito.doReturn(mailbox).when(store).findMailbox("alice@localhost");

		// prepare
		Command command = new DATA();

		// test
		command.execute("DATA", server, client);

		// verify
		Mockito.verify(server).isAuthenticationRequired();
		Mockito.verify(client).writeLine("354 Send message, end with <CRLF>.<CRLF>");
		Mockito.verify(client, Mockito.times(5)).readLine();
		Mockito.verify(server).getStore();
		Mockito.verify(server).getRecipients();
		Mockito.verify(store).findMailbox("alice@localhost");
		Mockito.verify(mailbox).addMessage("Subject: Test\r\n\r\nHello World!\r\n.");
		Mockito.verify(client).writeLine("250 2.6.0 Message accepted");

		Mockito.verifyNoMoreInteractions(server, client, store, mailbox);
	}

	@Test
	void execute_notAuthenticated() {

		// mock
		Mockito.doReturn(true).when(server).isAuthenticationRequired();

		// prepare
		Command command = new DATA();

		// test
		Exception exception = assertThrows(ProtocolException.class, () -> command.execute("DATA", server, client));

		// assert
		assertThat(exception).hasMessage("530 5.7.0 Authentication required");

		// verify
		Mockito.verify(server).isAuthenticationRequired();

		Mockito.verifyNoMoreInteractions(server, client, store, mailbox);
	}

}