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
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.markwalder.junit.mailserver.AuthType;
import net.markwalder.junit.mailserver.Mailbox;
import net.markwalder.junit.mailserver.MailboxStore;
import net.markwalder.junit.mailserver.testutils.SmtpClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class SmtpServerTest {

	private static final String FROM = "bob@localhost";
	private static final String TO = "alice@localhost";
	private static final String USERNAME = "alice";
	private static final String PASSWORD = "password123";

	@Test
	@DisplayName("Port")
	void getPort() throws IOException {

		// prepare: SMTP server
		MailboxStore store = new MailboxStore();
		try (SmtpServer server = new SmtpServer(store)) {
			server.start();

			// test
			int port = server.getPort();

			// assert
			assertThat(port).isBetween(1024, 65535);
		}
	}

	@Test
	@DisplayName("Data")
	void testData() throws IOException, MessagingException {

		// prepare: mailbox
		MailboxStore store = new MailboxStore();
		store.createMailbox(USERNAME, PASSWORD, TO);

		// prepare: SMTP server
		try (SmtpServer server = new SmtpServer(store)) {
			server.start();

			// prepare: SMTP client
			SmtpClient client = SmtpClient.forServer(server).build();

			// prepare: email
			Message message = createTestMessage(client);

			// test
			client.send(message);

			// assert
			Mailbox mailbox = store.getMailbox(USERNAME);
			List<Mailbox.Message> messages = mailbox.getMessages();
			assertThat(messages).hasSize(1);

			Mailbox.Message mail = messages.get(0);
			String content = mail.getContent();
			assertThat(content).isEqualTo(
					"Date: Wed, 1 Jan 2020 00:00:00 +0000\r\n" +
							"From: bob@localhost\r\n" +
							"To: alice@localhost\r\n" +
							"Message-ID: <1234567890@localhost>\r\n" +
							"Subject: Test email\r\n" +
							"MIME-Version: 1.0\r\n" +
							"Content-Type: text/plain; charset=utf-8\r\n" +
							"Content-Transfer-Encoding: 7bit\r\n" +
							"\r\n" +
							"This is a test email."
			);

			// TODO: add more assertions

			// TODO: server.getSessions();
			// TODO: session.getLog();
			// TODO: session.getClientAddress();
			// TODO: session.getClientPort();
			// TODO: session.getSSLProtocol();
			// TODO: session.getCipherSuite();
			// TODO: session.getAuthType();
			// TODO: session.getUsername();
			// TODO: session.getMessages();
			// TODO: message.getMailFrom();
			// TODO: message.getRecipients();
			// TODO: message.getData();

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
		store.createMailbox(USERNAME, PASSWORD, TO);

		// prepare: SMTP server
		try (SmtpServer server = new SmtpServer(store)) {
			server.setUseSSL(true);
			server.setSSLProtocol(sslProtocol);
			// TODO: require encryption
			server.start();

			// prepare: SMTP client
			SmtpClient client = SmtpClient.forServer(server)
					.withEncryption(sslProtocol)
					.build();

			// prepare: email
			Message message = createTestMessage(client);

			// test
			client.send(message);

			// assert
			Mailbox mailbox = store.getMailbox(USERNAME);
			List<Mailbox.Message> messages = mailbox.getMessages();
			assertThat(messages).hasSize(1);
		}
	}

	@TestFactory
	@DisplayName("Authentication")
	Collection<DynamicTest> testAuthentication() {

		// note: CRAM-MD5 is not supported by JavaMail
		List<String> authTypes = Arrays.asList(
				AuthType.LOGIN,
				AuthType.PLAIN,
				AuthType.DIGEST_MD5,
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

	private void testAuthentication(String authType, boolean encrypted, boolean wrongPassword) throws IOException, MessagingException {

		// prepare: mailbox
		MailboxStore store = new MailboxStore();
		store.createMailbox(USERNAME, PASSWORD, TO);

		// prepare: SMTP server
		try (SmtpServer server = new SmtpServer(store)) {
			server.setAuthenticationRequired(true);
			server.setAuthTypes(authType);
			if (encrypted) {
				server.setUseSSL(true);
				server.setSSLProtocol("TLSv1.2");
				// TODO: require encryption
			}
			server.start();

			// prepare: SMTP client
			SmtpClient.SmtpClientBuilder clientBuilder = SmtpClient.forServer(server);
			if (encrypted) {
				clientBuilder.withEncryption("TLSv1.2");
			}
			String password = wrongPassword ? PASSWORD + "!123" : PASSWORD;
			clientBuilder.withAuthentication(authType, USERNAME, password);
			SmtpClient client = clientBuilder.build();

			// prepare: email
			Message message = createTestMessage(client);

			// test
			try {

				client.send(message);

			} catch (MessagingException e) {

				if (wrongPassword) {
					// assert: expected authentication error
					assertThat(e)
							.isInstanceOf(AuthenticationFailedException.class)
							.hasMessageContaining("535 5.7.8 Authentication failed");
				} else {
					// unexpected exception
					fail(e);
				}

				return;
			}

			// assert
			Mailbox mailbox = store.getMailbox(USERNAME);
			List<Mailbox.Message> messages = mailbox.getMessages();
			assertThat(messages).hasSize(1);
		}
	}

	private Message createTestMessage(SmtpClient client) throws MessagingException {
		return client.prepareMessage()
				.messageId("1234567890@localhost")
				.date(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
				.from(FROM)
				.to(TO)
				.subject("Test email")
				.body("This is a test email.")
				.build();
	}

}
