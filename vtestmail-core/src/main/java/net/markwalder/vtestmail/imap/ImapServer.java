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

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import net.markwalder.vtestmail.core.MailCommand;
import net.markwalder.vtestmail.core.MailException;
import net.markwalder.vtestmail.core.MailServer;
import net.markwalder.vtestmail.store.MailboxStore;
import net.markwalder.vtestmail.utils.StringUtils;

/**
 * Virtual IMAP server for integration tests.
 * <p>
 * Limitations:
 * <ul>
 *     <li>Only one client can connect to the server at a time.</li>
 *     <li>Only IMAP4rev2 (RFC 9051) is supported.</li>
 *     <li>The format of messages is not validated.</li>
 * </ul>
 */
public class ImapServer extends MailServer<ImapCommand, ImapSession, ImapClient, ImapException> {

	public ImapServer(MailboxStore store) {
		super("IMAP", store);

		//  Internet Message Access Protocol (IMAP) - Version 4rev2
		// https://datatracker.ietf.org/doc/html/rfc9051

		// register available IMAP commands

		// any state
		addCommand("CAPABILITY", CAPABILITY::parse);
		addCommand("NOOP", NOOP::parse);
		addCommand("LOGOUT", LOGOUT::parse);

		// not authenticated state
		addCommand("STARTTLS", STARTTLS::parse); // TODO: implement test for STARTTLS
		addCommand("LOGIN", LOGIN::parse);
		addCommand("AUTHENTICATE", AUTHENTICATE::parse);

		// authenticated state
		addCommand("ENABLE", ENABLE::parse);
		addCommand("SELECT", SELECT::parse);
		addCommand("EXAMINE", EXAMINE::parse);
		addCommand("CREATE", CREATE::parse);
		addCommand("DELETE", DELETE::parse);
		addCommand("RENAME", RENAME::parse);
		addCommand("SUBSCRIBE", SUBSCRIBE::parse);
		addCommand("UNSUBSCRIBE", UNSUBSCRIBE::parse);
		// TODO: LIST
		addCommand("NAMESPACE", NAMESPACE::parse);
		addCommand("STATUS", STATUS::parse);
		// TODO: APPEND
		// TODO: IDLE

		// selected state
		addCommand("CLOSE", CLOSE::parse);
		addCommand("UNSELECT", UNSELECT::parse);
		addCommand("EXPUNGE", EXPUNGE::parse);
		// TODO: SEARCH
		// TODO: FETCH
		// TODO: STORE
		// TODO: COPY
		// TODO: MOVE
		// TODO: UID
	}

	@Override
	protected ImapClient createClient(Socket socket, StringBuilder log) throws IOException {
		return new ImapClient(socket, log);
	}

	@Override
	protected ImapSession createSession() {
		return new ImapSession();
	}

	@Override
	protected void handleNewClient() throws IOException {
		List<String> capabilities = getCapabilities(session);
		client.writeLine("* OK [" + StringUtils.join(capabilities, " ") + "]  Server ready");
	}

	@Override
	@SuppressWarnings("StringConcatenationInLoop")
	protected String readCommand() throws IOException {
		String line = client.readLine();

		// while line ends with a literal ...
		while (line.contains("{") && line.endsWith("}")) {

			// parse literal (number of characters and synchronizing flag)
			long number;
			boolean synchronizing = true;
			int pos = line.lastIndexOf('{');
			if (line.endsWith("+}")) {
				// non-synchronizing literal
				String value = line.substring(pos + 1, line.length() - 2);
				number = Long.parseLong(value);
				synchronizing = false;
			} else {
				// synchronizing literal
				String value = line.substring(pos + 1, line.length() - 1);
				number = Long.parseLong(value);
			}

			// synchronizing literal -> tell client to proceed with the literal
			if (synchronizing) {
				client.writeContinue(null);
			}

			// read literal characters
			String chars = client.readChars(number); // TODO: re-encode to UTF-8?

			// read rest of line
			String remaining = client.readLine();

			// add literal data to command line
			line += "\r\n" + chars + remaining;
		}

		return line;
	}

	@Override
	protected void handleCommand(String line) throws ImapException, IOException {

		// get tag
		String tag = StringUtils.substringBefore(line, " ");
		line = StringUtils.substringAfter(line, " ");

		// parse command line
		ImapCommand command = createCommand(line);

		// add command to history
		session.addCommand(command);

		// execute command
		command.execute(this, session, client, tag);

	}

	protected ImapCommand createCommand(String line) throws ImapException {

		// split line into command name and parameters
		String name = StringUtils.substringBefore(line, " ").toUpperCase();
		String parameters = StringUtils.substringAfter(line, " ");

		// check if command is supported
		if (!hasCommand(name)) {
			return new UnknownCommand(line);
		} else if (!isCommandEnabled(name)) {
			return new DisabledCommand(line);
		}

		// create command instance
		MailCommand.Parser<ImapCommand, ImapException> commandFactory = commands.get(name);
		return commandFactory.parse(parameters);
	}

	@Override
	protected void handleException(String line, MailException e) throws IOException {
		String tag = StringUtils.substringBefore(line, " ");
		client.writeLine(tag + " " + e.getMessage());
	}

	/**
	 * Get list of IMAP capabilities supported by this server. This list will be
	 * returned when the CAPABILITY command is sent to the server.
	 *
	 * @param session IMAP session.
	 * @return List of IMAP capabilities.
	 */
	protected List<String> getCapabilities(ImapSession session) {
		List<String> capabilities = new ArrayList<>();

		if (isCommandEnabled("CAPABILITY")) {
			capabilities.add("CAPABILITY");
		}

		capabilities.add("IMAP4rev2");

		if (!session.isEncrypted()) {
			if (isCommandEnabled("STARTTLS")) {
				capabilities.add("STARTTLS");
			}
		}

		if (isCommandEnabled("AUTHENTICATE")) {
			List<String> authTypes = getAuthTypes();
			for (String authType : authTypes) {
				capabilities.add("AUTH=" + authType);
			}
		}

		// TODO: implemenmt LOGINDISABLED
		// if (!session.isEncrypted()) {
		// 	capabilities.add("LOGINDISABLED");
		// }

		// TODO: implement support for UTF-8

		return capabilities;
	}

}
