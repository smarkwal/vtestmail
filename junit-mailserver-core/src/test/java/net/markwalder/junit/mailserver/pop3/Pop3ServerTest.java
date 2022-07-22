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
import net.markwalder.junit.mailserver.MailboxStore;
import net.markwalder.junit.mailserver.testutils.Pop3Client;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

public class Pop3ServerTest {

	private static final String USERNAME = "alice";
	private static final String PASSWORD = "password123";
	private static final String EMAIL = "alice@localhost";

	@Test
	@DisplayName("Port")
	void getPort() throws IOException {

		// prepare: POP3 server
		MailboxStore store = new MailboxStore();
		try (Pop3Server server = new Pop3Server(store)) {
			server.start();

			// test
			int port = server.getPort();

			// assert
			assertThat(port).isBetween(1024, 65535);
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
		// TODO: add messages to mailbox

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
			client.getMessages(); // TODO: get messages

			// assert
			// TODO: implement test on messages

		}
	}


	@TestFactory
	@DisplayName("Authentication")
	Collection<DynamicTest> testAuthentication() {

		// note: CRAM-MD5 and DIGEST-MD5 are not supported by Jakarta Mail API 2.0.1
		List<String> authTypes = Arrays.asList(
				// TODO: test with USER/PASS
				// TODO: test with APOP
				AuthType.LOGIN, // TODO: why does this use USER/PASS instead of AUTH LOGIN?
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

	private void testAuthentication(String authType, boolean encrypted, boolean wrongPassword) throws IOException, MessagingException {

		// prepare: mailbox
		MailboxStore store = new MailboxStore();
		store.createMailbox(USERNAME, PASSWORD, EMAIL);

		// prepare: SMTP server
		try (Pop3Server server = new Pop3Server(store)) {
			server.setAuthenticationRequired(true);
			server.setAuthTypes(authType);
			if (encrypted) {
				server.setUseSSL(true);
				server.setSSLProtocol("TLSv1.2");
				// TODO: require encryption
			}
			server.start();

			// prepare: SMTP client
			Pop3Client.Pop3ClientBuilder clientBuilder = Pop3Client.forServer(server);
			if (encrypted) {
				clientBuilder.withEncryption("TLSv1.2");
			}
			String password = wrongPassword ? PASSWORD + "!123" : PASSWORD;
			clientBuilder.withAuthentication(authType, USERNAME, password);
			Pop3Client client = clientBuilder.build();

			// test
			try {

				client.getMessages();

			} catch (MessagingException e) {

				if (wrongPassword) {
					// assert: expected authentication error
					assertThat(e)
							.isInstanceOf(AuthenticationFailedException.class)
							.hasMessage("Authentication failed");
				} else {
					// unexpected exception
					fail(e);
				}

				return;
			}

			// assert
			// TODO: implement test on messages

		}
	}


}