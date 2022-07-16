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
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;
import net.markwalder.junit.mailserver.SmtpServer;

public class SmtpClient {

	private final Session session;

	private SmtpClient(Session session) {
		this.session = session;
	}

	public static SmtpClientBuilder forServer(SmtpServer server) {
		return new SmtpClientBuilder(server);
	}

	public MessageBuilder prepareMessage() {
		return new MessageBuilder(session);
	}

	public void send(Message message) throws MessagingException {
		Transport.send(message);
	}

	public static class SmtpClientBuilder {

		private final Properties properties = new Properties();
		private Authenticator authenticator = null;

		private SmtpClientBuilder(SmtpServer server) {
			if (server == null) throw new IllegalArgumentException("server must not be null");

			int port = server.getPort();

			// SMTP properties:
			// https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/package-summary.html

			// SMTP server address (host name)
			properties.put("mail.smtp.host", "localhost");

			// SMTP server port
			properties.put("mail.smtp.port", String.valueOf(port));

			// host name used in HELO or EHLO command
			properties.put("mail.smtp.localhost", "localhost");

			// SMTP client address (host name)
			properties.put("mail.smtp.localaddress", "localhost");

			// try to use EHLO before HELO
			properties.put("mail.smtp.ehlo", "true");

			// disable authentication and encryption by default
			properties.put("mail.smtp.auth", "false");
			properties.put("mail.smtp.ssl.enable", "false");
			properties.put("mail.smtp.starttls.enable", "false");
			properties.put("mail.smtp.ssl.trust", "*");

			// socket timeouts
			properties.put("mail.smtp.connectiontimeout", "5000"); // connect
			properties.put("mail.smtp.timeout", "5000"); // read
			properties.put("mail.smtp.writetimeout", "5000"); // write
		}

		public SmtpClientBuilder withAuthentication(String authType, String username, String password) {
			if (authType == null) throw new IllegalArgumentException("authType must not be null");
			if (username == null) throw new IllegalArgumentException("username must not be null");
			if (password == null) throw new IllegalArgumentException("password must not be null");

			// If true, attempt to authenticate the user using the AUTH command.
			// Defaults to false.
			properties.put("mail.smtp.auth", "true");

			// If set, lists the authentication mechanisms to consider, and the
			// order in which to consider them. Only mechanisms supported by the
			// server and supported by the current implementation will be used.
			// The default is "LOGIN PLAIN DIGEST-MD5 NTLM", which includes all
			// the authentication mechanisms supported by the current
			// implementation except XOAUTH2.
			properties.put("mail.smtp.auth.mechanisms", authType);

			// create authenticator
			authenticator = new PasswordAuthenticator(username, password);

			return this;
		}

		public SmtpClientBuilder withEncryption(String protocols) {
			if (protocols == null || protocols.isEmpty()) throw new IllegalArgumentException("protocols must not be null or empty");

			// use SSL to connect to SMTP server
			properties.put("mail.smtp.ssl.enable", "true");

			// SSL protocols (whitespace separated list)
			properties.put("mail.smtp.ssl.protocols", protocols);

			// SSL cipher suites (whitespace separated list)
			// TODO: support this?
			// properties.put("mail.smtp.ssl.ciphersuites", "");

			return this;
		}

		public SmtpClientBuilder withSSLv3() {
			return withEncryption("SSLv3");
		}

		public SmtpClientBuilder withTLSv1() {
			return withEncryption("TLSv1");
		}

		public SmtpClientBuilder withTLSv11() {
			return withEncryption("TLSv1.1");
		}

		public SmtpClientBuilder withTLSv12() {
			return withEncryption("TLSv1.2");
		}

		public SmtpClientBuilder withTLSv13() {
			return withEncryption("TLSv1.3");
		}

		public SmtpClientBuilder withStartTLS() {

			// enable the use of the STARTTLS command
			properties.put("mail.smtp.starttls.enable", "true");

			// require the use of the STARTTLS command
			properties.put("mail.smtp.starttls.required", "true");

			return this;
		}

		public SmtpClient build() {
			Session session = Session.getInstance(properties, authenticator);
			return new SmtpClient(session);
		}

	}

	public static class MessageBuilder {

		private final CustomMimeMessage message;

		private MessageBuilder(Session session) {
			this.message = new CustomMimeMessage(session);
		}

		public MessageBuilder messageId(String messageId) {
			message.setMessageId(messageId);
			return this;
		}

		public MessageBuilder date(OffsetDateTime date) throws MessagingException {
			// example date: Wed, 1 Jan 2020 00:00:00 +0000
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E, d MMM yyyy HH:mm:ss xx", Locale.US);
			String value = date.format(formatter);
			return header("Date", value);
		}

		public MessageBuilder header(String name, String value) throws MessagingException {
			message.setHeader(name, value);
			return this;
		}

		public MessageBuilder from(String address) throws MessagingException {
			message.setFrom(new InternetAddress(address, true));
			return this;
		}

		public MessageBuilder to(String address) throws MessagingException {
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(address));
			return this;
		}

		public MessageBuilder cc(String address) throws MessagingException {
			message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(address));
			return this;
		}

		public MessageBuilder bcc(String address) throws MessagingException {
			message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(address));
			return this;
		}

		public MessageBuilder subject(String subject) throws MessagingException {
			message.setSubject(subject);
			return this;
		}

		public MessageBuilder body(String text) throws MessagingException {
			message.setContent(text, "text/plain; charset=utf-8");
			return this;
		}

		public Message build() {
			return message;
		}

	}
}
