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
import java.util.List;
import net.markwalder.vtestmail.store.MailboxMessage;
import net.markwalder.vtestmail.utils.Assert;

public class UIDL extends Pop3Command {

	private final int messageNumber;

	public UIDL() {
		this.messageNumber = -1;
	}

	public UIDL(int messageNumber) {
		Assert.isInRange(messageNumber, 1, Integer.MAX_VALUE, "messageNumber");
		this.messageNumber = messageNumber;
	}

	public static UIDL parse(String parameters) throws Pop3Exception {
		if (parameters == null) {
			return new UIDL();
		} else {
			int messageNumber = parseMessageNumber(parameters);
			return new UIDL(messageNumber);
		}
	}

	@Override
	public String toString() {
		if (messageNumber < 0) {
			return "UIDL";
		} else {
			return "UIDL " + messageNumber;
		}
	}

	@Override
	protected void execute(Pop3Server server, Pop3Session session, Pop3Client client) throws IOException, Pop3Exception {
		session.assertState(State.TRANSACTION);

		if (messageNumber < 0) {

			client.writeLine("+OK");
			List<MailboxMessage> messages = session.getMessages();
			for (int i = 0; i < messages.size(); i++) {
				MailboxMessage message = messages.get(i);
				if (message.isDeleted() || session.isDeleted(i + 1)) {
					continue; // ignore deleted messages
				}

				String msg = String.valueOf(i + 1);
				int uid = message.getUID();
				client.writeLine(msg + " " + toHexUID(uid));
			}
			client.writeLine(".");

		} else {

			// try to find message by number
			MailboxMessage message = session.getMessage(messageNumber);
			if (message == null || message.isDeleted() || session.isDeleted(messageNumber)) {
				throw Pop3Exception.MessageNotFound();
			}

			int uid = message.getUID();
			client.writeLine("+OK " + messageNumber + " " + toHexUID(uid));

		}

	}

	private static String toHexUID(int uid) {
		return String.format("%1$8x", uid).replace(' ', '0');
	}

}
