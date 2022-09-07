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

package net.markwalder.vtestmail.imap;

import java.io.IOException;
import java.util.List;
import net.markwalder.vtestmail.store.MailboxFolder;
import net.markwalder.vtestmail.store.MailboxMessage;

public class EXPUNGE extends ImapCommand {

	public EXPUNGE() {
		// command has no parameters
	}

	public static EXPUNGE parse(String parameters) throws ImapException {
		isNull(parameters);
		return new EXPUNGE();
	}

	@Override
	public String toString() {
		return "EXPUNGE";
	}

	@Override
	protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {

		// see https://datatracker.ietf.org/doc/html/rfc9051#section-6.4.3

		session.assertState(State.Selected);
		session.assertReadWrite();

		// The EXPUNGE command permanently removes all messages that have the \Deleted flag set from the currently selected mailbox.
		// Before returning an OK to the client, an untagged EXPUNGE response is sent for each message that is removed.

		MailboxFolder folder = session.getFolder();
		List<MailboxMessage> messages = folder.getMessages();
		if (!messages.isEmpty()) {

			// send untagged EXPUNGE for each message that is marked as deleted
			int deleted = 0;
			for (int i = 0; i < messages.size(); i++) {
				MailboxMessage message = messages.get(i);
				if (message.isDeleted()) {
					int messageNumber = i + 1 - deleted; // 1-based, decremented for each deleted message
					client.writeLine("* " + messageNumber + " EXPUNGE");
					deleted++;
				}
			}

			if (deleted > 0) {
				// remove all messages marked as deleted
				folder.removeDeletedMessages();
			}

		}

		client.writeLine(tag + " OK EXPUNGE completed");
	}

}
