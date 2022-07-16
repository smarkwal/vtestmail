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

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.markwalder.junit.mailserver.testutils.SmtpClient;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class SmtpServerTest {

	@Test
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

	@TestFactory
	Collection<DynamicTest> testAuthenticationAndEncryption() {

		List<DynamicTest> tests = new ArrayList<>();

		// TODO: support tests with STARTTLS

		List<String> authTypes = Arrays.asList(
				null,
				AuthType.LOGIN,
				AuthType.PLAIN,
				// TODO: AuthType.CRAM_MD5,
				// TODO: AuthType.DIGEST_MD5,
				AuthType.XOAUTH2
		);
		List<String> sslProtocols = Arrays.asList(
				null,
				"SSLv3",
				"TLSv1",
				"TLSv1.1",
				"TLSv1.2",
				"TLSv1.3"
		);

		for (String authType : authTypes) {
			for (String sslProtocol : sslProtocols) {
				String name = "test(" + authType + "," + sslProtocol + ")";
				DynamicTest test = DynamicTest.dynamicTest(name, () -> testAuthenticationAndEncryption(authType, sslProtocol));
				tests.add(test);
			}
		}

		return tests;
	}

	void testAuthenticationAndEncryption(String authType, String sslProtocol) throws IOException, MessagingException {

		// prepare: mailbox
		MailboxStore store = new MailboxStore();
		store.createMailbox("alice", "password123", "alice@localhost");

		// prepare: SMTP server
		try (SmtpServer server = new SmtpServer(store)) {
			if (authType != null) {
				server.setAuthenticationRequired(true);
				server.setAuthTypes(authType);
			}
			if (sslProtocol != null) {
				server.setUseSSL(true);
				server.setSSLProtocol(sslProtocol);
				// TODO: require encryption
			}
			server.start();

			// prepare: SMTP client
			SmtpClient.SmtpClientBuilder clientBuilder = SmtpClient.forServer(server);
			if (authType != null) {
				clientBuilder.withAuthentication(authType, "alice", "password123");
			}
			if (sslProtocol != null) {
				clientBuilder.withEncryption(sslProtocol);
			}
			SmtpClient client = clientBuilder.build();

			// prepare: email
			Message message = client.prepareMessage()
					.messageId("1234567890@localhost")
					.date(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
					.from("bob@localhost")
					.to("alice@localhost")
					.subject("Test email")
					.body("This is a test email.")
					.build();

			// test
			client.send(message);

			// assert
			Mailbox mailbox = store.getMailbox("alice");
			List<Mailbox.Message> messages = mailbox.getMessages();
			assertThat(messages).hasSize(1);
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

}
