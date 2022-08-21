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

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.markwalder.junit.mailserver.core.MailClient;
import net.markwalder.junit.mailserver.store.Mailbox;
import net.markwalder.junit.mailserver.store.MailboxFolder;
import net.markwalder.junit.mailserver.store.MailboxStore;

public class DATA extends SmtpCommand {

	private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("E, d MMM uuuu HH:mm:ss Z").withLocale(Locale.US);

	public DATA() {
	}

	public static DATA parse(String parameters) throws SmtpException {
		isNull(parameters);
		return new DATA();
	}

	@Override
	public String toString() {
		return "DATA";
	}

	@Override
	protected void execute(SmtpServer server, SmtpSession session, SmtpClient client) throws IOException, SmtpException {

		if (server.isAuthenticationRequired()) {
			throw SmtpException.AuthenticationRequired();
		}

		// ask client to send message data
		client.writeLine("354 Send message, end with <CRLF>.<CRLF>");

		// read message data from client
		String message = readMessage(client);

		// add a "Received" header at the top of the message
		message = addReceivedHeader(session, server, message);

		// deliver message to local recipients
		deliverMessage(message, server, session);

		// clear sender, list of recipients, and message data
		session.endTransaction(message);

		client.writeLine("250 2.6.0 Message accepted");
	}

	/**
	 * Read message until a line with only a dot is received.
	 */
	private String readMessage(MailClient client) throws IOException {

		StringBuilder message = new StringBuilder();
		while (true) {

			String line = client.readLine();
			if (line.equals(".")) {
				// end of message detected
				break;
			}

			// remove leading dot (if present)
			if (line.startsWith(".")) {
				line = line.substring(1);
			}

			// add line to message
			if (message.length() > 0) {
				message.append("\r\n");
			}
			message.append(line);
		}

		return message.toString();
	}

	/**
	 * Add a "Received" header to the start of the given message.
	 * See <a href="https://datatracker.ietf.org/doc/html/rfc5322#section-3.6.7">RFC 5322, section 3.6.7</a>.
	 *
	 * @param session SMTP session.
	 * @param server  SMTP server.
	 * @param message Message.
	 * @return Message with "Received" header.
	 */
	private String addReceivedHeader(SmtpSession session, SmtpServer server, String message) {
		String dateTime = formatDateTime(server.getClock());
		String stamp = String.format("from %s by %s; %s", session.getClientAddress(), session.getServerAddress(), dateTime);
		return String.format("Received: %s\r\n%s", stamp, message);
	}

	/**
	 * Format current date and time of given clock.
	 * See <a href="https://datatracker.ietf.org/doc/html/rfc5322#section-3.3">RFC 5322, section 3.3</a>.
	 *
	 * @param clock Clock to use.
	 * @return Formatted date and time.
	 */
	static String formatDateTime(Clock clock) {
		ZoneId zone = clock.getZone();
		DateTimeFormatter formatter = DATETIME_FORMAT.withZone(zone);
		Instant instant = clock.instant();
		return formatter.format(instant);
	}

	/**
	 * Deliver message to mailboxes of known recipients.
	 */
	private void deliverMessage(String message, SmtpServer server, SmtpSession session) {
		MailboxStore store = server.getStore();
		for (String email : session.getRecipients()) {
			Mailbox mailbox = store.findMailbox(email);
			if (mailbox != null) {
				MailboxFolder folder = mailbox.getInbox();
				folder.addMessage(message);
			}
		}
	}

}
