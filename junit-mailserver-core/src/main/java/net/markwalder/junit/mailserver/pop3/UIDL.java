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
import java.util.List;
import net.markwalder.junit.mailserver.Mailbox;

public class UIDL extends Pop3Command {

	public UIDL() {
		this(null);
	}

	public UIDL(int msg) {
		this(String.valueOf(msg));
	}

	UIDL(String parameters) {
		super(parameters);
	}

	@Override
	protected void execute(Pop3Server server, Pop3Session session, Pop3Client client) throws IOException, ProtocolException {
		session.assertState(State.TRANSACTION);

		if (parameters == null) {

			client.writeLine("+OK");
			List<Mailbox.Message> messages = session.getMessages();
			for (int i = 0; i < messages.size(); i++) {
				Mailbox.Message message = messages.get(i);
				if (message.isDeleted()) {
					continue; // ignore deleted messages
				}

				String msg = String.valueOf(i + 1);
				String uid = message.getUID();
				client.writeLine(msg + " " + uid);
			}
			client.writeLine(".");

		} else {

			// try to find message by number
			Mailbox.Message message = session.getMessage(parameters);
			if (message == null || message.isDeleted()) {
				throw ProtocolException.MessageNotFound();
			}

			String uid = message.getUID();
			client.writeLine("+OK " + parameters + " " + uid);

		}

	}

}
