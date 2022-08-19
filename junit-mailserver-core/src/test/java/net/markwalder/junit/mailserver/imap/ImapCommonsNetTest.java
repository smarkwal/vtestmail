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

package net.markwalder.junit.mailserver.imap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import net.markwalder.junit.mailserver.Mailbox;
import net.markwalder.junit.mailserver.MailboxStore;
import org.apache.commons.net.imap.IMAPClient;
import org.apache.commons.net.imap.IMAPReply;
import org.junit.jupiter.api.Test;

class ImapCommonsNetTest {

	private static final String USERNAME = "\u00E4li\u00E7\u00E9";
	private static final String PASSWORD = "p\u00E4ssw\u00F6rd!123";
	private static final String EMAIL = "alice@localhost";

	@Test
	void test() throws IOException, InterruptedException {

		// prepare: mailbox
		MailboxStore store = new MailboxStore();
		Mailbox mailbox = store.createMailbox(USERNAME, PASSWORD, EMAIL);
		mailbox.addMessage("Subject: Test 1\r\n\r\nTest message 1");
		mailbox.addMessage("Subject: Test 2\r\n\r\nTest message 2");

		// prepare: IMAP server
		try (ImapServer server = new ImapServer(store)) {
			server.setAuthenticationRequired(true);
			// server.setAuthTypes("LOGIN", "PLAIN");

			// add custom command CMD1 (enabled)
			server.addCommand("CMD1", parameters -> new CustomCommand("CMD1"));
			// add custom command CMD2 (disabled)
			server.addCommand("CMD2", parameters -> new CustomCommand("CMD2"));
			server.setCommandEnabled("CMD2", false);
			// note: CMD3 is an unknown commend

			server.start();

			// prepare: IMAP client
			IMAPClient client = new IMAPClient();
			try {

				// connect to server
				client.connect("localhost", server.getPort());

				// assert: new session started
				ImapSession session = server.getActiveSession();
				assertThat(session).isNotNull();
				assertThat(session.isAuthenticated()).isFalse();
				assertThat(session.getUsername()).isNull();

				// CAPABILITY
				boolean success = client.capability();
				assertThat(success).isTrue();
				assertReply(client, "* CAPABILITY IMAP4rev2 STARTTLS", "AAAA OK CAPABILITY completed");

				// LOGIN with wrong password
				success = client.login(USERNAME, "wrong password");
				assertThat(success).isFalse();
				assertReply(client, "AAAB NO [AUTHENTICATIONFAILED] Authentication failed");

				// LOGIN
				success = client.login(USERNAME, PASSWORD);
				assertThat(success).isTrue();
				assertReply(client, "AAAC OK [CAPABILITY IMAP4rev2 STARTTLS] LOGIN completed");

				// assert: session is authenticated
				assertThat(session.isAuthenticated()).isTrue();
				assertThat(session.getUsername()).isEqualTo(USERNAME);

				// NOOP
				success = client.noop();
				assertThat(success).isTrue();
				assertReply(client, "AAAD OK NOOP completed");

				int replyCode = client.sendCommand("ENABLE", "FOO BAR");
				assertThat(replyCode).isEqualTo(IMAPReply.OK);
				assertReply(client, "AAAE OK ENABLE completed");

				// TODO: execute more commands

				// CMD1 <-- custom command
				replyCode = client.sendCommand("CMD1");
				assertThat(replyCode).isEqualTo(IMAPReply.OK);
				assertReply(client, "AAAF OK CMD1 completed");

				// CMD2 <-- disabled custom command
				replyCode = client.sendCommand("CMD2");
				assertThat(replyCode).isEqualTo(IMAPReply.BAD);
				assertReply(client, "AAAG BAD Command not implemented");

				// CMD3 <-- unknown command
				replyCode = client.sendCommand("CMD3");
				assertThat(replyCode).isEqualTo(IMAPReply.BAD);
				assertReply(client, "AAAH BAD Command not implemented");

				// LOGOUT
				success = client.logout();
				assertThat(success).isTrue();
				assertReply(client, "* BYE IMAP4rev2 Server logging out", "AAAI OK LOGOUT completed");

				// assert: session has been closed
				session.waitUntilClosed(5000);
				assertThat(session.isClosed()).isTrue();

				// assert: commands have been recorded
				List<ImapCommand> commands = session.getCommands();
				assertThat(commands).hasSize(7);
				assertThat(commands).containsExactly(
						new CAPABILITY(),
						new LOGIN(USERNAME, "wrong password"),
						new LOGIN(USERNAME, PASSWORD),
						new NOOP(),
						new ENABLE(List.of("FOO", "BAR")),
						new CustomCommand("CMD1"),
						new LOGOUT()
				);

			} finally {

				// close connection
				client.disconnect();
			}

		}

	}

	private void assertReply(IMAPClient client, String... expectedReply) {
		String[] reply = client.getReplyStrings();
		assertThat(reply).containsExactly(expectedReply);
	}

	private static String readMessage(Reader reader) throws IOException {
		StringBuilder buffer = new StringBuilder();
		while (true) {
			int chr = reader.read();
			if (chr < 0) break;
			buffer.append((char) chr);
		}
		return buffer.toString();
	}

	public static class CustomCommand extends ImapCommand {

		private final String name;

		public CustomCommand(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {
			client.writeLine(tag + " OK " + name + " completed");
		}

	}

}
