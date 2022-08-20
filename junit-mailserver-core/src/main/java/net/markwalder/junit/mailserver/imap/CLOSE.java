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

package net.markwalder.junit.mailserver.imap;

import java.io.IOException;
import net.markwalder.junit.mailserver.Mailbox;

public class CLOSE extends ImapCommand {

	public CLOSE() {
	}

	public static CLOSE parse(String parameters) throws ImapException {
		isNull(parameters);
		return new CLOSE();
	}

	@Override
	public String toString() {
		return "CLOSE";
	}

	@Override
	protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {

		// see https://datatracker.ietf.org/doc/html/rfc9051#section-6.4.1

		session.assertState(State.Selected);

		// No messages are removed, and no error is given, if the mailbox is
		// selected by an EXAMINE command or is otherwise selected as read-only.
		if (!session.isReadOnly()) {

			// The CLOSE command permanently removes all messages that have the
			// \Deleted flag set from the currently selected mailbox, and it returns
			// to the authenticated state from the selected state. No untagged
			// EXPUNGE responses are sent.
			Mailbox.Folder folder = session.getFolder();
			folder.removeDeletedMessages();

		}

		session.unselectFolder();

		client.writeLine(tag + " OK CLOSE completed");
	}

}
