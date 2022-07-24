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

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.markwalder.junit.mailserver.Client;
import net.markwalder.junit.mailserver.MailServer;
import net.markwalder.junit.mailserver.Mailbox;
import net.markwalder.junit.mailserver.MailboxStore;
import org.apache.commons.lang3.StringUtils;

/**
 * Virtual SMTP server for testing.
 * <p>
 * Limitations:
 * <ul>
 *     <li>Only one client can connect to the server at a time.</li>
 *     <li>Support for STARTTLS command is not implemented.</li>
 *     <li>The format of messages is not validated.</li>
 *     <li>The mailbox is not exclusively locked by the server.</li>
 * </ul>
 */
public class Pop3Server extends MailServer {

	private static final Map<String, Command> commands = new HashMap<>();

	static {
		commands.put("CAPA", new CAPA());
		commands.put("AUTH", new AUTH());
		commands.put("APOP", new APOP());
		commands.put("USER", new USER());
		commands.put("PASS", new PASS());
		commands.put("STAT", new STAT());
		commands.put("LIST", new LIST());
		commands.put("UIDL", new UIDL());
		commands.put("RETR", new RETR());
		commands.put("DELE", new DELE());
		commands.put("TOP", new TOP());
		commands.put("NOOP", new NOOP());
		commands.put("RSET", new RSET());
		commands.put("QUIT", new QUIT());
	}

	enum State {
		AUTHORIZATION,
		TRANSACTION,
		UPDATE
	}

	private final Map<String, Boolean> enabledCommands = new HashMap<>();

	private State state = null;
	private String timestamp = null;
	private String username = null;

	public Pop3Server(MailboxStore store) {
		super("POP3", store);
	}

	public Boolean isCommandEnabled(String command) {
		if (command == null) throw new IllegalArgumentException("command must not be null");
		command = command.toUpperCase();
		return commands.containsKey(command) && enabledCommands.getOrDefault(command, true);
	}

	public void setCommandEnabled(String command, boolean enabled) {
		if (command == null) throw new IllegalArgumentException("command must not be null");
		command = command.toUpperCase();
		if (enabled) {
			enabledCommands.put(command, true);
		} else {
			enabledCommands.put(command, false);
		}
	}

	/**
	 * Check if session is currently in given state.
	 *
	 * @param expectedState Expected state.
	 */
	void assertState(State expectedState) throws ProtocolException {
		if (this.state != expectedState) {
			throw ProtocolException.IllegalState(this.state);
		}
	}

	void setState(State state) {
		this.state = state;
	}

	String getTimestamp() {
		return timestamp;
	}

	@Override
	public String getUsername() {
		return username;
	}

	void setUsername(String username) {
		this.username = username;
	}

	@Override
	protected void reset(boolean logout) {

		// "forget" all state
		state = null;
		timestamp = null;
		username = null;

		super.reset(logout);
	}

	@Override
	protected Client createClient(Socket socket, StringBuilder log) throws IOException {
		return new Pop3Client(socket, log);
	}

	@Override
	protected void handleNewClient() throws IOException {

		// calculate a new timestamp for APOP authentication
		timestamp = "<" + System.currentTimeMillis() + "@localhost>";

		client.writeLine("+OK POP3 server ready " + timestamp);

		// enter authorization state
		state = State.AUTHORIZATION;
	}

	@Override
	protected boolean handleCommand(String line) throws IOException {

		// convert the first word of the command to uppercase
		// (POP3 commands are case-insensitive)
		line = convertToUppercase(line);

		String name = StringUtils.substringBefore(line, " ");
		Command command = commands.get(name);
		if (command == null) {
			client.writeLine("-ERR Unknown command");
			return false;
		}

		if (!isCommandEnabled(name)) {
			client.writeLine("-ERR Disabled command");
			return false;
		}

		try {
			command.execute(line, this, client);
		} catch (ProtocolException e) {
			client.writeLine("-ERR " + e.getMessage());
		}

		return (command instanceof QUIT);
	}

	@Override
	protected void login(String username, String secret) {
		super.login(username, secret);
	}

	@Override
	protected void login(String username, String digest, String timestamp) {
		super.login(username, digest, timestamp);

		if (isAuthenticated()) {
			// remember username for mailbox access
			this.username = username;
		}
	}

	@Override
	protected void logout() {
		super.logout();
	}

	// helper methods --------------------------------------------------

	/**
	 * Convert the first word in the given command to uppercase.
	 * If the command is only a single word, the complete command is converted.
	 * If the command is {@code null}, the method returns {@code null}.
	 *
	 * @param command Command (may be null)
	 * @return Command with first word in uppercase.
	 */
	private static String convertToUppercase(String command) {
		if (command != null) {
			int pos = command.indexOf(' ');
			if (pos > 0) {
				command = command.substring(0, pos).toUpperCase() + command.substring(pos);
			} else {
				command = command.toUpperCase();
			}
		}
		return command;
	}

	List<Mailbox.Message> getMessages(String username) {
		Mailbox mailbox = store.getMailbox(username);
		if (mailbox == null) {
			// mailbox not found -> return empty list
			return Collections.emptyList();
		}
		return mailbox.getMessages();
	}

	Mailbox.Message getMessage(String username, String msg) {

		// try to parse parameter "msg"
		int idx;
		try {
			idx = Integer.parseInt(msg) - 1;
		} catch (NumberFormatException e) {
			// not a number -> message not found
			return null;
		}

		List<Mailbox.Message> messages = getMessages(username);
		if (idx < 0 || idx >= messages.size()) {
			// index out of range -> message not found
			return null;
		}

		return messages.get(idx);

	}

	/**
	 * Get the total number of non-deleted messages in the mailbox of the given
	 * user. If the mailbox does not exist, 0 is returned.
	 *
	 * @param username Mailbox owner.
	 * @return Number of messages in the mailbox.
	 */
	int getMessageCount(String username) {
		List<Mailbox.Message> messages = getMessages(username);
		long count = messages.stream()
				.filter(m -> !m.isDeleted()) // ignore deleted messages
				.count();
		return (int) count;
	}

	/**
	 * Get the total size of all non-deleted messages in the mailbox of the
	 * given user. If the mailbox does not exist, 0 is returned.
	 *
	 * @param username Mailbox owner.
	 * @return Size to all messages in the mailbox.
	 */
	int getTotalSize(String username) {
		List<Mailbox.Message> messages = getMessages(username);
		return messages.stream()
				.filter(m -> !m.isDeleted()) // ignore deleted messages
				.mapToInt(Mailbox.Message::getSize)
				.sum();
	}

}
