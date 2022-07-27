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
import java.util.HashMap;
import java.util.Map;
import net.markwalder.junit.mailserver.MailServer;
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
public class Pop3Server extends MailServer<Pop3Session, Pop3Client> {

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
		// TODO: implement RFC 6856: UTF8 and LANG (https://www.rfc-editor.org/rfc/rfc6856)
	}

	private final Map<String, Boolean> enabledCommands = new HashMap<>();

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

	@Override
	protected Pop3Client createClient(Socket socket, StringBuilder log) throws IOException {
		return new Pop3Client(socket, log);
	}

	@Override
	protected Pop3Session createSession() {
		return new Pop3Session();
	}

	@Override
	protected void handleNewClient() throws IOException {
		String timestamp = session.getTimestamp();
		client.writeLine("+OK POP3 server ready " + timestamp);
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
			command.execute(line, this, session, client);
		} catch (ProtocolException e) {
			client.writeLine("-ERR " + e.getMessage());
		}

		return (command instanceof QUIT);
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

}
