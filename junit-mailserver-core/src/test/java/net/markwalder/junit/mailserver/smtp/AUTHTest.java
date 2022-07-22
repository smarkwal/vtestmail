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
import net.markwalder.junit.mailserver.MailboxStore;
import net.markwalder.junit.mailserver.auth.Authenticator;
import net.markwalder.junit.mailserver.auth.Credentials;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AUTHTest extends CommandTest {

	private final Authenticator authenticator = Mockito.mock(Authenticator.class);
	private final Credentials credentials = Mockito.mock(Credentials.class);
	private final MailboxStore store = Mockito.mock(MailboxStore.class);

	@Test
	void execute() throws ProtocolException, IOException {

		// mock
		Mockito.doReturn(true).when(server).isAuthTypeSupported("LOGIN");
		Mockito.doReturn(authenticator).when(server).getAuthenticator("LOGIN");
		Mockito.doReturn(store).when(server).getStore();
		Mockito.doReturn(credentials).when(authenticator).authenticate(null, client, store);
		Mockito.doReturn("alice").when(credentials).getUsername();
		Mockito.doReturn("password123").when(credentials).getSecret();
		Mockito.doReturn(true).when(server).isAuthenticated();

		// prepare
		Command command = new AUTH();

		// test
		command.execute("AUTH LOGIN", server, client);

		// verify
		Mockito.verify(server).logout();
		Mockito.verify(server).isAuthTypeSupported("LOGIN");
		Mockito.verify(server).getAuthenticator("LOGIN");
		Mockito.verify(server).getStore();
		Mockito.verify(authenticator).authenticate(null, client, store);
		Mockito.verify(credentials).getUsername();
		Mockito.verify(credentials).getSecret();
		Mockito.verify(server).login("alice", "password123");
		Mockito.verify(server).isAuthenticated();
		Mockito.verify(client).writeLine("235 2.7.0 Authentication succeeded");

		Mockito.verifyNoMoreInteractions(server, client, authenticator, credentials, store);
	}

	@Test
	void execute_unsupportedAuthType() {

		// mock
		Mockito.doReturn(false).when(server).isAuthTypeSupported("LOGIN");

		// prepare
		Command command = new AUTH();

		// test
		Exception exception = assertThrows(ProtocolException.class, () -> command.execute("AUTH LOGIN", server, client));

		// assert
		assertThat(exception).hasMessage("504 5.5.4 Unrecognized authentication type");

		// verify
		Mockito.verify(server).logout();
		Mockito.verify(server).isAuthTypeSupported("LOGIN");

		Mockito.verifyNoMoreInteractions(server, client, authenticator, credentials, store);
	}

	@Test
	void execute_wrongPassword() throws IOException {

		// mock
		Mockito.doReturn(true).when(server).isAuthTypeSupported("PLAIN");
		Mockito.doReturn(authenticator).when(server).getAuthenticator("PLAIN");
		Mockito.doReturn(store).when(server).getStore();
		Mockito.doReturn(credentials).when(authenticator).authenticate(null, client, store);
		Mockito.doReturn("alice").when(credentials).getUsername();
		Mockito.doReturn("password123").when(credentials).getSecret();
		Mockito.doReturn(false).when(server).isAuthenticated();

		// prepare
		Command command = new AUTH();

		// test
		Exception exception = assertThrows(ProtocolException.class, () -> command.execute("AUTH PLAIN", server, client));

		// assert
		assertThat(exception).hasMessage("535 5.7.8 Authentication failed");

		// verify
		Mockito.verify(server).logout();
		Mockito.verify(server).isAuthTypeSupported("PLAIN");
		Mockito.verify(server).getAuthenticator("PLAIN");
		Mockito.verify(server).getStore();
		Mockito.verify(authenticator).authenticate(null, client, store);
		Mockito.verify(credentials).getUsername();
		Mockito.verify(credentials).getSecret();
		Mockito.verify(server).login("alice", "password123");
		Mockito.verify(server).isAuthenticated();

		Mockito.verifyNoMoreInteractions(server, client, authenticator, credentials, store);
	}

}