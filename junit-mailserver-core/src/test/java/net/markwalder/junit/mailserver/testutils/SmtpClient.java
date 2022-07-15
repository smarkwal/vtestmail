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

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
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

	public Message createMessage(String from, String to, String subject, String body) throws MessagingException {
		return prepareMessage()
				.from(from)
				.to(to)
				.subject(subject)
				.body(body)
				.build();
	}

	public void send(Message message) throws MessagingException {
		Transport.send(message);
	}

	public static class SmtpClientBuilder {

		private final Properties properties = new Properties();

		private SmtpClientBuilder(SmtpServer server) {
			int port = server.getPort();

			// set default properties
			properties.put("mail.smtp.auth", false);
			properties.put("mail.smtp.starttls.enable", "false");
			properties.put("mail.smtp.host", "localhost");
			properties.put("mail.smtp.port", String.valueOf(port));
			// TODO: add more default properties?
		}

		// TODO: add methods to control authentication and encryption (SSL/TLS)
		//  - authentication method (e.g. PLAIN, LOGIN, CRAM-MD5, XOAUTH2, ...)
		//  - username and password (or access token)
		//  - encryption method (e.g. TLS, SSL, ...)

		public SmtpClient build() {
			Session session = Session.getInstance(properties);
			return new SmtpClient(session);
		}

	}

	public static class MessageBuilder {

		private final Message message;

		private MessageBuilder(Session session) {
			this.message = new MimeMessage(session);
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
