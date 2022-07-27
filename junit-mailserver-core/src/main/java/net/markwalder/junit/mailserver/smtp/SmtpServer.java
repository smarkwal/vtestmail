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

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Locale;
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
 *     <li>The format of email addresses and messages is not validated.</li>
 *     <li>Server does not add a "Received" header to messages.</li>
 *     <li>Messages are not queued or relayed to another SMTP server.</li>
 *     <li>Messages are either delivered to a local mailbox, or silently discarded.</li>
 * </ul>
 */
public class SmtpServer extends MailServer<SmtpSession, SmtpClient> {

	private static final Map<String, Command> commands = new HashMap<>();

	static {
		commands.put("HELO", new HELO());
		commands.put("EHLO", new EHLO());
		commands.put("STARTTLS", new STARTTLS());
		commands.put("AUTH", new AUTH());
		commands.put("MAIL", new MAIL());
		commands.put("RCPT", new RCPT());
		commands.put("DATA", new DATA());
		commands.put("NOOP", new NOOP());
		commands.put("RSET", new RSET());
		commands.put("QUIT", new QUIT());
	}

	public SmtpServer(MailboxStore store) {
		super("SMTP", store);
	}

	@Override
	protected SmtpClient createClient(Socket socket, StringBuilder log) throws IOException {
		return new SmtpClient(socket, log);
	}

	@Override
	protected SmtpSession createSession() {
		return new SmtpSession();
	}

	@Override
	protected void handleNewClient() throws IOException {
		client.writeLine("220 localhost Service ready");
	}

	@Override
	protected boolean handleCommand(String line) throws IOException {

		String name = StringUtils.substringBefore(line, " ").toUpperCase(Locale.ROOT);
		Command command = commands.get(name);
		if (command == null) {
			client.writeLine("502 5.5.1 Command not implemented");
			return false;
		}

		try {
			command.execute(line, this, session, client);
		} catch (ProtocolException e) {
			client.writeLine(e.getMessage());
		}

		return (command instanceof QUIT);
	}

}
