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

import com.sun.mail.pop3.POP3Message;
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
import net.markwalder.vtestmail.auth.AuthType;
import net.markwalder.vtestmail.pop3.Pop3Server;
import net.markwalder.vtestmail.utils.Assert;

public class Pop3Client {

	private final Session session;

	private Pop3Client(Session session) {
		this.session = session;
	}

	public static Pop3ClientBuilder forServer(Pop3Server server) {
		return new Pop3ClientBuilder(server);
	}

	public List<String> getMessages() throws MessagingException, IOException {

		List<String> messages = new ArrayList<>();

		Store store = session.getStore("pop3");
		store.connect();
		Folder inbox = store.getFolder("Inbox");
		inbox.open(Folder.READ_ONLY);

		int count = inbox.getMessageCount();
		for (int num = 1; num <= count; num++) {
			POP3Message message = (POP3Message) inbox.getMessage(num);

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

	public static class Pop3ClientBuilder {

		private final Properties properties = new Properties();
		private Authenticator authenticator = null;

		private Pop3ClientBuilder(Pop3Server server) {
			Assert.isNotNull(server, "server");

			int port = server.getPort();

			// POP3 properties:
			// https://eclipse-ee4j.github.io/mail/docs/api/jakarta.mail/com/sun/mail/pop3/package-summary.html

			// POP3 server address (host name)
			properties.put("mail.pop3.host", "localhost");

			// POP3 server port
			properties.put("mail.pop3.port", String.valueOf(port));

			// POP3 client address (host name)
			properties.put("mail.pop3.localaddress", "localhost");

			// disable all authentication mechanisms by default
			properties.put("mail.pop3.auth.mechanisms", "");
			properties.put("mail.pop3.apop.enable", "false");

			// disable encryption by default
			properties.put("mail.pop3.ssl.enable", "false");
			properties.put("mail.pop3.starttls.enable", "false");
			properties.put("mail.pop3.ssl.trust", "*");

			// socket timeouts
			properties.put("mail.pop3.connectiontimeout", "5000"); // connect
			properties.put("mail.pop3.timeout", "5000"); // read
			properties.put("mail.pop3.writetimeout", "5000"); // write

			// TODO: test with mail.pop3.disabletop	= true
			// TODO: test with mail.pop3.disablecapa = true
			// TODO: test with mail.pop3.rsetbeforequit	= true

		}

		public Pop3ClientBuilder withAuthentication(String authType, String username, String password) {
			Assert.isNotEmpty(authType, "authType");
			Assert.isNotEmpty(username, "username");
			Assert.isNotEmpty(password, "password");

			// Jakarta Mail API special behavior:
			// LOGIN is not supported.
			// instead, if the client is configured to use LOGIN,
			// the client will use either the USER/PASS or APOP commands.

			if (authType.equalsIgnoreCase("USER")) {

				properties.put("mail.pop3.auth.mechanisms", AuthType.LOGIN);

			} else if (authType.equalsIgnoreCase("APOP")) {

				properties.put("mail.pop3.apop.enable", "true");
				properties.put("mail.pop3.auth.mechanisms", AuthType.LOGIN);

			} else {

				// If set, lists the authentication mechanisms to consider, and the
				// order in which to consider them. Only mechanisms supported by
				// the server and supported by the current implementation will be
				// used. The default is "LOGIN PLAIN DIGEST-MD5 NTLM", which
				// includes all the authentication mechanisms supported by the
				// current implementation except XOAUTH2.
				properties.put("mail.pop3.auth.mechanisms", authType);

			}

			// create authenticator
			authenticator = new PasswordAuthenticator(username, password);

			return this;
		}

		public Pop3ClientBuilder withEncryption(String protocols) {
			Assert.isNotEmpty(protocols, "protocols");

			// use SSL to connect to POP3 server
			properties.put("mail.pop3.ssl.enable", "true");

			setProtocols(protocols);

			return this;
		}

		public Pop3ClientBuilder withStartTLS(String protocols) {

			// enable the use of the STARTTLS command
			properties.put("mail.pop3.starttls.enable", "true");

			// require the use of the STARTTLS command
			properties.put("mail.pop3.starttls.required", "true");

			setProtocols(protocols);

			return this;
		}

		private void setProtocols(String protocols) {

			// SSL protocols (whitespace separated list)
			properties.put("mail.pop3.ssl.protocols", protocols);

			// SSL cipher suites (whitespace separated list)
			// TODO: support this?
			// properties.put("mail.smtp.ssl.ciphersuites", "");
		}

		public Pop3Client build() {
			Session session = Session.getInstance(properties, authenticator);
			return new Pop3Client(session);
		}

	}

}
