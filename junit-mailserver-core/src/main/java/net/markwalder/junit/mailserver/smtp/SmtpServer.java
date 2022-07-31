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
import java.util.function.Function;
import net.markwalder.junit.mailserver.MailServer;
import net.markwalder.junit.mailserver.MailboxStore;
import net.markwalder.junit.mailserver.utils.StringUtils;

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
public class SmtpServer extends MailServer<SmtpCommand, SmtpSession, SmtpClient> {

	public SmtpServer(MailboxStore store) {
		super("SMTP", store);

		// register available SMTP commands
		addCommand("HELO", HELO::new);
		addCommand("EHLO", EHLO::new);
		addCommand("STARTTLS", STARTTLS::new);
		addCommand("AUTH", AUTH::new);
		addCommand("MAIL", MAIL::new);
		addCommand("RCPT", RCPT::new);
		addCommand("DATA", DATA::new);
		addCommand("NOOP", NOOP::new);
		addCommand("RSET", RSET::new);
		addCommand("QUIT", QUIT::new);
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

		// TODO: try to move some of the following code into MailServer

		// split line into command name and parameters
		String name = StringUtils.substringBefore(line, " ").toUpperCase();
		String parameters = StringUtils.substringAfter(line, " ");

		// try to find command implementation class
		Function<String, SmtpCommand> commandFactory = commands.get(name);
		if (commandFactory == null) {
			client.writeLine("502 5.5.1 Command not implemented");
			return false;
		}

		if (!isCommandEnabled(name)) {
			client.writeLine("502 5.5.1 Disabled command");
			return false;
		}

		// create command instance
		// TODO: validate parameters in factory
		SmtpCommand command = commandFactory.apply(parameters);

		if (command instanceof DATA) {
			// add command to history
			// (before DATA is closing the current transaction)
			session.addCommand(command);
		}

		// execute command
		try {
			command.execute(this, session, client);
		} catch (ProtocolException e) {
			client.writeLine(e.getMessage());
		}

		if (!(command instanceof DATA)) {
			// add command to history
			session.addCommand(command);
		}

		return (command instanceof QUIT);
	}

}
