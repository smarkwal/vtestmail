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

package net.markwalder.junit.mailserver;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Virtual SMTP server for testing.
 * <p>
 * Limitations:
 * <ul>
 *     <li>Only one client can connect to the server at a time.</li>
 *     <li>Support for SSL/TLS sockets and STARTTLS command is not implemented.</li>
 *     <li>The format of messages is not validated.</li>
 *     <li>The mailbox is not exclusively locked by the server.</li>
 * </ul>
 */
public class Pop3Server extends MailServer {

	private enum State {
		AUTHORIZATION,
		TRANSACTION,
		UPDATE
	}

	private State state = null;
	private String timestamp = null;
	private String username = null;

	public Pop3Server(MailboxStore store) {
		super("POP3", store);
	}

	@Override
	protected void reset() {

		// "forget" all state
		state = null;
		timestamp = null;
		username = null;

		super.reset();
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
	protected boolean handleCommand(String command) throws IOException {

		// convert the first word of the command to uppercase
		// (POP3 commands are case-insensitive)
		command = convertToUppercase(command);

		boolean quit = false;
		try {

			if (command.startsWith("APOP ")) {
				handleAPOP(command);
			} else if (command.startsWith("USER ")) {
				handleUSER(command);
			} else if (command.startsWith("PASS ")) {
				handlePASS(command);
			} else if (command.equals("STAT")) {
				handleSTAT();
			} else if (command.equals("LIST")) {
				handleLIST();
			} else if (command.startsWith("LIST ")) {
				handleLIST(command);
			} else if (command.equals("UIDL")) {
				handleUIDL();
			} else if (command.startsWith("UIDL ")) {
				handleUIDL(command);
			} else if (command.startsWith("RETR ")) {
				handleRETR(command);
			} else if (command.startsWith("TOP ")) {
				handleTOP(command);
			} else if (command.startsWith("DELE ")) {
				handleDELE(command);
			} else if (command.equals("NOOP")) {
				handleNOOP();
			} else if (command.equals("RSET")) {
				handleRSET();
			} else if (command.equals("QUIT")) {
				handleQUIT();
				quit = true; // close connection
			} else {
				handleUnknownCommand();
			}

		} catch (MessageNotFoundException e) {
			client.writeLine("-ERR No such message");
		} catch (IllegalStateException e) {
			client.writeLine("-ERR Command is not allowed in " + e.getMessage() + " state");
		} catch (ProtocolException e) {
			client.writeLine("-ERR " + e.getMessage());
		}

		return quit;

	}

	private void handleAPOP(String command) throws IOException {
		assertState(State.AUTHORIZATION);

		String[] parts = StringUtils.split(command, " ");
		if (parts.length != 3) {
			throw new ProtocolException("Invalid APOP command");
		}

		String username = parts[1];
		String digest = parts[2];

		// try to authenticate
		login(username, digest, timestamp);

		if (!isAuthenticated()) {
			client.writeLine("-ERR Authentication failed");
			return;
		}

		// remember username for mailbox access
		this.username = username;

		// enter transaction state
		state = State.TRANSACTION;

		client.writeLine("+OK Authentication successful");
	}

	private void handleUSER(String command) throws IOException {
		assertState(State.AUTHORIZATION);

		// remember username
		username = StringUtils.substringAfter(command, "USER ");
		client.writeLine("+OK User accepted");
	}

	private void handlePASS(String command) throws IOException {
		assertState(State.AUTHORIZATION);

		// get password
		String password = StringUtils.substringAfter(command, "PASS ");

		// try to authenticate
		login(username, password);

		if (!isAuthenticated()) {
			client.writeLine("-ERR Authentication failed");
			return;
		}

		// enter transaction state
		state = State.TRANSACTION;

		client.writeLine("+OK Authentication successful");
	}

	private void handleSTAT() throws IOException {
		assertState(State.TRANSACTION);

		int count = getMessageCount(username);
		int totalSize = getTotalSize(username);
		client.writeLine("+OK " + count + " " + totalSize);
	}

	private void handleLIST() throws IOException {
		assertState(State.TRANSACTION);

		int count = getMessageCount(username);
		int totalSize = getTotalSize(username);
		client.writeLine("+OK " + count + " messages (" + totalSize + " octets)");

		List<Mailbox.Message> messages = getMessages(username);
		for (int i = 0; i < messages.size(); i++) {
			Mailbox.Message message = messages.get(i);
			if (message.isDeleted()) {
				continue; // ignore deleted messages
			}

			String msg = String.valueOf(i + 1);
			int size = message.getSize();
			client.writeLine(msg + " " + size);
		}
		client.writeLine(".");
	}

	private void handleLIST(String command) throws IOException {
		assertState(State.TRANSACTION);

		// try to find message by number
		String msg = StringUtils.substringAfter(command, "LIST ");
		Mailbox.Message message = getMessage(username, msg);
		if (message == null || message.isDeleted()) {
			throw new MessageNotFoundException();
		}

		int size = message.getSize();
		client.writeLine("+OK " + msg + " " + size);
	}

	private void handleUIDL() throws IOException {
		assertState(State.TRANSACTION);

		client.writeLine("+OK");
		List<Mailbox.Message> messages = getMessages(username);
		for (int i = 0; i < messages.size(); i++) {
			Mailbox.Message message = messages.get(i);
			if (message.isDeleted()) {
				continue; // ignore deleted messages
			}

			String msg = String.valueOf(i + 1);
			String uid = message.getUID();
			client.writeLine(msg + " " + uid);
		}
		client.writeLine(".");
	}

	private void handleUIDL(String command) throws IOException {
		assertState(State.TRANSACTION);

		// try to find message by number
		String msg = StringUtils.substringAfter(command, "UIDL ");
		Mailbox.Message message = getMessage(username, msg);
		if (message == null || message.isDeleted()) {
			throw new MessageNotFoundException();
		}

		String uid = message.getUID();
		client.writeLine("+OK " + msg + " " + uid);
	}

	private void handleRETR(String command) throws IOException {
		assertState(State.TRANSACTION);

		// try to find message by number
		String msg = StringUtils.substringAfter(command, "RETR ");
		Mailbox.Message message = getMessage(username, msg);
		if (message == null || message.isDeleted()) {
			throw new MessageNotFoundException();
		}

		int size = message.getSize();
		String content = message.getContent();
		client.writeLine("+OK " + size + " octets");
		client.writeLine(content);
		client.writeLine(".");
	}

	private void handleTOP(String command) throws IOException {
		assertState(State.TRANSACTION);

		// try to find message by number, and get top n lines
		String msg = StringUtils.substringBetween(command, "TOP ", " ");
		String n = StringUtils.substringAfterLast(command, " ");
		String lines = getMessageLines(username, msg, n);
		if (lines == null) {
			throw new MessageNotFoundException();
		}

		client.writeLine("+OK");
		client.writeLine(lines);
		client.writeLine(".");
	}

	private void handleDELE(String command) throws IOException {
		assertState(State.TRANSACTION);

		// try to find message by number
		String msg = StringUtils.substringAfter(command, "DELE ");
		Mailbox.Message message = getMessage(username, msg);
		if (message == null || message.isDeleted()) {
			throw new MessageNotFoundException();
		}

		// mark message as deleted
		message.setDeleted(true);
		client.writeLine("+OK");
	}

	private void handleNOOP() throws IOException {
		assertState(State.TRANSACTION);

		client.writeLine("+OK");
	}

	private void handleRSET() throws IOException {
		assertState(State.TRANSACTION);

		// unmark all messages marked as deleted
		List<Mailbox.Message> messages = getMessages(username);
		for (Mailbox.Message message : messages) {
			if (message.isDeleted()) {
				message.setDeleted(false);
			}
		}

		client.writeLine("+OK");
	}

	private void handleQUIT() throws IOException {

		// enter update state
		state = State.UPDATE;

		// delete messages marked as deleted
		Mailbox mailbox = store.getMailbox(username);
		if (mailbox != null) {
			mailbox.removeDeletedMessages();
		}

		// reset state
		state = null;

		client.writeLine("+OK Goodbye");
	}

	private void handleUnknownCommand() {
		throw new ProtocolException("Unknown command");
	}

	// exceptions --------------------------------------------------------------

	static class Pop3Exception extends RuntimeException {
		public Pop3Exception(String message) {
			super(message);
		}
	}

	static class IllegalStateException extends Pop3Exception {
		public IllegalStateException(String message) {
			super(message);
		}
	}

	static class ProtocolException extends Pop3Exception {
		public ProtocolException(String message) {
			super(message);
		}
	}

	static class MessageNotFoundException extends Pop3Exception {
		public MessageNotFoundException() {
			super(null);
		}
	}

	// private helper methods --------------------------------------------------

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

	/**
	 * Check if session is currently in given state.
	 *
	 * @param expectedState Expected state.
	 */
	private void assertState(State expectedState) {
		if (this.state != expectedState) {
			throw new IllegalStateException(this.state.name());
		}
	}

	private List<Mailbox.Message> getMessages(String username) {
		Mailbox mailbox = store.getMailbox(username);
		if (mailbox == null) {
			// mailbox not found -> return empty list
			return Collections.emptyList();
		}
		return mailbox.getMessages();
	}

	private Mailbox.Message getMessage(String username, String msg) {

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

	private String getMessageLines(String username, String msg, String n) {

		Mailbox.Message message = getMessage(username, msg);
		if (message == null || message.isDeleted()) {
			return null;
		}

		// try to parse parameter "n"
		int lines;
		try {
			lines = Integer.parseInt(n);
		} catch (NumberFormatException e) {
			// not a number -> message not found
			return null;
		}

		return message.getTop(lines);
	}

	/**
	 * Get the total number of non-deleted messages in the mailbox of the given
	 * user. If the mailbox does not exist, 0 is returned.
	 *
	 * @param username Mailbox owner.
	 * @return Number of messages in the mailbox.
	 */
	private int getMessageCount(String username) {
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
	private int getTotalSize(String username) {
		List<Mailbox.Message> messages = getMessages(username);
		return messages.stream()
				.filter(m -> !m.isDeleted()) // ignore deleted messages
				.mapToInt(Mailbox.Message::getSize)
				.sum();
	}

}
