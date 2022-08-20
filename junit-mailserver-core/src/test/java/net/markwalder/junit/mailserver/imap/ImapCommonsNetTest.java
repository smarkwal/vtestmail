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

				TagGenerator tag = new TagGenerator();

				// connect to server
				client.connect("localhost", server.getPort());

				// assert: new session started
				ImapSession session = server.getActiveSession();
				assertThat(session).isNotNull();
				assertThat(session.isAuthenticated()).isFalse();
				assertThat(session.getUsername()).isNull();

				// assert: state is not authenticated
				assertThat(session.getState()).isEqualTo(State.NotAuthenticated);

				// CAPABILITY
				boolean success = client.capability();
				assertThat(success).isTrue();
				assertReply(client,
						"* CAPABILITY IMAP4rev2 STARTTLS",
						tag.next() + " OK CAPABILITY completed"
				);

				// LOGIN with wrong password
				success = client.login(USERNAME, "wrong password");
				assertThat(success).isFalse();
				assertReply(client, tag.next() + " NO [AUTHENTICATIONFAILED] Authentication failed");

				// assert: state is not authenticated
				assertThat(session.getState()).isEqualTo(State.NotAuthenticated);

				// LOGIN
				success = client.login(USERNAME, PASSWORD);
				assertThat(success).isTrue();
				assertReply(client, tag.next() + " OK [CAPABILITY IMAP4rev2 STARTTLS] LOGIN completed");

				// assert: session is authenticated
				assertThat(session.isAuthenticated()).isTrue();
				assertThat(session.getUsername()).isEqualTo(USERNAME);

				// assert: state is authenticated
				assertThat(session.getState()).isEqualTo(State.Authenticated);

				// NOOP
				success = client.noop();
				assertThat(success).isTrue();
				assertReply(client, tag.next() + " OK NOOP completed");

				int replyCode = client.sendCommand("ENABLE", "FOO BAR");
				assertThat(replyCode).isEqualTo(IMAPReply.OK);
				assertReply(client, tag.next() + " OK ENABLE completed");

				// SELECT Drafts
				success = client.select("Drafts");
				assertThat(success).isFalse();
				assertReply(client, tag.next() + " NO [TRYCREATE] No such mailbox");

				// assert: state is authenticated
				assertThat(session.getState()).isEqualTo(State.Authenticated);

				// SELECT INBOX
				success = client.select("INBOX");
				assertThat(success).isTrue();
				assertReply(client,
						"* 2 EXISTS",
						"* OK [UIDVALIDITY 1000000000] UIDs valid",
						"* OK [UIDNEXT 1000000003] Predicted next UID",
						"* FLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft)",
						"* OK [PERMANENTFLAGS (\\Deleted \\Seen \\*)] Limited",
						"* LIST () \"/\" INBOX",
						tag.next() + " OK [READ-WRITE] SELECT completed"
				);

				// assert: state is selected
				assertThat(session.getState()).isEqualTo(State.Selected);

				// mark message 2 as deleted
				mailbox.getMessages().get(1).setDeleted(true);

				// CLOSE
				success = client.close();
				assertThat(success).isTrue();
				assertReply(client, tag.next() + " OK CLOSE completed");

				// assert: state is authenticated
				assertThat(session.getState()).isEqualTo(State.Authenticated);

				// SELECT inbox
				success = client.select("inbox");
				assertThat(success).isTrue();
				assertReply(client,
						"* 1 EXISTS",
						"* OK [UIDVALIDITY 1000000000] UIDs valid",
						"* OK [UIDNEXT 1000000003] Predicted next UID",
						"* FLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft)",
						"* OK [PERMANENTFLAGS (\\Deleted \\Seen \\*)] Limited",
						"* LIST () \"/\" INBOX",
						tag.next() + " OK [READ-WRITE] SELECT completed"
				);

				// assert: state is selected
				assertThat(session.getState()).isEqualTo(State.Selected);

				// mark message 1 as deleted
				mailbox.getMessages().get(0).setDeleted(true);

				// EXPUNGE
				success = client.expunge();
				assertThat(success).isTrue();
				assertReply(client,
						"* 1 EXPUNGE",
						tag.next() + " OK EXPUNGE completed"
				);

				// UNSELECT
				replyCode = client.sendCommand("UNSELECT");
				assertThat(replyCode).isEqualTo(IMAPReply.OK);
				assertReply(client, tag.next() + " OK UNSELECT completed");

				// assert: state is authenticated
				assertThat(session.getState()).isEqualTo(State.Authenticated);

				// TODO: execute more commands

				// CMD1 <-- custom command
				replyCode = client.sendCommand("CMD1");
				assertThat(replyCode).isEqualTo(IMAPReply.OK);
				assertReply(client, tag.next() + " OK CMD1 completed");

				// CMD2 <-- disabled custom command
				replyCode = client.sendCommand("CMD2");
				assertThat(replyCode).isEqualTo(IMAPReply.BAD);
				assertReply(client, tag.next() + " BAD Command disabled");

				// CMD3 <-- unknown command
				replyCode = client.sendCommand("CMD3");
				assertThat(replyCode).isEqualTo(IMAPReply.BAD);
				assertReply(client, tag.next() + " BAD Command not implemented");

				// EXPUNGE <-- not valid in authenticated state
				success = client.expunge();
				assertThat(success).isFalse();
				assertReply(client, tag.next() + " BAD Command is not allowed in Authenticated state");

				// LOGOUT
				success = client.logout();
				assertThat(success).isTrue();
				assertReply(client,
						"* BYE IMAP4rev2 Server logging out",
						tag.next() + " OK LOGOUT completed"
				);

				// assert: state is logout
				assertThat(session.getState()).isEqualTo(State.Logout);

				// assert: session has been closed
				session.waitUntilClosed(5000);
				assertThat(session.isClosed()).isTrue();

				// assert: commands have been recorded
				List<ImapCommand> commands = session.getCommands();
				assertThat(commands).containsExactly(
						new CAPABILITY(),
						new LOGIN(USERNAME, "wrong password"),
						new LOGIN(USERNAME, PASSWORD),
						new NOOP(),
						new ENABLE(List.of("FOO", "BAR")),
						new SELECT("Drafts"),
						new SELECT("INBOX"),
						new CLOSE(),
						new SELECT("inbox"),
						new EXPUNGE(),
						new UNSELECT(),
						new CustomCommand("CMD1"),
						new DisabledCommand("CMD2"),
						new UnknownCommand("CMD3"),
						new EXPUNGE(),
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

	private static class TagGenerator {

		private int counter = 0;

		public String next() {
			String tag = formatTag(counter);
			counter++;
			return tag;
		}

		private String formatTag(int counter) {
			int d4 = counter % 26;
			int d3 = (counter / 26) % 26;
			int d2 = (counter / (26 * 26)) % 26;
			int d1 = (counter / (26 * 26 * 26)) % 26;
			return "" + getLetter(d1) + getLetter(d2) + getLetter(d3) + getLetter(d4);
		}

		private static char getLetter(int digit) {
			return (char) ('A' + digit);
		}

	}

}
