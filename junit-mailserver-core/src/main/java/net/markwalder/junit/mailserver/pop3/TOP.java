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
import net.markwalder.junit.mailserver.Client;
import net.markwalder.junit.mailserver.Mailbox;
import org.apache.commons.lang3.StringUtils;

public class TOP extends Command {

	@Override
	protected void execute(String command, Pop3Server server, Client client) throws IOException, ProtocolException {
		server.assertState(Pop3Server.State.TRANSACTION);

		// try to find message by number, and get top n lines
		String msg = StringUtils.substringBetween(command, "TOP ", " ");
		String n = StringUtils.substringAfterLast(command, " ");
		String lines = getMessageLines(server, msg, n);
		if (lines == null) {
			throw ProtocolException.MessageNotFound();
		}

		client.writeLine("+OK");
		client.writeLine(lines);
		client.writeLine(".");
	}

	private String getMessageLines(Pop3Server server, String msg, String n) {

		Mailbox.Message message = server.getMessage(msg);
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

}
