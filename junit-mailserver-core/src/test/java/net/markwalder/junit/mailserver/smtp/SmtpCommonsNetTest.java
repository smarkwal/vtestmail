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

import java.io.IOException;
import java.util.List;
import net.markwalder.junit.mailserver.Mailbox;
import net.markwalder.junit.mailserver.MailboxStore;
import net.markwalder.junit.mailserver.testutils.TestUtils;
import org.apache.commons.net.smtp.SMTPClient;
import org.junit.jupiter.api.Test;

public class SmtpCommonsNetTest {

	private static final String USERNAME = "\u00E4li\u00E7\u00E9";
	private static final String PASSWORD = "p\u00E4ssw\u00F6rd!123";
	private static final String EMAIL = "alice@localhost";

	@Test
	void test() throws IOException, InterruptedException {

		// prepare: mailbox
		MailboxStore store = new MailboxStore();
		Mailbox mailbox = store.createMailbox(USERNAME, PASSWORD, EMAIL);

		// prepare: SMTP server
		try (SmtpServer server = new SmtpServer(store)) {
			server.setClock(TestUtils.createTestClock());

			// add custom command CMD1 (enabled)
			server.addCommand("CMD1", parameters -> new CustomCommand("CMD1"));
			// add custom command CMD2 (disabled)
			server.addCommand("CMD2", parameters -> new CustomCommand("CMD2"));
			server.setCommandEnabled("CMD2", false);
			// note: CMD3 is an unknown commend

			server.start();

			// prepare: SMTP client
			SMTPClient client = new SMTPClient();
			try {

				// connect to server
				client.connect("localhost", server.getPort());

				// assert: new session started
				SmtpSession session = server.getActiveSession();
				assertThat(session).isNotNull();
				assertThat(session.isAuthenticated()).isFalse();
				assertThat(session.getUsername()).isNull();

				// HELO localhost
				// TODO: use EHLO instead of HELO
				int replyCode = client.helo("localhost");
				assertThat(replyCode).isEqualTo(250);

				// TODO: login

				// TODO: test all commands

				// NOOP
				replyCode = client.noop();
				assertThat(replyCode).isEqualTo(250);

				// RSET
				boolean success = client.reset();
				assertThat(success).isTrue();

				// VRFY alice@localhost <-- not supported
				success = client.verify(EMAIL);
				assertThat(success).isTrue();
				assertReply(client, "250 2.1.0 <" + EMAIL + "> User OK");

				// MAIL FROM:<bob@localhost>
				replyCode = client.mail("<bob@localhost>");
				assertThat(replyCode).isEqualTo(250);
				assertReply(client, "250 2.1.0 OK");

				// RCPT TO:<alice@localhost>
				replyCode = client.rcpt("<" + EMAIL + ">");
				assertThat(replyCode).isEqualTo(250);
				assertReply(client, "250 2.1.5 OK");

				// DATA
				success = client.sendShortMessageData("Subject: Test 1\r\n\r\nTest message 1");
				assertThat(success).isTrue();
				assertReply(client, "250 2.6.0 Message accepted");

				// MAIL FROM:<dan@localhost>
				replyCode = client.mail("<dan@localhost>");
				assertThat(replyCode).isEqualTo(250);
				assertReply(client, "250 2.1.0 OK");

				// RCPT TO:<alice@localhost>
				replyCode = client.rcpt("<" + EMAIL + ">");
				assertThat(replyCode).isEqualTo(250);
				assertReply(client, "250 2.1.5 OK");

				// DATA
				success = client.sendShortMessageData("Subject: Test 2\r\n\r\nTest message 2");
				assertThat(success).isTrue();
				assertReply(client, "250 2.6.0 Message accepted");

				// CMD1 <-- custom command
				replyCode = client.sendCommand("CMD1");
				assertThat(replyCode).isEqualTo(250);

				// CMD2 <-- disabled custom command
				replyCode = client.sendCommand("CMD2");
				assertThat(replyCode).isEqualTo(502);
				assertReply(client, "502 5.5.1 Command disabled");

				// CMD3 <-- unknown command
				replyCode = client.sendCommand("CMD3");
				assertThat(replyCode).isEqualTo(500);
				assertReply(client, "500 5.5.1 Command not implemented");

				// QUIT
				success = client.logout();
				assertThat(success).isTrue();

				// assert: session has been closed
				session.waitUntilClosed(5000);
				assertThat(session.isClosed()).isTrue();

				// assert: commands have been recorded
				List<SmtpCommand> commands = session.getCommands();
				assertThat(commands).containsExactly(
						new HELO("localhost"),
						new NOOP(),
						new RSET(),
						new VRFY(EMAIL),
						new CustomCommand("CMD1"),
						new DisabledCommand("CMD2"),
						new UnknownCommand("CMD3"),
						new QUIT()
				);

				List<SmtpTransaction> transactions = session.getTransactions();
				assertThat(transactions).hasSize(2);

				SmtpTransaction transaction = transactions.get(0);
				assertThat(transaction.getSender()).isEqualTo("bob@localhost");
				assertThat(transaction.getRecipients()).containsExactly(EMAIL);
				assertThat(transaction.getCommands()).containsExactly(
						new MAIL("bob@localhost"),
						new RCPT(EMAIL),
						new DATA()
				);

				transaction = transactions.get(1);
				assertThat(transaction.getSender()).isEqualTo("dan@localhost");
				assertThat(transaction.getRecipients()).containsExactly(EMAIL);
				assertThat(transaction.getCommands()).containsExactly(
						new MAIL("dan@localhost"),
						new RCPT(EMAIL),
						new DATA()
				);

				// assert: messages have been delivered
				List<Mailbox.Message> messages = mailbox.getMessages();
				assertThat(messages).hasSize(2);

				Mailbox.Message message = messages.get(0);
				assertThat(message.getContent()).isEqualTo("Received: from 127.0.0.1 by 127.0.0.1; Wed, 1 Jan 2020 00:00:00 +0000\r\nSubject: Test 1\r\n\r\nTest message 1");

				message = messages.get(1);
				assertThat(message.getContent()).isEqualTo("Received: from 127.0.0.1 by 127.0.0.1; Wed, 1 Jan 2020 00:00:00 +0000\r\nSubject: Test 2\r\n\r\nTest message 2");

			} finally {

				// close connection
				client.disconnect();
			}

		}

	}

	private void assertReply(SMTPClient client, String... expectedReply) {
		String[] reply = client.getReplyStrings();
		assertThat(reply).containsExactly(expectedReply);
	}

	public static class CustomCommand extends SmtpCommand {

		private final String name;

		public CustomCommand(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		protected void execute(SmtpServer server, SmtpSession session, SmtpClient client) throws IOException, SmtpException {
			client.writeLine("250 OK");
		}

	}

}
