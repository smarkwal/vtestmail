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

package net.markwalder.junit.mailserver.testutils;

import jakarta.mail.Authenticator;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import java.util.Properties;
import net.markwalder.junit.mailserver.pop3.Pop3Server;

public class Pop3Client {

	private final Session session;

	private Pop3Client(Session session) {
		this.session = session;
	}

	public static Pop3ClientBuilder forServer(Pop3Server server) {
		return new Pop3ClientBuilder(server);
	}

	public void getMessages() throws MessagingException {
		Store store = session.getStore("pop3");
		store.connect();
		Folder inbox = store.getFolder("Inbox");
		inbox.open(Folder.READ_ONLY);
		Message[] messages = inbox.getMessages();
		for (Message message : messages) {
			System.out.println(message);
			// TODO: implement
		}
		inbox.close(true);
		store.close();
	}

	public static class Pop3ClientBuilder {

		private final Properties properties = new Properties();
		private Authenticator authenticator = null;

		private Pop3ClientBuilder(Pop3Server server) {
			if (server == null) throw new IllegalArgumentException("server must not be null");

			int port = server.getPort();

			// POP3 properties:
			// https://eclipse-ee4j.github.io/mail/docs/api/jakarta.mail/com/sun/mail/pop3/package-summary.html

			// POP3 server address (host name)
			properties.put("mail.pop3.host", "localhost");

			// POP3 server port
			properties.put("mail.pop3.port", String.valueOf(port));

			// POP3 client address (host name)
			properties.put("mail.pop3.localaddress", "localhost");

			// enable APOP authentication
			// TODO: properties.put("mail.pop3.apop.enable", "true");

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
			if (authType == null) throw new IllegalArgumentException("authType must not be null");
			if (username == null) throw new IllegalArgumentException("username must not be null");
			if (password == null) throw new IllegalArgumentException("password must not be null");

			// If set, lists the authentication mechanisms to consider, and the
			// order in which to consider them. Only mechanisms supported by
			// the server and supported by the current implementation will be
			// used. The default is "LOGIN PLAIN DIGEST-MD5 NTLM", which
			// includes all the authentication mechanisms supported by the
			// current implementation except XOAUTH2.
			properties.put("mail.pop3.auth.mechanisms", authType);

			// create authenticator
			authenticator = new PasswordAuthenticator(username, password);

			return this;
		}

		public Pop3ClientBuilder withEncryption(String protocols) {
			if (protocols == null || protocols.isEmpty()) throw new IllegalArgumentException("protocols must not be null or empty");

			// use SSL to connect to POP3 server
			properties.put("mail.pop3.ssl.enable", "true");

			// SSL protocols (whitespace separated list)
			properties.put("mail.pop3.ssl.protocols", protocols);

			// SSL cipher suites (whitespace separated list)
			// TODO: support this?
			// properties.put("mail.pop3.ssl.ciphersuites", "");

			return this;
		}

		public Pop3ClientBuilder withSSLv3() {
			return withEncryption("SSLv3");
		}

		public Pop3ClientBuilder withTLSv1() {
			return withEncryption("TLSv1");
		}

		public Pop3ClientBuilder withTLSv11() {
			return withEncryption("TLSv1.1");
		}

		public Pop3ClientBuilder withTLSv12() {
			return withEncryption("TLSv1.2");
		}

		public Pop3ClientBuilder withTLSv13() {
			return withEncryption("TLSv1.3");
		}

		public Pop3ClientBuilder withStartTLS() {

			// enable the use of the STARTTLS command
			properties.put("mail.pop3.starttls.enable", "true");

			// require the use of the STARTTLS command
			properties.put("mail.pop3.starttls.required", "true");

			return this;
		}

		public Pop3Client build() {
			Session session = Session.getInstance(properties, authenticator);
			return new Pop3Client(session);
		}

	}

}
