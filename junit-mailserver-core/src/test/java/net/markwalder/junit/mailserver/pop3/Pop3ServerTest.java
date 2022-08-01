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

package net.markwalder.junit.mailserver.pop3;

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
import net.markwalder.junit.mailserver.AuthType;
import net.markwalder.junit.mailserver.Mailbox;
import net.markwalder.junit.mailserver.MailboxStore;
import net.markwalder.junit.mailserver.testutils.Pop3Client;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

public class Pop3ServerTest {

	private static final String USERNAME = "\u00E4li\u00E7\u00E9";
	private static final String PASSWORD = "p\u00E4ssw\u00F6rd!123";
	private static final String EMAIL = "alice@localhost";

	@Test
	void getPort() throws IOException {

		// prepare: POP3 server
		MailboxStore store = new MailboxStore();
		try (Pop3Server server = new Pop3Server(store)) {

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

		// prepare: POP3 server
		MailboxStore store = new MailboxStore();
		try (Pop3Server server = new Pop3Server(store)) {

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

	private void testEncryption(String sslProtocol, boolean useStarTLS) throws IOException, MessagingException {

		// TODO: support tests with STARTTLS
		assumeFalse(useStarTLS, "STARTTLS not implemented");

		// prepare: mailbox
		MailboxStore store = new MailboxStore();
		store.createMailbox(USERNAME, PASSWORD, EMAIL);

		// prepare: POP3 server
		try (Pop3Server server = new Pop3Server(store)) {
			server.setAuthTypes(AuthType.PLAIN);
			server.setUseSSL(true);
			server.setSSLProtocol(sslProtocol);
			// TODO: require encryption
			server.start();

			// prepare: POP3 client
			Pop3Client client = Pop3Client.forServer(server)
					.withAuthentication(AuthType.PLAIN, USERNAME, PASSWORD)
					.withEncryption(sslProtocol)
					.build();

			// test
			List<String> messages = client.getMessages();

			// assert
			assertThat(messages).isEmpty();

			List<Pop3Session> sessions = server.getSessions();
			assertThat(sessions).hasSize(1);
			Pop3Session session = sessions.get(0);
			assertThat(session.getSSLProtocol()).isEqualTo(sslProtocol);
			assertThat(session.getCipherSuite()).isNotEmpty();
			assertThat(session.isClosed()).isTrue();

			List<Pop3Command> commands = session.getCommands();
			assertThat(commands).containsExactly(
					new CAPA(),
					new AUTH("PLAIN", "w6RsacOnw6kAw6RsacOnw6kAcMOkc3N3w7ZyZCExMjM="),
					new STAT(),
					new QUIT()
			);
		}
	}


	@TestFactory
	@DisplayName("Authentication")
	Collection<DynamicTest> testAuthentication() {

		// note: CRAM-MD5 and DIGEST-MD5 are not supported by Jakarta Mail API 2.0.1
		// note: LOGIN is implemented by Jakarta Mail using USER/PASS or APOP instead
		List<String> authTypes = Arrays.asList(
				"USER",
				"APOP",
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

	private void testAuthentication(String authType, boolean encrypted, boolean wrongPassword) throws IOException {

		// prepare: mailbox
		MailboxStore store = new MailboxStore();
		store.createMailbox(USERNAME, PASSWORD, EMAIL);

		// prepare: POP3 server
		try (Pop3Server server = new Pop3Server(store)) {
			server.setAuthenticationRequired(true);

			// disable all authentication commands by default
			server.setCommandEnabled("USER", false);
			server.setCommandEnabled("PASS", false);
			server.setCommandEnabled("APOP", false);
			server.setCommandEnabled("AUTH", false);

			// enable only the authentication command we want to test
			if (authType.equals("APOP")) {
				server.setCommandEnabled("APOP", true);
			} else if (authType.equals("USER")) {
				server.setCommandEnabled("USER", true);
				server.setCommandEnabled("PASS", true);
			} else {
				server.setCommandEnabled("AUTH", true);
				server.setAuthTypes(authType);
			}

			if (encrypted) {
				server.setUseSSL(true);
				server.setSSLProtocol("TLSv1.2");
				// TODO: require encryption
			}
			server.start();

			// prepare: POP3 client
			Pop3Client.Pop3ClientBuilder clientBuilder = Pop3Client.forServer(server);
			if (encrypted) {
				clientBuilder.withEncryption("TLSv1.2");
			}
			String password = wrongPassword ? PASSWORD + "!123" : PASSWORD;
			clientBuilder.withAuthentication(authType, USERNAME, password);
			Pop3Client client = clientBuilder.build();

			// test
			List<String> messages;
			try {

				messages = client.getMessages();

			} catch (MessagingException e) {

				if (wrongPassword) {
					// assert: expected authentication error
					assertThat(e)
							.isInstanceOf(AuthenticationFailedException.class)
							.hasMessage("Authentication failed");
				} else {
					// unexpected exception
					fail("Unexpected exception: " + e, e);
				}

				return;
			}

			// assert
			assertThat(messages).isEmpty();

			String log = server.getLog();
			if (authType.equals("APOP")) {
				assertThat(log).contains("APOP " + USERNAME + " ");
			} else if (authType.equals("USER")) {
				assertThat(log).contains("USER " + USERNAME, "PASS " + PASSWORD);
			} else {
				assertThat(log).contains("SASL " + authType);
				assertThat(log).contains("AUTH " + authType + " ");
			}

			List<Pop3Session> sessions = server.getSessions();
			assertThat(sessions).hasSize(1);
			Pop3Session session = sessions.get(0);
			assertThat(session.getAuthType()).isEqualTo(authType);
			assertThat(session.getUsername()).isEqualTo(USERNAME);
			assertThat(session.isClosed()).isTrue();

			List<Pop3Command> commands = session.getCommands();
			assertThat(commands).contains(
					new CAPA(),
					new STAT(),
					new QUIT()
			);

			if (authType.equals("APOP")) {
				assertThat(commands.get(1)).isInstanceOf(APOP.class);
			} else if (authType.equals("USER")) {
				assertThat(commands.get(1)).isEqualTo(new USER(USERNAME));
				assertThat(commands.get(2)).isEqualTo(new PASS(PASSWORD));
			} else {
				assertThat(commands.get(1)).isInstanceOf(AUTH.class);
			}

			assertThat(commands).hasSize(authType.equals("USER") ? 5 : 4);
		}
	}

	@Test
	void testGetMessages() throws IOException, MessagingException {

		// prepare: mailbox
		MailboxStore store = new MailboxStore();
		Mailbox mailbox = store.createMailbox(USERNAME, PASSWORD, EMAIL);
		mailbox.addMessage("Subject: Test 1\r\n\r\nTest message 1");
		mailbox.addMessage("Subject: Test 2\r\n\r\nTest message 2");

		// prepare: POP3 server
		try (Pop3Server server = new Pop3Server(store)) {
			server.setAuthenticationRequired(true);
			server.start();

			// prepare: POP3 client
			Pop3Client.Pop3ClientBuilder clientBuilder = Pop3Client.forServer(server);
			clientBuilder.withAuthentication("USER", USERNAME, PASSWORD);
			Pop3Client client = clientBuilder.build();

			// test
			List<String> messages = client.getMessages();

			// assert
			assertThat(messages).hasSize(2);
			assertThat(messages).containsExactly(
					"Subject: Test 1\r\n\r\nTest message 1\r\n", // TODO: why is there a CRLF at the end?
					"Subject: Test 2\r\n\r\nTest message 2\r\n" // TODO: why is there a CRLF at the end?
			);

			List<Pop3Session> sessions = server.getSessions();
			assertThat(sessions).hasSize(1);
			Pop3Session session = sessions.get(0);
			assertThat(session.getAuthType()).isEqualTo("USER");
			assertThat(session.getUsername()).isEqualTo(USERNAME);
			assertThat(session.isClosed()).isTrue();

			List<Pop3Command> commands = session.getCommands();
			assertThat(commands).contains(
					new CAPA(),
					new USER(USERNAME),
					new PASS(PASSWORD),
					new STAT(),
					new TOP(1, 0),
					new RETR(1),
					new TOP(2, 0),
					new RETR(2),
					new QUIT()
			);

			// assert: messages have not been deleted
			assertThat(mailbox.getMessages()).hasSize(2);
		}

	}

	// TODO: implement POP3 test where messages are deleted from mailbox

}
