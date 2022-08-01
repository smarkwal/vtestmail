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
import net.markwalder.junit.mailserver.utils.Assert;

public class DELE extends Pop3Command {

	private final int messageNumber;

	public DELE(int messageNumber) {
		Assert.isInRange(messageNumber, 1, Integer.MAX_VALUE, "messageNumber");
		this.messageNumber = messageNumber;
	}

	public static DELE parse(String parameters) throws Pop3Exception {
		isNotEmpty(parameters);
		int messageNumber = parseMessageNumber(parameters);
		return new DELE(messageNumber);
	}

	@Override
	public String toString() {
		return "DELE " + messageNumber;
	}

	@Override
	protected void execute(Pop3Server server, Pop3Session session, Pop3Client client) throws IOException, Pop3Exception {
		session.assertState(State.TRANSACTION);

		// try to find message by number
		Mailbox.Message message = session.getMessage(messageNumber);
		if (message == null || message.isDeleted()) {
			throw Pop3Exception.MessageNotFound();
		}

		// mark message as deleted
		message.setDeleted(true);
		client.writeLine("+OK");
	}

}
