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

package net.markwalder.vtestmail.pop3;

import java.io.IOException;
import net.markwalder.vtestmail.store.MailboxMessage;
import net.markwalder.vtestmail.utils.Assert;
import net.markwalder.vtestmail.utils.StringUtils;

public class TOP extends Pop3Command {

	private final int messageNumber;
	private final int lines;

	public TOP(int messageNumber, int lines) {
		Assert.isInRange(messageNumber, 1, Integer.MAX_VALUE, "messageNumber");
		Assert.isInRange(lines, 0, Integer.MAX_VALUE, "lines");
		this.messageNumber = messageNumber;
		this.lines = lines;
	}

	public static TOP parse(String parameters) throws Pop3Exception {
		isNotEmpty(parameters);
		String msg = StringUtils.substringBefore(parameters, " ");
		String n = StringUtils.substringAfter(parameters, " ");
		int messageNumber = parseMessageNumber(msg);
		int lines = parseNumber(n, 0);
		return new TOP(messageNumber, lines);
	}

	@Override
	public String toString() {
		return "TOP " + messageNumber + " " + lines;
	}

	@Override
	protected void execute(Pop3Server server, Pop3Session session, Pop3Client client) throws IOException, Pop3Exception {
		session.assertState(State.TRANSACTION);

		// try to find message by number, and get top n lines
		MailboxMessage message = session.getMessage(messageNumber);
		if (message == null || message.isDeleted() || session.isDeleted(messageNumber)) {
			throw Pop3Exception.MessageNotFound();
		}
		String reply = message.getTop(lines);

		client.writeLine("+OK");
		client.writeMultiLines(reply);
	}

}
