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

package net.markwalder.vtestmail.testutils;

import com.sun.mail.imap.IMAPMessage;
import jakarta.mail.Authenticator;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import net.markwalder.vtestmail.imap.ImapServer;
import net.markwalder.vtestmail.utils.Assert;

public class ImapClient {

	private final Session session;

	private ImapClient(Session session) {
		this.session = session;
	}

	public static ImapClientBuilder forServer(ImapServer server) {
		return new ImapClientBuilder(server);
	}

	public List<String> getMessages(String folderName) throws MessagingException, IOException {

		List<String> messages = new ArrayList<>();

		Store store = session.getStore("imap");
		store.connect();
		Folder inbox = store.getFolder(folderName);
		inbox.open(Folder.READ_ONLY);

		int count = inbox.getMessageCount();
		for (int num = 1; num <= count; num++) {
			IMAPMessage message = (IMAPMessage) inbox.getMessage(num);

			StringBuilder text = new StringBuilder();
			Enumeration<String> headers = message.getAllHeaderLines();
			while (headers.hasMoreElements()) {
				text.append(headers.nextElement());
				text.append("\r\n");
			}
			text.append("\r\n");
			text.append(message.getContent());
			messages.add(text.toString());
		}
		inbox.close(true);
		store.close();

		return messages;
	}

	// TODO: add methods to connect and disconnect
	// TODO: add method to get a single message
	// TODO: add method to delete a message

	public static class ImapClientBuilder {

		private final Properties properties = new Properties();
		private Authenticator authenticator = null;

		private ImapClientBuilder(ImapServer server) {
			Assert.isNotNull(server, "server");

			int port = server.getPort();

			// IMAP properties:
			// https://eclipse-ee4j.github.io/mail/docs/api/jakarta.mail/com/sun/mail/imap/package-summary.html

			// IMAP server address (host name)
			properties.put("mail.imap.host", "localhost");

			// IMAP server port
			properties.put("mail.imap.port", String.valueOf(port));

			// IMAP client address (host name)
			properties.put("mail.imap.localaddress", "localhost");

			// disable all authentication mechanisms by default
			properties.put("mail.imap.auth.mechanisms", "");

			// disable encryption by default
			properties.put("mail.imap.ssl.enable", "false");
			properties.put("mail.imap.starttls.enable", "false");
			properties.put("mail.imap.ssl.trust", "*");

			// socket timeouts
			properties.put("mail.imap.connectiontimeout", "5000"); // connect
			properties.put("mail.imap.timeout", "5000"); // read
			properties.put("mail.imap.writetimeout", "5000"); // write

		}

		public ImapClientBuilder withAuthentication(String authType, String username, String password) {
			Assert.isNotEmpty(authType, "authType");
			Assert.isNotEmpty(username, "username");
			Assert.isNotEmpty(password, "password");

			// Jakarta Mail API special behavior:
			// LOGIN is not supported.
			// instead, if the client is configured to use LOGIN,
			// the client will use either the USER/PASS or APOP commands.

			if (authType.equalsIgnoreCase("DEFAULT")) {
				properties.put("mail.imap.auth.mechanisms", "");
			} else {

				// If set, lists the authentication mechanisms to consider, and
				// the order in which to consider them. Only mechanisms
				// supported by the server and supported by the current
				// implementation will be used. The default is "PLAIN LOGIN
				// NTLM", which includes all the authentication mechanisms
				// supported by the current implementation except XOAUTH2.
				properties.put("mail.imap.auth.mechanisms", authType);

			}

			// create authenticator
			authenticator = new PasswordAuthenticator(username, password);

			return this;
		}

		public ImapClientBuilder withEncryption(String protocols) {
			Assert.isNotEmpty(protocols, "protocols");

			// use SSL to connect to IMAP server
			properties.put("mail.imap.ssl.enable", "true");

			setProtocols(protocols);

			return this;
		}

		public ImapClientBuilder withStartTLS(String protocols) {

			// enable the use of the STARTTLS command
			properties.put("mail.imap.starttls.enable", "true");

			// require the use of the STARTTLS command
			properties.put("mail.imap.starttls.required", "true");

			setProtocols(protocols);

			return this;
		}

		private void setProtocols(String protocols) {

			// SSL protocols (whitespace separated list)
			properties.put("mail.imap.ssl.protocols", protocols);

			// SSL cipher suites (whitespace separated list)
			// TODO: support this?
			// properties.put("mail.smtp.ssl.ciphersuites", "");
		}

		public ImapClient build() {
			Session session = Session.getInstance(properties, authenticator);
			return new ImapClient(session);
		}

	}

}
