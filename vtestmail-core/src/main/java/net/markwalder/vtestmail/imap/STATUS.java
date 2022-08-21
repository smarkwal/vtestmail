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
import java.util.ArrayList;
import java.util.List;
import net.markwalder.vtestmail.store.Mailbox;
import net.markwalder.vtestmail.store.MailboxFolder;
import net.markwalder.vtestmail.store.MailboxMessage;
import net.markwalder.vtestmail.utils.Assert;
import net.markwalder.vtestmail.utils.StringUtils;

public class STATUS extends ImapCommand {

	private final String folderName;
	private final String[] statusDataItemNames;

	public STATUS(String folderName, String... statusDataItemNames) {
		Assert.isNotEmpty(folderName, "folderName");
		this.folderName = folderName;
		this.statusDataItemNames = statusDataItemNames;
	}

	public static STATUS parse(String parameters) throws ImapException {
		isNotEmpty(parameters);
		// TODO: support quoted mailbox name
		String[] parts = StringUtils.split(parameters, 2);
		String folderName = parts[0];
		String items = parts[1];
		if (!items.startsWith("(") || !items.endsWith(")")) {
			throw ImapException.SyntaxError();
		}
		String[] statusDataItemNames = StringUtils.split(items.substring(1, items.length() - 1), Integer.MAX_VALUE);
		return new STATUS(folderName, statusDataItemNames);
	}

	@Override
	public String toString() {
		return "STATUS " + folderName;
	}

	@Override
	protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {
		session.assertState(State.Authenticated, State.Selected);

		// The STATUS command requests the status of the indicated mailbox.  It
		// does not change the currently selected mailbox, nor does it affect
		// the state of any messages in the queried mailbox.
		// see https://datatracker.ietf.org/doc/html/rfc9051#section-6.3.11

		Mailbox mailbox = session.getMailbox();
		if (!mailbox.hasFolder(folderName)) {
			throw ImapException.MailboxNotFound();
		}

		MailboxFolder folder = mailbox.getFolder(folderName);

		List<String> statusDataItems = new ArrayList<>(statusDataItemNames.length);

		for (String statusDataItemName : statusDataItemNames) {
			switch (statusDataItemName) {

				case "MESSAGES":
					// The number of messages in the mailbox.
					int count = folder.getMessages().size();
					statusDataItems.add(statusDataItemName + " " + count);
					break;

				case "UIDNEXT":
					// The next unique identifier value of the mailbox.
					// Refer to Section 2.3.1.1 for more information.
					int uidNext = folder.getUIDNext();
					statusDataItems.add(statusDataItemName + " " + uidNext);
					break;

				case "UIDVALIDITY":
					// The unique identifier validity value of the mailbox.
					// Refer to Section 2.3.1.1 for more information.
					int uidValidity = folder.getUIDValidity();
					statusDataItems.add(statusDataItemName + " " + uidValidity);
					break;

				case "UNSEEN":
					// The number of messages that do not have the \Seen flag set.
					long unseen = folder.getMessages().stream()
							.filter(message -> !message.isSeen())
							.count();
					statusDataItems.add(statusDataItemName + " " + unseen);
					break;

				case "DELETED":
					// The number of messages that have the \Deleted flag set.
					long deleted = folder.getMessages().stream()
							.filter(MailboxMessage::isDeleted)
							.count();
					statusDataItems.add(statusDataItemName + " " + deleted);
					break;

				case "SIZE":
					// The total size of the mailbox in octets.  This is not strictly
					// required to be an exact value, but it MUST be equal to or greater
					// than the sum of the values of the RFC822.SIZE FETCH message data
					// items (see Section 6.4.5) of all messages in the mailbox.
					long size = folder.getMessages().stream()
							.mapToLong(MailboxMessage::getSize)
							.sum();
					statusDataItems.add(statusDataItemName + " " + size);
					break;

				default:
					// TODO: is this correct?
					throw ImapException.SyntaxError();
			}
		}
		client.writeLine("* STATUS " + folderName + " (" + StringUtils.join(statusDataItems, " ") + ")");

		client.writeLine(tag + " OK STATUS completed");
	}

}
