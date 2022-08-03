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
import net.markwalder.junit.mailserver.MailCommand;
import net.markwalder.junit.mailserver.MailServer;
import net.markwalder.junit.mailserver.MailboxStore;
import net.markwalder.junit.mailserver.utils.StringUtils;

/**
 * Virtual POP3 server for testing.
 * <p>
 * Limitations:
 * <ul>
 *     <li>Only one client can connect to the server at a time.</li>
 *     <li>Support for STARTTLS command is not implemented.</li>
 *     <li>The format of messages is not validated.</li>
 *     <li>The mailbox is not exclusively locked by the server.</li>
 * </ul>
 */
public class Pop3Server extends MailServer<Pop3Command, Pop3Session, Pop3Client, Pop3Exception> {

	public Pop3Server(MailboxStore store) {
		super("POP3", store);

		// register available POP3 commands
		addCommand("CAPA", CAPA::parse);
		addCommand("AUTH", AUTH::parse);
		addCommand("APOP", APOP::parse);
		addCommand("USER", USER::parse);
		addCommand("PASS", PASS::parse);
		addCommand("STAT", STAT::parse);
		addCommand("LIST", LIST::parse);
		addCommand("UIDL", UIDL::parse);
		addCommand("RETR", RETR::parse);
		addCommand("DELE", DELE::parse);
		addCommand("TOP", TOP::parse);
		addCommand("NOOP", NOOP::parse);
		addCommand("RSET", RSET::parse);
		addCommand("QUIT", QUIT::parse);
		// TODO: implement RFC 6856: UTF8 and LANG (https://www.rfc-editor.org/rfc/rfc6856)
	}

	@Override
	protected Pop3Client createClient(Socket socket, StringBuilder log) throws IOException {
		return new Pop3Client(socket, log);
	}

	@Override
	protected Pop3Session createSession() {

		// generate timestamp for APOP authentication
		String timestamp = "<" + clock.millis() + "@localhost>";

		return new Pop3Session(timestamp);
	}

	@Override
	protected void handleNewClient() throws IOException {
		String timestamp = session.getTimestamp();
		client.writeLine("+OK POP3 server ready " + timestamp);
	}

	@Override
	protected void handleCommand(String line) throws Pop3Exception, IOException {

		// TODO: try to move some of the following code into MailServer

		// TODO: use an "exception handler" with try/catch over all of the following code

		Pop3Command command = createCommand(line);

		// add command to history
		session.addCommand(command);

		// execute command
		command.execute(this, session, client);

	}

	protected Pop3Command createCommand(String line) throws Pop3Exception {

		// split line into command name and parameters
		String name = StringUtils.substringBefore(line, " ").toUpperCase();
		String parameters = StringUtils.substringAfter(line, " ");

		// check if command is supported
		if (!isCommandEnabled(name)) {
			throw Pop3Exception.CommandNotImplemented();
		}

		// create command instance
		MailCommand.Parser<Pop3Command, Pop3Exception> commandFactory = commands.get(name);
		return commandFactory.parse(parameters);
	}

}
