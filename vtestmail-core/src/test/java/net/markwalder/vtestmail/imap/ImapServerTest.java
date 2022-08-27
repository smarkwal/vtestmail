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

package net.markwalder.vtestmail.imap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.markwalder.vtestmail.auth.AuthType;
import net.markwalder.vtestmail.store.Mailbox;
import net.markwalder.vtestmail.store.MailboxFolder;
import net.markwalder.vtestmail.store.MailboxStore;
import net.markwalder.vtestmail.testutils.ImapClient;
import net.markwalder.vtestmail.testutils.JavaUtils;
import net.markwalder.vtestmail.testutils.TestUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

public class ImapServerTest {

	private static final String USERNAME = "\u00E4li\u00E7\u00E9";
	private static final String PASSWORD = "p\u00E4ssw\u00F6rd!123";
	private static final String EMAIL = "alice@localhost";

	@Test
	void getPort() throws IOException {

		// prepare: IMAP server
		MailboxStore store = new MailboxStore();
		try (ImapServer server = new ImapServer(store)) {

			// test: before server has been started
			int port = server.getPort();

			// assert
			assertThat(port).isZero();

			server.start();

			// test: after server has been started
			port = server.getPort();

			// assert
			assertThat(port).isBetween(1024, 65535);
		}
	}

	@Test
	void setPort() throws IOException {

		// prepare: IMAP server
		MailboxStore store = new MailboxStore();
		try (ImapServer server = new ImapServer(store)) {

			// test
			server.setPort(24277);
			server.start();

			// assert
			int port = server.getPort();
			assertThat(port).isEqualTo(24277);
		}
	}

	@TestFactory
	@DisplayName("Encryption")
	Collection<DynamicTest> testEncryption() {

		List<String> sslProtocols = Arrays.asList(
				"SSLv3",
				"TLSv1",
				"TLSv1.1",
				"TLSv1.2",
				"TLSv1.3"
		);

		List<DynamicTest> tests = new ArrayList<>();
		for (String sslProtocol : sslProtocols) {

			DynamicTest test = DynamicTest.dynamicTest(sslProtocol, () -> testEncryption(sslProtocol, false));
			tests.add(test);

			test = DynamicTest.dynamicTest(sslProtocol + " (STARTTLS)", () -> testEncryption(sslProtocol, true));
			tests.add(test);
		}
		return tests;
	}

	private void testEncryption(String sslProtocol, boolean useStartTLS) throws IOException, MessagingException, InterruptedException {

		// STARTTLS not supported for SSLv3 in Java 14+
		// see https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8190492
		assumeFalse(sslProtocol.equals("SSLv3") && useStartTLS && JavaUtils.getJavaVersion() >= 14, "STARTTLS not supported for SSLv3 in Java 14+");

		// prepare: mailbox
		MailboxStore store = new MailboxStore();
		store.createMailbox(USERNAME, PASSWORD, EMAIL);

		// prepare: IMAP server
		try (ImapServer server = new ImapServer(store)) {
			server.setClock(TestUtils.createTestClock());
			server.setAuthTypes(AuthType.PLAIN);
			server.setUseSSL(!useStartTLS);
			server.setSSLProtocol(sslProtocol);
			server.setCommandEnabled("STARTTLS", useStartTLS);
			// TODO: require encryption
			server.start();

			// prepare: IMAP client
			ImapClient client;
			if (useStartTLS) {
				client = ImapClient.forServer(server)
						.withAuthentication(AuthType.PLAIN, USERNAME, PASSWORD)
						.withStartTLS(sslProtocol)
						.build();
			} else {
				client = ImapClient.forServer(server)
						.withAuthentication(AuthType.PLAIN, USERNAME, PASSWORD)
						.withEncryption(sslProtocol)
						.build();
			}

			// test
			List<String> messages = client.getMessages("INBOX");

			// assert
			assertThat(messages).isEmpty();

			List<ImapSession> sessions = server.getSessions();
			assertThat(sessions).hasSize(1);
			ImapSession session = sessions.get(0);
			session.waitUntilClosed(5000);
			Assertions.assertThat(session.getSSLProtocol()).isEqualTo(sslProtocol);
			Assertions.assertThat(session.getCipherSuite()).isNotEmpty();
			Assertions.assertThat(session.isEncrypted()).isTrue();
			Assertions.assertThat(session.isClosed()).isTrue();

			List<ImapCommand> commands = session.getCommands();
			if (useStartTLS) {
				Assertions.assertThat(commands).containsExactly(
						new STARTTLS(),
						new CAPABILITY(),
						new AUTHENTICATE("PLAIN"),
						new EXAMINE("INBOX"),
						new CLOSE(),
						new LOGOUT()
				);
			} else {
				Assertions.assertThat(commands).containsExactly(
						new AUTHENTICATE("PLAIN"),
						new EXAMINE("INBOX"),
						new CLOSE(),
						new LOGOUT()
				);
			}

		}
	}


	@TestFactory
	@DisplayName("Authentication")
	Collection<DynamicTest> testAuthentication() {

		// note: CRAM-MD5 and DIGEST-MD5 are not supported by Jakarta Mail API 2.0.1
		List<String> authTypes = Arrays.asList(
				"DEFAULT",
				AuthType.LOGIN,
				AuthType.PLAIN,
				AuthType.XOAUTH2
		);

		List<DynamicTest> tests = new ArrayList<>();
		for (String authType : authTypes) {
			DynamicTest test = DynamicTest.dynamicTest(authType, () -> testAuthentication(authType, false, false));
			tests.add(test);
			test = DynamicTest.dynamicTest(authType + " (TLSv1.2)", () -> testAuthentication(authType, true, false));
			tests.add(test);
			test = DynamicTest.dynamicTest(authType + " (wrong password)", () -> testAuthentication(authType, false, true));
			tests.add(test);
		}
		return tests;
	}

	private void testAuthentication(String authType, boolean encrypted, boolean wrongPassword) throws IOException, InterruptedException {

		// prepare: mailbox
		MailboxStore store = new MailboxStore();
		store.createMailbox(USERNAME, PASSWORD, EMAIL);

		// prepare: IMAP server
		try (ImapServer server = new ImapServer(store)) {
			server.setAuthenticationRequired(true);
			server.setClock(TestUtils.createTestClock());

			// disable all authentication commands by default
			server.setCommandEnabled("LOGIN", false);
			server.setCommandEnabled("AUTHENTICATE", false);

			// enable only the authentication command we want to test
			if (authType.equals("DEFAULT")) {
				server.setCommandEnabled("LOGIN", true);
				if (!encrypted) {
					server.setLoginDisabled(false); // enable LOGIN on unencrypted connections
				}
			} else {
				server.setCommandEnabled("AUTHENTICATE", true);
				server.setAuthTypes(authType);
			}

			if (encrypted) {
				server.setUseSSL(true);
				server.setSSLProtocol("TLSv1.2");
				// TODO: require encryption
			}
			server.start();

			// prepare: IMAP client
			ImapClient.ImapClientBuilder clientBuilder = ImapClient.forServer(server);
			if (encrypted) {
				clientBuilder.withEncryption("TLSv1.2");
			}
			String password = wrongPassword ? PASSWORD + "!123" : PASSWORD;
			clientBuilder.withAuthentication(authType, USERNAME, password);
			ImapClient client = clientBuilder.build();

			// test
			List<String> messages;
			try {

				messages = client.getMessages("INBOX");

			} catch (MessagingException e) {

				if (wrongPassword) {
					// assert: expected authentication error
					assertThat(e)
							.isInstanceOf(AuthenticationFailedException.class)
							.hasMessage("[AUTHENTICATIONFAILED] Authentication failed");
				} else {
					// unexpected exception
					fail("Unexpected exception: " + e, e);
				}

				return;
			}

			// assert
			assertThat(messages).isEmpty();

			List<ImapSession> sessions = server.getSessions();
			assertThat(sessions).hasSize(1);
			ImapSession session = sessions.get(0);
			session.waitUntilClosed(5000);
			if (authType.equals("DEFAULT")) {
				Assertions.assertThat(session.getAuthType()).isEqualTo("LOGIN");
			} else {
				Assertions.assertThat(session.getAuthType()).isEqualTo(authType);
			}
			Assertions.assertThat(session.getUsername()).isEqualTo(USERNAME);
			Assertions.assertThat(session.isClosed()).isTrue();

			List<ImapCommand> commands = session.getCommands();
			if (authType.equals("DEFAULT")) {
				assertThat(commands.get(0)).isInstanceOf(LOGIN.class);
			} else {
				assertThat(commands.get(0)).isInstanceOf(AUTHENTICATE.class);
			}
			Assertions.assertThat(commands).endsWith(
					new EXAMINE("INBOX"),
					new CLOSE(),
					new LOGOUT()
			);
			Assertions.assertThat(commands).hasSize(4);

			String log = session.getLog();
			if (authType.equals("DEFAULT")) {
				assertThat(log).contains("LOGIN {5}\n" +
						"+\n" +
						USERNAME + " {12}\n" +
						"+\n" +
						PASSWORD + "\n"
				);
			} else {
				assertThat(log).contains("AUTH=" + authType);
				assertThat(log).contains("AUTHENTICATE " + authType);
			}
		}
	}

	@Disabled("FETCH command is not implemented") // TODO: enable when FETCH command is implemented
	@Test
	void testGetMessages() throws IOException, MessagingException, InterruptedException {

		// prepare: mailbox
		MailboxStore store = new MailboxStore();
		Mailbox mailbox = store.createMailbox(USERNAME, PASSWORD, EMAIL);
		MailboxFolder folder = mailbox.getInbox();
		folder.addMessage("Subject: Test 1\r\n\r\nTest message 1");
		folder.addMessage("Subject: Test 2\r\n\r\nTest message 2");

		// prepare: IMAP server
		try (ImapServer server = new ImapServer(store)) {
			server.setAuthenticationRequired(true);
			server.setAuthTypes(AuthType.PLAIN);
			server.start();

			// prepare: IMAP client
			ImapClient.ImapClientBuilder clientBuilder = ImapClient.forServer(server);
			clientBuilder.withAuthentication(AuthType.PLAIN, USERNAME, PASSWORD);
			ImapClient client = clientBuilder.build();

			// test
			List<String> messages = client.getMessages("INBOX");

			// assert
			assertThat(messages).hasSize(2);
			assertThat(messages).containsExactly(
					"Subject: Test 1\r\n\r\nTest message 1\r\n",
					"Subject: Test 2\r\n\r\nTest message 2\r\n"
			);

			List<ImapSession> sessions = server.getSessions();
			assertThat(sessions).hasSize(1);
			ImapSession session = sessions.get(0);
			session.waitUntilClosed(5000);
			Assertions.assertThat(session.getAuthType()).isEqualTo("USER");
			Assertions.assertThat(session.getUsername()).isEqualTo(USERNAME);
			Assertions.assertThat(session.isClosed()).isTrue();

			List<ImapCommand> commands = session.getCommands();
			Assertions.assertThat(commands).contains(
					// TODO
			);

			// assert: messages have not been deleted
			assertThat(folder.getMessages()).hasSize(2);
		}

	}

}
