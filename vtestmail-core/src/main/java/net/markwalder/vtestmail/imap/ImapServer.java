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
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.markwalder.vtestmail.core.MailCommand;
import net.markwalder.vtestmail.core.MailException;
import net.markwalder.vtestmail.core.MailServer;
import net.markwalder.vtestmail.store.MailboxMessage;
import net.markwalder.vtestmail.store.MailboxStore;
import net.markwalder.vtestmail.utils.Assert;
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

	private boolean loginDisabled = true;

	/**
	 * Supported flags. Key: flag name, value: permanent flag.
	 */
	private final Map<String, Boolean> flags = new TreeMap<>(StringUtils.CASE_INSENSITIVE);

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

		// add flags
		addFlag(MailboxMessage.FLAG_SEEN, true);
		addFlag(MailboxMessage.FLAG_ANSWERED, true);
		addFlag(MailboxMessage.FLAG_FLAGGED, true);
		addFlag(MailboxMessage.FLAG_DELETED, true);
		addFlag(MailboxMessage.FLAG_DRAFT, true);
	}

	public boolean isLoginDisabled() {
		return loginDisabled;
	}

	public void setLoginDisabled(boolean loginDisabled) {
		this.loginDisabled = loginDisabled;
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
		client.writeLine("* OK [" + StringUtils.join(capabilities, " ") + "] IMAP server ready");
	}

	@Override
	@SuppressWarnings("StringConcatenationInLoop")
	protected String readCommand() throws ImapException, IOException {
		String line = client.readLine();

		// get tag
		String tag = StringUtils.substringBefore(line, " ");

		// while line ends with a literal ...
		while (line != null && line.contains("{") && line.endsWith("}")) {

			// literal = "{" number64 ["+"] "}" CRLF *CHAR8 ; <number64> represents the number of CHAR8s.
			// A non-synchronizing literal is distinguished from a synchronizing literal by the presence of "+" before the closing "}".
			// number64 = 1*DIGIT ; Unsigned 63-bit integer ; (0 <= n <= 9,223,372,036,854,775,807)

			// parse literal (number of characters and synchronizing flag)
			String value;
			boolean synchronizing = true;
			int pos = line.lastIndexOf('{');
			if (line.endsWith("+}")) {
				// non-synchronizing literal
				value = line.substring(pos + 1, line.length() - 2);
				synchronizing = false;
			} else {
				// synchronizing literal
				value = line.substring(pos + 1, line.length() - 1);
			}

			// number must contain only digits
			if (!value.matches("0|[1-9][0-9]*")) {
				throw ImapException.SyntaxError(tag);
			}

			// Unless otherwise specified in an IMAP extension,
			// non-synchronizing literals MUST NOT be larger than 4096
			// octets. Any literal larger than 4096 bytes MUST be sent as a
			// synchronizing literal.

			BigInteger number = new BigInteger(value);
			BigInteger minNumber = BigInteger.ZERO;
			BigInteger maxNumber = synchronizing ? BigInteger.valueOf(Long.MAX_VALUE) : BigInteger.valueOf(4096);
			if (number.compareTo(minNumber) < 0 || number.compareTo(maxNumber) > 0) {

				// (Non-synchronizing literals defined in this document are the
				// same as non-synchronizing literals defined by the LITERAL-
				// extension from [RFC7888]. See that document for details on
				// how to handle invalid non-synchronizing literals longer than
				// 4096 octets and for interaction with other IMAP extensions.)
				// TODO: implement correct error handling

				throw ImapException.SyntaxError(tag);
			}

			// synchronizing literal -> tell client to proceed with the literal
			if (synchronizing) {
				client.writeContinue(null);
			}

			// read literal characters
			long len = number.intValue();
			String chars = client.readChars(len); // TODO: re-encode to UTF-8?

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

		String tag = null;
		if (e instanceof ImapException) {
			ImapException ie = (ImapException) e;
			tag = ie.getTag();
		}

		// if tag is not known, try to get it from command line
		if (tag == null) {
			if (line == null) {
				// send bye response with alert and close connection
				client.writeLine("* BYE [ALERT] " + e.getMessage());
				session.close();
				return;
			}

			tag = StringUtils.substringBefore(line, " ");
		}

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

		if (loginDisabled) {
			if (!session.isEncrypted()) {
				capabilities.add("LOGINDISABLED");
			}
		}

		// TODO: implement support for UTF-8

		return capabilities;
	}

	public void addFlag(String flag, boolean permanent) {
		Assert.isNotEmpty(flag, "flag");
		synchronized (flags) {
			flags.put(flag, permanent);
		}
	}

	public boolean hasFlag(String flag) {
		Assert.isNotEmpty(flag, "flag");
		synchronized (flags) {
			return flags.containsKey(flag);
		}
	}

	public boolean isPermanentFlag(String flag) {
		Assert.isNotEmpty(flag, "flag");
		synchronized (flags) {
			return flags.getOrDefault(flag, false);
		}
	}

	public void removeFlag(String flag) {
		Assert.isNotEmpty(flag, "flag");
		synchronized (flags) {
			flags.remove(flag);
		}
	}

	public List<String> getFlags() {
		synchronized (flags) {
			return new ArrayList<>(flags.keySet());
		}
	}

	public List<String> getPermanentFlags() {
		synchronized (flags) {
			return flags.entrySet().stream()
					.filter(Map.Entry::getValue) // only permanent flags
					.map(Map.Entry::getKey)
					.collect(Collectors.toList());
		}
	}

}
