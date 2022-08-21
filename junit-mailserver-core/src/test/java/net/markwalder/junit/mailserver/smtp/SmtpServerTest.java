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
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import com.sun.mail.smtp.SMTPSendFailedException;
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
import net.markwalder.junit.mailserver.auth.AuthType;
import net.markwalder.junit.mailserver.store.Mailbox;
import net.markwalder.junit.mailserver.store.MailboxFolder;
import net.markwalder.junit.mailserver.store.MailboxMessage;
import net.markwalder.junit.mailserver.store.MailboxStore;
import net.markwalder.junit.mailserver.testutils.JavaUtils;
import net.markwalder.junit.mailserver.testutils.SmtpClient;
import net.markwalder.junit.mailserver.testutils.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class SmtpServerTest {

	private static final String FROM = "bob@localhost";
	private static final String TO = "alice@localhost";
	private static final String USERNAME = "\u00E4li\u00E7\u00E9";
	private static final String PASSWORD = "p\u00E4ssw\u00F6rd!123";

	@Test
	void getPort() throws IOException {

		// prepare: SMTP server
		MailboxStore store = new MailboxStore();
		try (SmtpServer server = new SmtpServer(store)) {

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
		try (SmtpServer server = new SmtpServer(store)) {

			// test
			server.setPort(24277);
			server.start();

			// assert
			int port = server.getPort();
			assertThat(port).isEqualTo(24277);
		}
	}

	@Test
	@DisplayName("Data")
	void testData() throws IOException, MessagingException, InterruptedException {

		// prepare: mailbox
		MailboxStore store = new MailboxStore();
		store.createMailbox(USERNAME, PASSWORD, TO);

		// prepare: SMTP server
		try (SmtpServer server = new SmtpServer(store)) {
			server.setClock(TestUtils.createTestClock());
			server.start();

			// prepare: SMTP client
			SmtpClient client = SmtpClient.forServer(server).withEHLO(false).build();

			// prepare: email
			Message message = createTestMessage(client);

			// test
			client.send(message);

			// assert
			List<SmtpSession> sessions = server.getSessions();
			assertThat(sessions).hasSize(1);
			SmtpSession session = sessions.get(0);
			session.waitUntilClosed(5000);
			assertThat(session.getServerAddress()).isEqualTo("127.0.0.1");
			assertThat(session.getServerPort()).isEqualTo(server.getPort());
			assertThat(session.getClientAddress()).isEqualTo("127.0.0.1");
			assertThat(session.getClientPort()).isBetween(1024, 65535);
			assertThat(session.getSSLProtocol()).isNull();
			assertThat(session.getCipherSuite()).isNull();
			assertThat(session.isEncrypted()).isFalse();
			assertThat(session.getAuthType()).isNull();
			assertThat(session.getUsername()).isNull();
			assertThat(session.isClosed()).isTrue();

			Mailbox mailbox = store.getMailbox(USERNAME);
			MailboxFolder folder = mailbox.getInbox();
			List<MailboxMessage> messages = folder.getMessages();
			assertThat(messages).hasSize(1);

			MailboxMessage mail = messages.get(0);
			String content = mail.getContent();
			assertThat(content).isEqualTo(
					"Received: from 127.0.0.1 by 127.0.0.1; Wed, 1 Jan 2020 00:00:00 +0000\r\n" +
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

			List<SmtpCommand> commands = session.getCommands();
			assertThat(commands).containsExactly(
					new HELO("localhost"),
					new QUIT()
			);

			List<SmtpTransaction> transactions = session.getTransactions();
			assertThat(transactions).hasSize(1);
			SmtpTransaction transaction = transactions.get(0);
			assertThat(transaction.getSender()).isEqualTo(FROM);
			assertThat(transaction.getRecipients()).containsExactly(TO);
			assertThat(transaction.getData()).isEqualTo(content);

			commands = transaction.getCommands();
			assertThat(commands).containsExactly(
					new MAIL("bob@localhost"),
					new RCPT("alice@localhost"),
					new DATA()
			);

			String log = session.getLog();
			assertThat(log).contains("220 localhost Service ready\n" +
					"HELO localhost\n" +
					"250 OK\n" +
					"MAIL FROM:<bob@localhost>\n" +
					"250 2.1.0 OK\n" +
					"RCPT TO:<alice@localhost>\n" +
					"250 2.1.5 OK\n" +
					"DATA\n" +
					"354 Send message, end with <CRLF>.<CRLF>\n" +
					"Date: Wed, 1 Jan 2020 00:00:00 +0000\n" +
					"From: bob@localhost\n" +
					"To: alice@localhost\n" +
					"Message-ID: <1234567890@localhost>\n" +
					"Subject: Test email\n" +
					"MIME-Version: 1.0\n" +
					"Content-Type: text/plain; charset=utf-8\n" +
					"Content-Transfer-Encoding: 7bit\n" +
					"\n" +
					"This is a test email.\n" +
					".\n" +
					"250 2.6.0 Message accepted\n" +
					"QUIT\n" +
					"221 2.0.0 Goodbye\n");
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
		store.createMailbox(USERNAME, PASSWORD, TO);

		// prepare: SMTP server
		try (SmtpServer server = new SmtpServer(store)) {
			server.setUseSSL(!useStartTLS);
			server.setSSLProtocol(sslProtocol);
			server.setCommandEnabled("STARTTLS", useStartTLS);
			// TODO: require encryption
			server.start();

			// prepare: SMTP client
			SmtpClient client;
			if (useStartTLS) {
				client = SmtpClient.forServer(server)
						.withStartTLS(sslProtocol)
						.build();
			} else {
				client = SmtpClient.forServer(server)
						.withEncryption(sslProtocol)
						.build();
			}

			// prepare: email
			Message message = createTestMessage(client);

			// test
			client.send(message);

			// assert
			List<SmtpSession> sessions = server.getSessions();
			assertThat(sessions).hasSize(1);
			SmtpSession session = sessions.get(0);
			session.waitUntilClosed(5000);
			assertThat(session.getSSLProtocol()).isEqualTo(sslProtocol);
			assertThat(session.getCipherSuite()).isNotEmpty();
			assertThat(session.isEncrypted()).isTrue();
			assertThat(session.isClosed()).isTrue();

			Mailbox mailbox = store.getMailbox(USERNAME);
			MailboxFolder folder = mailbox.getInbox();
			List<MailboxMessage> messages = folder.getMessages();
			assertThat(messages).hasSize(1);

			String log = session.getLog();
			if (useStartTLS) {
				assertThat(log).startsWith("220 localhost Service ready\n" +
						"EHLO localhost\n" +
						"250-127.0.0.1 Hello localhost\n" +
						"250-STARTTLS\n" +
						"250-VRFY\n" +
						"250-ENHANCEDSTATUSCODES\n" +
						"250 OK\n" +
						"STARTTLS\n" +
						"220 Ready to start TLS\n" +
						"EHLO localhost\n" +
						"250-127.0.0.1 Hello localhost\n" +
						"250-VRFY\n" +
						"250-ENHANCEDSTATUSCODES\n" +
						"250 OK\n");
			} else {
				assertThat(log).startsWith("220 localhost Service ready\n" +
						"EHLO localhost\n" +
						"250-127.0.0.1 Hello localhost\n" +
						"250-VRFY\n" +
						"250-ENHANCEDSTATUSCODES\n" +
						"250 OK\n");
			}
			assertThat(log).endsWith("MAIL FROM:<bob@localhost>\n" +
					"250 2.1.0 OK\n" +
					"RCPT TO:<alice@localhost>\n" +
					"250 2.1.5 OK\n" +
					"DATA\n" +
					"354 Send message, end with <CRLF>.<CRLF>\n" +
					"Date: Wed, 1 Jan 2020 00:00:00 +0000\n" +
					"From: bob@localhost\n" +
					"To: alice@localhost\n" +
					"Message-ID: <1234567890@localhost>\n" +
					"Subject: Test email\n" +
					"MIME-Version: 1.0\n" +
					"Content-Type: text/plain; charset=utf-8\n" +
					"Content-Transfer-Encoding: 7bit\n" +
					"\n" +
					"This is a test email.\n" +
					".\n" +
					"250 2.6.0 Message accepted\n" +
					"QUIT\n" +
					"221 2.0.0 Goodbye\n");
		}
	}

	@TestFactory
	@DisplayName("Authentication")
	Collection<DynamicTest> testAuthentication() {

		// note: CRAM-MD5 is not supported by Jakarta Mail API 2.0.1
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

	private void testAuthentication(String authType, boolean encrypted, boolean wrongPassword) throws IOException, MessagingException, InterruptedException {

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
			List<SmtpSession> sessions = server.getSessions();
			assertThat(sessions).hasSize(1);
			SmtpSession session = sessions.get(0);
			session.waitUntilClosed(5000);
			assertThat(session.getAuthType()).isEqualTo(authType);
			assertThat(session.getUsername()).isEqualTo(USERNAME);
			assertThat(session.isClosed()).isTrue();

			Mailbox mailbox = store.getMailbox(USERNAME);
			MailboxFolder folder = mailbox.getInbox();
			List<MailboxMessage> messages = folder.getMessages();
			assertThat(messages).hasSize(1);
		}
	}

	@Test
	@DisplayName("Unknown authentication type")
	void testUnknownAuthenticationType() throws IOException, MessagingException {

		// prepare: SMTP server
		try (SmtpServer server = new SmtpServer(new MailboxStore())) {
			server.setAuthenticationRequired(true);
			server.setAuthTypes(AuthType.PLAIN);
			server.start();

			// prepare: SMTP client
			SmtpClient.SmtpClientBuilder clientBuilder = SmtpClient.forServer(server);
			clientBuilder.withAuthentication(AuthType.XOAUTH2, USERNAME, PASSWORD);
			SmtpClient client = clientBuilder.build();

			// prepare: email
			Message message = createTestMessage(client);

			// test
			Exception exception = assertThrows(AuthenticationFailedException.class, () -> client.send(message));

			// assert
			assertThat(exception).hasMessage("No authentication mechanisms supported by both server and client");
		}
	}

	@Test
	@DisplayName("Authentication required")
	void testAuthenticationRequired() throws IOException, MessagingException {

		// prepare: SMTP server
		try (SmtpServer server = new SmtpServer(new MailboxStore())) {
			server.setAuthenticationRequired(true);
			server.setAuthTypes(AuthType.PLAIN);
			server.start();

			// prepare: SMTP client
			SmtpClient.SmtpClientBuilder clientBuilder = SmtpClient.forServer(server);
			SmtpClient client = clientBuilder.build();

			// prepare: email
			Message message = createTestMessage(client);

			// test and assert
			Exception exception = assertThrows(SMTPSendFailedException.class, () -> client.send(message));

			// assert
			assertThat(exception).hasMessageContaining("530 5.7.0 Authentication required");
		}
	}

	@Test
	@DisplayName("TO, CC, and BCC recipients")
	void testRecipients() throws IOException, MessagingException, InterruptedException {

		// prepare: mailbox
		MailboxStore store = new MailboxStore();
		store.createMailbox(USERNAME, PASSWORD, TO);

		// prepare: SMTP server
		try (SmtpServer server = new SmtpServer(store)) {
			server.start();

			// prepare: SMTP client
			SmtpClient.SmtpClientBuilder clientBuilder = SmtpClient.forServer(server);
			SmtpClient client = clientBuilder.build();

			// prepare: email
			Message message = client.prepareMessage()
					.from(FROM)
					.to(TO)
					.cc("chris@localhost")
					.bcc("dan@localhost")
					.subject("Test email")
					.body("This is a test email.")
					.build();

			// test
			client.send(message);

			// assert
			List<SmtpSession> sessions = server.getSessions();
			assertThat(sessions).hasSize(1);
			SmtpSession session = sessions.get(0);
			session.waitUntilClosed(5000);

			String log = session.getLog();
			assertThat(log).contains(
					"MAIL FROM:<bob@localhost>",
					"RCPT TO:<alice@localhost>",
					"RCPT TO:<chris@localhost>",
					"RCPT TO:<dan@localhost>",
					"From: bob@localhost",
					"To: alice@localhost",
					"Cc: chris@localhost"
			);

			// assert: email has been delivered to mailbox
			Mailbox mailbox = store.getMailbox(USERNAME);
			MailboxFolder folder = mailbox.getInbox();
			List<MailboxMessage> messages = folder.getMessages();
			assertThat(messages).hasSize(1);

			// assert: BCC recipient is not included in message
			MailboxMessage email = messages.get(0);
			String content = email.getContent();
			assertThat(content).doesNotContain("dan@localhost");

			List<SmtpTransaction> transactions = session.getTransactions();
			assertThat(transactions).hasSize(1);
			SmtpTransaction transaction = transactions.get(0);
			assertThat(transaction.getSender()).isEqualTo("bob@localhost");
			assertThat(transaction.getRecipients()).containsExactly("alice@localhost", "chris@localhost", "dan@localhost");
			assertThat(transaction.getData()).isEqualTo(content);
		}

	}

	// TODO: add test for unknown command
	// TODO: add test for disabled command

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
