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

package net.markwalder.vtestmail.imap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import net.markwalder.vtestmail.store.Mailbox;
import net.markwalder.vtestmail.store.MailboxFolder;
import net.markwalder.vtestmail.store.MailboxMessage;
import net.markwalder.vtestmail.store.MailboxStore;
import org.apache.commons.net.imap.IMAPClient;
import org.apache.commons.net.imap.IMAPReply;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImapCommonsNetTest {

	private static final String USERNAME = "\u00E4li\u00E7\u00E9";
	private static final String PASSWORD = "p\u00E4ssw\u00F6rd!123";
	private static final String EMAIL = "alice@localhost";

	private ImapServer server;
	private IMAPClient client;
	private TagGenerator tag;

	@BeforeEach
	void setUp() throws IOException {

		// prepare: mailbox
		MailboxStore store = new MailboxStore();
		Mailbox mailbox = store.createMailbox(USERNAME, PASSWORD, EMAIL);
		MailboxFolder folder = mailbox.getFolder("INBOX");
		folder.addMessage("Subject: Test 1\r\n\r\nTest message 1");
		folder.addMessage("Subject: Test 2\r\n\r\nTest message 2");

		// prepare: IMAP server
		server = new ImapServer(store);
		server.setAuthenticationRequired(true);
		server.setLoginDisabled(false); // enable LOGIN on unencrypted connections
		// server.setAuthTypes("LOGIN", "PLAIN");

		// add custom command CMD1 (enabled)
		server.addCommand("CMD1", parameters -> new CustomCommand("CMD1"));
		// add custom command CMD2 (disabled)
		server.addCommand("CMD2", parameters -> new CustomCommand("CMD2"));
		server.setCommandEnabled("CMD2", false);
		// note: CMD3 is an unknown commend

		// start IMAP server
		server.start();

		// prepare: IMAP client
		client = new FixedIMAPClient();

		// prepare: tag generator
		tag = new TagGenerator();

		// connect to server
		client.connect("localhost", server.getPort());
		assertReply(client, "* OK [CAPABILITY IMAP4rev2 STARTTLS] IMAP server ready");

	}

	@AfterEach
	void tearDown() throws IOException {

		if (client != null && client.isConnected()) {

			// LOGOUT
			boolean success = client.logout();
			assertThat(success).isTrue();

			// close connection
			client.disconnect();
		}

		if (server != null) {
			// stop server
			server.stop();
		}

	}

	@Test
	void test() throws IOException, InterruptedException {

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

		// NAMESPACE
		replyCode = client.sendCommand("NAMESPACE");
		assertThat(replyCode).isEqualTo(IMAPReply.OK);
		assertReply(client,
				"* NAMESPACE ((\"\" \"/\")) NIL NIL",
				tag.next() + " OK NAMESPACE completed"
		);

		// STATUS INBOX (MESSAGES UIDNEXT UIDVALIDITY UNSEEN DELETED SIZE)
		success = client.status("INBOX", new String[] { "MESSAGES", "UIDNEXT", "UIDVALIDITY", "UNSEEN", "DELETED", "SIZE" });
		assertThat(success).isTrue();
		assertReply(client,
				"* STATUS INBOX (MESSAGES 2 UIDNEXT 3 UIDVALIDITY 1 UNSEEN 2 DELETED 0 SIZE 66)",
				tag.next() + " OK STATUS completed"
		);

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
				"* OK [UIDVALIDITY 1] UIDs valid",
				"* OK [UIDNEXT 3] Predicted next UID",
				"* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen)",
				"* OK [PERMANENTFLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen \\*)] Limited",
				"* LIST () \"/\" INBOX",
				tag.next() + " OK [READ-WRITE] SELECT completed"
		);

		// assert: state is selected
		assertThat(session.getState()).isEqualTo(State.Selected);

		// assert: session is read-write
		assertThat(session.isReadOnly()).isFalse();

		// mark message 2 as deleted
		MailboxFolder folder = session.getFolder();
		folder.getMessages().get(1).setDeleted(true);

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
				"* OK [UIDVALIDITY 1] UIDs valid",
				"* OK [UIDNEXT 3] Predicted next UID",
				"* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen)",
				"* OK [PERMANENTFLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen \\*)] Limited",
				"* LIST () \"/\" INBOX",
				tag.next() + " OK [READ-WRITE] SELECT completed"
		);

		// assert: state is selected
		assertThat(session.getState()).isEqualTo(State.Selected);

		// mark message 1 as deleted
		folder = session.getFolder();
		folder.getMessages().get(0).setDeleted(true);

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

		// EXAMINE INBOX
		success = client.examine("INBOX");
		assertThat(success).isTrue();
		assertReply(client,
				"* 0 EXISTS",
				"* OK [UIDVALIDITY 1] UIDs valid",
				"* OK [UIDNEXT 3] Predicted next UID",
				"* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen)",
				"* OK [PERMANENTFLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen \\*)] Limited",
				"* LIST () \"/\" INBOX",
				tag.next() + " OK [READ-ONLY] EXAMINE completed"
		);

		// assert: state is selected
		assertThat(session.getState()).isEqualTo(State.Selected);

		// assert: session is read-only
		assertThat(session.isReadOnly()).isTrue();

		// EXPUNGE <-- not valid in read-only mode
		success = client.expunge();
		assertThat(success).isFalse();
		assertReply(client, tag.next() + " NO [READ-ONLY] Mailbox is read-only");

		// UNSELECT
		replyCode = client.sendCommand("UNSELECT");
		assertThat(replyCode).isEqualTo(IMAPReply.OK);
		assertReply(client, tag.next() + " OK UNSELECT completed");

		// assert: state is authenticated
		assertThat(session.getState()).isEqualTo(State.Authenticated);

		// CREATE Work
		success = client.create("Work");
		assertThat(success).isTrue();
		assertReply(client, tag.next() + " OK CREATE completed");

		// RENAME Work Private
		success = client.rename("Work", "Private");
		assertThat(success).isTrue();
		assertReply(client, tag.next() + " OK RENAME completed");

		// SUBSCRIBE Private
		success = client.subscribe("Private");
		assertThat(success).isTrue();
		assertReply(client, tag.next() + " OK SUBSCRIBE completed");

		// UNSUBSCRIBE Private
		success = client.unsubscribe("Private");
		assertThat(success).isTrue();
		assertReply(client, tag.next() + " OK UNSUBSCRIBE completed");

		// DELETE Private
		success = client.delete("Private");
		assertThat(success).isTrue();
		assertReply(client, tag.next() + " OK DELETE completed");

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

		// release client
		client = null;

		// assert: commands have been recorded
		List<ImapCommand> commands = session.getCommands();
		assertThat(commands).containsExactly(
				new CAPABILITY(),
				new LOGIN(USERNAME, "wrong password"),
				new LOGIN(USERNAME, PASSWORD),
				new NOOP(),
				new ENABLE(List.of("FOO", "BAR")),
				new NAMESPACE(),
				new STATUS("INBOX", "MESSAGES", "UIDNEXT", "UIDVALIDITY", "UNSEEN", "DELETED", "SIZE"),
				new SELECT("Drafts"),
				new SELECT("INBOX"),
				new CLOSE(),
				new SELECT("inbox"),
				new EXPUNGE(),
				new UNSELECT(),
				new EXAMINE("INBOX"),
				new EXPUNGE(),
				new UNSELECT(),
				new CREATE("Work"),
				new RENAME("Work", "Private"),
				new SUBSCRIBE("Private"),
				new UNSUBSCRIBE("Private"),
				new DELETE("Private"),
				new CustomCommand("CMD1"),
				new DisabledCommand("CMD2"),
				new UnknownCommand("CMD3"),
				new EXPUNGE(),
				new LOGOUT()
		);

	}

	@Test
	void test_folders() throws IOException {

		// LOGIN
		boolean success = client.login(USERNAME, PASSWORD);
		assertThat(success).isTrue();
		tag.next();

		// CREATE "Foo Bar"
		success = client.create("Foo Bar");
		assertThat(success).isTrue();
		assertReply(client, tag.next() + " OK CREATE completed");

		// RENAME "Foo Bar" "Hello World!"
		success = client.rename("Foo Bar", "Hello World!");
		assertThat(success).isTrue();
		assertReply(client, tag.next() + " OK RENAME completed");

		// DELETE "Hello World!"
		success = client.delete("Hello World!");
		assertThat(success).isTrue();
		assertReply(client, tag.next() + " OK DELETE completed");

	}

	@Test
	void test_login_disabled() throws IOException {

		// enable LOGINDISABLED
		server.setLoginDisabled(true);

		// recheck capabilities
		client.capability();
		assertReply(client,
				"* CAPABILITY IMAP4rev2 STARTTLS LOGINDISABLED",
				tag.next() + " OK CAPABILITY completed"
		);

		// LOGIN
		boolean success = client.login(USERNAME, PASSWORD);
		assertThat(success).isFalse();
		assertReply(client, tag.next() + " NO LOGIN not allowed");

	}

	@Test
	void test_login_withSynchronizingLiterals() throws IOException {

		int replyCode = client.sendCommand("LOGIN", "{" + USERNAME.length() + "}");
		assertThat(replyCode).isEqualTo(IMAPReply.CONT);
		assertReply(client, "+");

		replyCode = client.sendData(USERNAME + " {" + PASSWORD.length() + "}");
		assertThat(replyCode).isEqualTo(IMAPReply.CONT);
		assertReply(client, "+");

		replyCode = client.sendData(PASSWORD);
		assertThat(replyCode).isEqualTo(IMAPReply.OK);
		assertReply(client, tag.next() + " OK [CAPABILITY IMAP4rev2 STARTTLS] LOGIN completed");

	}

	@Test
	void test_login_withNonSynchronizingLiterals() throws IOException {

		String args = "{" + USERNAME.length() + "+}\r\n" + USERNAME + " {" + PASSWORD.length() + "+}\r\n" + PASSWORD;
		int replyCode = client.sendCommand("LOGIN", args);
		assertThat(replyCode).isEqualTo(IMAPReply.OK);
		assertReply(client, tag.next() + " OK [CAPABILITY IMAP4rev2 STARTTLS] LOGIN completed");

	}

	@Test
	void test_login_withInvalidLiteral_XYZ() throws IOException {

		int replyCode = client.sendCommand("LOGIN", "{XYZ}");
		assertThat(replyCode).isEqualTo(IMAPReply.BAD);
		assertReply(client, tag.next() + " BAD Syntax error");

	}

	@Test
	void test_login_withInvalidLiteral_007() throws IOException {

		int replyCode = client.sendCommand("LOGIN", "{007}");
		assertThat(replyCode).isEqualTo(IMAPReply.BAD);
		assertReply(client, tag.next() + " BAD Syntax error");

	}

	@Test
	void test_login_withInvalidLiteral_5000plus() throws IOException {

		int replyCode = client.sendCommand("LOGIN", "{5000+}");
		assertThat(replyCode).isEqualTo(IMAPReply.BAD);
		assertReply(client, tag.next() + " BAD Syntax error");

	}

	@Test
	void test_flags() throws IOException {

		// prepare: \Answered is not supported anymore
		server.removeFlag(MailboxMessage.FLAG_ANSWERED);

		// prepare: \Deleted is not a permanent flag
		server.addFlag(MailboxMessage.FLAG_DELETED, false);

		// prepare: \Recent is added as new permanent flag
		server.addFlag(MailboxMessage.FLAG_RECENT, true);

		// LOGIN
		boolean success = client.login(USERNAME, PASSWORD);
		assertThat(success).isTrue();
		assertReply(client, tag.next() + " OK [CAPABILITY IMAP4rev2 STARTTLS] LOGIN completed");

		// SELECT INBOX
		success = client.select("INBOX");
		assertThat(success).isTrue();
		assertReply(client,
				"* 2 EXISTS",
				"* OK [UIDVALIDITY 1] UIDs valid",
				"* OK [UIDNEXT 3] Predicted next UID",
				"* FLAGS (\\Deleted \\Draft \\Flagged \\Recent \\Seen)",
				"* OK [PERMANENTFLAGS (\\Draft \\Flagged \\Recent \\Seen \\*)] Limited",
				"* LIST () \"/\" INBOX",
				tag.next() + " OK [READ-WRITE] SELECT completed"
		);

	}

	@Test
	void test_store() throws IOException {

		// LOGIN
		boolean success = client.login(USERNAME, PASSWORD);
		assertThat(success).isTrue();
		assertReply(client, tag.next() + " OK [CAPABILITY IMAP4rev2 STARTTLS] LOGIN completed");

		// SELECT INBOX
		success = client.select("INBOX");
		assertThat(success).isTrue();
		assertReply(client,
				"* 2 EXISTS",
				"* OK [UIDVALIDITY 1] UIDs valid",
				"* OK [UIDNEXT 3] Predicted next UID",
				"* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen)",
				"* OK [PERMANENTFLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen \\*)] Limited",
				"* LIST () \"/\" INBOX",
				tag.next() + " OK [READ-WRITE] SELECT completed"
		);

		// assert: no flags set on message 1
		ImapSession session = server.getActiveSession();
		MailboxFolder folder = session.getFolder();
		MailboxMessage message = folder.getMessage(1);
		assertThat(message.getFlags()).isEmpty();

		// STORE 1 FLAGS (\Seen)
		success = client.store("1", "FLAGS", "(\\Seen)");
		assertThat(success).isTrue();
		// TODO: assert update messages
		assertReply(client,
				"* 1 FETCH (FLAGS (\\Seen))",
				tag.next() + " OK STORE completed"
		);

		// assert: \Seen flag set on message 1
		assertThat(message.getFlags()).containsExactly(MailboxMessage.FLAG_SEEN);

		// STORE 1 +FLAGS (\Flagged)
		success = client.store("1", "+FLAGS", "(\\Flagged)");
		assertThat(success).isTrue();
		assertReply(client,
				"* 1 FETCH (FLAGS (\\Flagged \\Seen))",
				tag.next() + " OK STORE completed"
		);

		// assert: \Flagged flag added to message 1
		assertThat(message.getFlags()).containsExactly(MailboxMessage.FLAG_FLAGGED, MailboxMessage.FLAG_SEEN);

		// STORE 1 -FLAGS (\Flagged)
		success = client.store("1", "-FLAGS", "(\\Flagged)");
		assertThat(success).isTrue();
		assertReply(client,
				"* 1 FETCH (FLAGS (\\Seen))",
				tag.next() + " OK STORE completed"
		);

		// assert: \Flagged flag removed from message 1
		assertThat(message.getFlags()).containsExactly(MailboxMessage.FLAG_SEEN);

		// STORE 1 FLAGS.SILENT (\Draft)
		success = client.store("1", "+FLAGS.SILENT", "(\\Draft)");
		assertThat(success).isTrue();
		assertReply(client,
				tag.next() + " OK STORE completed"
		);

		// assert: \Draft flag added to message 1
		assertThat(message.getFlags()).containsExactly(MailboxMessage.FLAG_DRAFT, MailboxMessage.FLAG_SEEN);

		// STORE 1 FLAGS ()
		success = client.store("1", "FLAGS", "()");
		assertThat(success).isTrue();
		assertReply(client,
				"* 1 FETCH (FLAGS ())",
				tag.next() + " OK STORE completed"
		);

		// assert: all flags removed from message 1
		assertThat(message.getFlags()).isEmpty();

		// STORE 1:* +FLAGS (\Deleted)
		success = client.store("1:*", "+FLAGS", "(\\Deleted)");
		assertThat(success).isTrue();
		assertReply(client,
				"* 1 FETCH (FLAGS (\\Deleted))",
				"* 2 FETCH (FLAGS (\\Deleted))",
				tag.next() + " OK STORE completed"
		);

		// assert: \Deleted flag added to all messages
		for (MailboxMessage folderMessage : folder.getMessages()) {
			assertThat(folderMessage.getFlags()).containsExactly(MailboxMessage.FLAG_DELETED);
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

	/**
	 * Patched IMAP client which quotes username and password if needed.
	 */
	private static class FixedIMAPClient extends IMAPClient {

		@Override
		public boolean login(String username, String password) throws IOException {
			return super.login(quote(username), quote(password));
		}

		private static String quote(String value) {
			if (value == null || value.isEmpty()) {
				return "\"\"";
			} else if (!value.matches("[a-zA-Z0-9]")) {
				value = value.replace("\\", "\\\\");
				value = value.replace("\"", "\\\"");
				return "\"" + value + "\"";
			} else {
				return value;
			}
		}

	}

}
