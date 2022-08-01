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

public class RSET extends Pop3Command {

	public RSET() {
	}

	public static RSET parse(String parameters) throws Pop3Exception {
		if (parameters != null) {
			throw Pop3Exception.SyntaxError();
		}
		return new RSET();
	}

	@Override
	public String toString() {
		return "RSET";
	}

	@Override
	protected void execute(Pop3Server server, Pop3Session session, Pop3Client client) throws IOException, Pop3Exception {
		session.assertState(State.TRANSACTION);

		// unmark all messages marked as deleted
		List<Mailbox.Message> messages = session.getMessages();
		for (Mailbox.Message message : messages) {
			if (message.isDeleted()) {
				message.setDeleted(false);
			}
		}

		client.writeLine("+OK");
	}

}
