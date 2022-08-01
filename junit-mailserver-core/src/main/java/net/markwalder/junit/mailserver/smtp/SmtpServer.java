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
import net.markwalder.junit.mailserver.MailCommand;
import net.markwalder.junit.mailserver.MailException;
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
public class SmtpServer extends MailServer<SmtpCommand, SmtpSession, SmtpClient, SmtpException> {

	public SmtpServer(MailboxStore store) {
		super("SMTP", store);

		// register available SMTP commands
		addCommand("HELO", HELO::parse);
		addCommand("EHLO", EHLO::parse);
		addCommand("STARTTLS", STARTTLS::parse);
		addCommand("AUTH", AUTH::parse);
		addCommand("MAIL", MAIL::parse);
		addCommand("RCPT", RCPT::parse);
		addCommand("DATA", DATA::parse);
		addCommand("NOOP", NOOP::parse);
		addCommand("RSET", RSET::parse);
		addCommand("QUIT", QUIT::parse);
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
	protected void handleCommand(String line) throws IOException {

		// TODO: try to move some of the following code into MailServer

		// TODO: use an "exception handler" with try/catch over all of the following code

		// split line into command name and parameters
		String name = StringUtils.substringBefore(line, " ").toUpperCase();
		String parameters = StringUtils.substringAfter(line, " ");

		// try to find command implementation class
		MailCommand.Parser<SmtpCommand, SmtpException> commandFactory = commands.get(name);
		if (commandFactory == null) {
			client.writeLine("502 5.5.1 Command not implemented");
			return;
		}

		if (!isCommandEnabled(name)) {
			client.writeLine("502 5.5.1 Disabled command");
			return;
		}

		// create command instance
		SmtpCommand command;
		try {
			command = commandFactory.parse(parameters);
		} catch (MailException e) {
			client.writeLine(e.getMessage());
			return;
		}

		if (command instanceof DATA) {
			// add command to history
			// (before DATA is closing the current transaction)
			session.addCommand(command);
		}

		// execute command
		try {
			command.execute(this, session, client);
		} catch (SmtpException e) {
			client.writeLine(e.getMessage());
		}

		if (!(command instanceof DATA)) {
			// add command to history
			session.addCommand(command);
		}
	}

}
