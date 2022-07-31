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
import net.markwalder.junit.mailserver.Mailbox;
import net.markwalder.junit.mailserver.utils.StringUtils;

public class TOP extends Pop3Command {

	public TOP(int msg, int n) {
		this(msg + " " + n);
	}

	TOP(String parameters) {
		super(parameters);
	}

	@Override
	protected void execute(Pop3Server server, Pop3Session session, Pop3Client client) throws IOException, ProtocolException {
		session.assertState(State.TRANSACTION);

		// try to find message by number, and get top n lines
		String msg = StringUtils.substringBefore(parameters, " ");
		String n = StringUtils.substringAfter(parameters, " ");
		String lines = getMessageLines(session, msg, n);
		if (lines == null) {
			throw ProtocolException.MessageNotFound();
		}

		client.writeLine("+OK");
		client.writeLine(lines);
		client.writeLine(".");
	}

	private String getMessageLines(Pop3Session session, String msg, String n) {

		Mailbox.Message message = session.getMessage(msg);
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
