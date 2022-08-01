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

package net.markwalder.junit.mailserver.pop3;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import net.markwalder.junit.mailserver.Mailbox;
import net.markwalder.junit.mailserver.MailboxStore;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.apache.commons.net.pop3.POP3Reply;
import org.junit.jupiter.api.Test;

class Pop3CommonsNetTest {

	private static final String USERNAME = "\u00E4li\u00E7\u00E9";
	private static final String PASSWORD = "p\u00E4ssw\u00F6rd!123";
	private static final String EMAIL = "alice@localhost";

	@Test
	void test() throws IOException {

		// prepare: mailbox
		MailboxStore store = new MailboxStore();
		Mailbox mailbox = store.createMailbox(USERNAME, PASSWORD, EMAIL);
		mailbox.addMessage("Subject: Test 1\r\n\r\nTest message 1");
		mailbox.addMessage("Subject: Test 2\r\n\r\nTest message 2");

		// prepare: POP3 server
		try (Pop3Server server = new Pop3Server(store)) {
			server.setAuthenticationRequired(true);
			server.setCommandEnabled("APOP", false);

			// add custom command CMD1 (enabled)
			server.addCommand("CMD1", parameters -> new CustomCommand("CMD1"));
			// add custom command CMD2 (disabled)
			server.addCommand("CMD2", parameters -> new CustomCommand("CMD2"));
			server.setCommandEnabled("CMD2", false);
			// note: CMD3 is an unknown commend

			server.start();

			// prepare: POP3 client
			POP3Client client = new POP3Client();
			try {

				// connect to server
				client.connect("localhost", server.getPort());

				// assert: new session started
				Pop3Session session = server.getActiveSession();
				assertThat(session).isNotNull();
				assertThat(session.isAuthenticated()).isFalse();
				assertThat(session.getUser()).isNull();

				// CAPA
				boolean success = client.capa();
				assertThat(success).isTrue();
				assertReply(client,
						"+OK Capability list follows",
						"USER",
						"TOP",
						"UIDL",
						"EXPIRE NEVER",
						"IMPLEMENTATION junit-mailserver",
						"."
				);

				// USER and PASS with wrong password
				success = client.login(USERNAME, "wrong password");
				assertThat(success).isFalse();
				assertReply(client, "-ERR Authentication failed");

				// USER and PASS
				success = client.login(USERNAME, PASSWORD);
				assertThat(success).isTrue();

				// assert: session is authenticated
				assertThat(session.isAuthenticated()).isTrue();
				assertThat(session.getUser()).isEqualTo(USERNAME);

				// STAT
				POP3MessageInfo status = client.status();
				assertThat(status.number).isEqualTo(2);
				assertThat(status.size).isEqualTo(66);

				// UIDL
				POP3MessageInfo[] infos = client.listUniqueIdentifiers();
				assertThat(infos).hasSize(2);
				assertThat(infos[0].number).isEqualTo(1);
				assertThat(infos[0].identifier).isEqualTo("c3dcab48ecb4f0e74f1df41cf73684e2");
				assertThat(infos[1].number).isEqualTo(2);
				assertThat(infos[1].identifier).isEqualTo("73b4cb2c1e979bfc24eddfe33fc220ac");

				// UIDL 1
				POP3MessageInfo info = client.listUniqueIdentifier(1);
				assertThat(info).isNotNull();
				assertThat(info.number).isEqualTo(1);
				assertThat(info.identifier).isEqualTo("c3dcab48ecb4f0e74f1df41cf73684e2");

				// LIST
				infos = client.listMessages();
				assertThat(infos).hasSize(2);
				assertThat(infos[0].number).isEqualTo(1);
				assertThat(infos[0].size).isEqualTo(33);
				assertThat(infos[1].number).isEqualTo(2);
				assertThat(infos[1].size).isEqualTo(33);

				// LIST 2
				info = client.listMessage(2);
				assertThat(info).isNotNull();
				assertThat(info.number).isEqualTo(2);
				assertThat(info.size).isEqualTo(33);

				// NOOP
				success = client.noop();
				assertThat(success).isTrue();

				// RETR 1
				Reader reader = client.retrieveMessage(1);
				String message = readMessage(reader);
				assertThat(message).isEqualTo("Subject: Test 1\r\n\r\nTest message 1\r\n");

				// TOP 2 0
				reader = client.retrieveMessageTop(2, 0);
				message = readMessage(reader);
				assertThat(message).isEqualTo("Subject: Test 2\r\n");

				// DELE 1
				success = client.deleteMessage(1);
				assertThat(success).isTrue();

				// assert: message is marked as deleted
				assertThat(mailbox.getMessages().get(0).isDeleted()).isTrue();

				// RSET
				success = client.reset();
				assertThat(success).isTrue();

				// assert: message is NOT marked as deleted
				assertThat(mailbox.getMessages().get(0).isDeleted()).isFalse();

				// DELE 2
				success = client.deleteMessage(2);
				assertThat(success).isTrue();

				// UIDL
				infos = client.listUniqueIdentifiers();
				assertThat(infos).hasSize(1);
				assertThat(infos[0].number).isEqualTo(1);
				assertThat(infos[0].identifier).isEqualTo("c3dcab48ecb4f0e74f1df41cf73684e2");

				// UIDL 2 <-- try to access deleted message
				info = client.listUniqueIdentifier(2);
				assertThat(info).isNull();
				assertReply(client, "-ERR No such message");

				// LIST
				infos = client.listMessages();
				assertThat(infos).hasSize(1);
				assertThat(infos[0].number).isEqualTo(1);
				assertThat(infos[0].size).isEqualTo(33);

				// LIST 2 <-- try to access deleted message
				info = client.listMessage(2);
				assertThat(info).isNull();
				assertReply(client, "-ERR No such message");

				// RETR 2 <-- try to access deleted message
				reader = client.retrieveMessage(2);
				assertThat(reader).isNull();
				assertReply(client, "-ERR No such message");

				// TOP 2 0 <-- try to access deleted message
				reader = client.retrieveMessageTop(2, 0);
				assertThat(reader).isNull();
				assertReply(client, "-ERR No such message");

				// DELE 2 <-- try to delete already deleted message
				success = client.deleteMessage(2);
				assertThat(success).isFalse();
				assertReply(client, "-ERR No such message");

				// CMD1 <-- custom command
				int replyCode = client.sendCommand("CMD1");
				assertThat(replyCode).isEqualTo(POP3Reply.OK);

				// CMD2 <-- disabled custom command
				replyCode = client.sendCommand("CMD2");
				assertThat(replyCode).isEqualTo(POP3Reply.ERROR);
				assertReply(client, "-ERR Unknown command");

				// CMD3 <-- unknown command
				replyCode = client.sendCommand("CMD3");
				assertThat(replyCode).isEqualTo(POP3Reply.ERROR);
				assertReply(client, "-ERR Unknown command");

				// QUIT
				success = client.logout();
				assertThat(success).isTrue();

				// assert: message has been deleted
				assertThat(mailbox.getMessages()).hasSize(1);

				// assert: session has been closed
				assertThat(session.isClosed()).isTrue();

				// assert: commands have been recorded
				List<Pop3Command> commands = session.getCommands();
				assertThat(commands).hasSize(25);
				assertThat(commands).containsExactly(
						new CAPA(),
						new USER(USERNAME),
						new PASS("wrong password"),
						new USER(USERNAME),
						new PASS(PASSWORD),
						new STAT(),
						new UIDL(),
						new UIDL(1),
						new LIST(),
						new LIST(2),
						new NOOP(),
						new RETR(1),
						new TOP(2, 0),
						new DELE(1),
						new RSET(),
						new DELE(2),
						new UIDL(),
						new UIDL(2),
						new LIST(),
						new LIST(2),
						new RETR(2),
						new TOP(2, 0),
						new DELE(2),
						new CustomCommand("CMD1"),
						new QUIT()
				);

			} finally {

				// close connection
				client.disconnect();
			}

		}

	}

	private void assertReply(POP3Client client, String... expectedReply) {
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

	public static class CustomCommand extends Pop3Command {

		private final String name;

		public CustomCommand(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		protected void execute(Pop3Server server, Pop3Session session, Pop3Client client) throws IOException, Pop3Exception {
			client.writeLine("+OK");
		}

	}

}
