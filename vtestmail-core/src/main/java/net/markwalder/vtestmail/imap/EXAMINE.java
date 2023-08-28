/*
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

package net.markwalder.vtestmail.imap;

import java.io.IOException;

public class EXAMINE extends SELECT {

	public EXAMINE(String folderName) {
		super(folderName);
	}

	public static EXAMINE parse(String parameters) throws ImapException {
		isNotEmpty(parameters);
		ImapCommandParser parser = new ImapCommandParser(parameters);
		String folderName = parser.readMailbox();
		parser.assertNoMoreArguments();
		return new EXAMINE(folderName);
	}

	@Override
	public String toString() {
		return "EXAMINE " + folderName;
	}

	@Override
	protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {

		// see https://datatracker.ietf.org/doc/html/rfc9051#section-6.3.3

		// The EXAMINE command is identical to SELECT and returns the same output; however, the selected mailbox is identified as read-only.
		// No changes to the permanent state of the mailbox, including per-user state, are permitted.

		select(server, session, client);

		session.setReadOnly(true);

		client.writeLine(tag + " OK [READ-ONLY] EXAMINE completed");
	}

}
