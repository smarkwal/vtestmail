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
import net.markwalder.vtestmail.store.Mailbox;
import net.markwalder.vtestmail.utils.Assert;

public class DELETE extends ImapCommand {

	private final String folderName;

	public DELETE(String folderName) {
		Assert.isNotEmpty(folderName, "folderName");
		this.folderName = folderName;
	}

	public static DELETE parse(String parameters) throws ImapException {
		isNotEmpty(parameters);
		ImapCommandParser parser = new ImapCommandParser(parameters);
		String folderName = parser.readMailbox();
		parser.assertNoMoreArguments();
		return new DELETE(folderName);
	}

	@Override
	public String toString() {
		return "DELETE " + folderName;
	}

	@Override
	protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {
		session.assertState(State.Authenticated, State.Selected);

		// The DELETE command permanently removes the mailbox with the given name.
		// see https://datatracker.ietf.org/doc/html/rfc9051#section-6.3.5

		// If the server decides to convert (normalize) the mailbox name, it
		// SHOULD return an untagged LIST with the "\NonExistent" attribute and
		// OLDNAME extended data item, with the OLDNAME value being the supplied
		// mailbox name and the name parameter being the normalized mailbox
		// name.  (See Section 6.3.9.7 for more details.)
		// TODO: implement

		// TODO: can the selected mailbox be deleted?

		// It is an error to attempt to delete INBOX or a mailbox name that does not exist.
		Mailbox mailbox = session.getMailbox();
		if (folderName.equals("INBOX")) {
			throw ImapException.MailboxNotDeleted();
		} else if (!mailbox.hasFolder(folderName)) {
			throw ImapException.MailboxNotFound();
		}

		// The DELETE command MUST NOT remove inferior hierarchical names.  For
		// example, if a mailbox "foo" has an inferior "foo.bar" (assuming "."
		// is the hierarchy delimiter character), removing "foo" MUST NOT remove
		// "foo.bar". It is an error to attempt to delete a name that has
		// inferior hierarchical names and also has the \Noselect mailbox name
		// attribute (see the description of the LIST response (Section 7.3.1)
		// for more details).
		// TODO: implement
		List<String> folderNames = mailbox.getFolderNames();
		for (String name : folderNames) {
			if (name.startsWith(folderName + HIERARCHY_SEPARATOR)) {
				throw ImapException.MailboxHasChildren();
			}
		}

		// It is permitted to delete a name that has inferior hierarchical names
		// and does not have the \Noselect mailbox name attribute.  If the
		// server implementation does not permit deleting the name while
		// inferior hierarchical names exist, then it SHOULD disallow the DELETE
		// command by returning a tagged NO response. The NO response SHOULD
		// include the HASCHILDREN response code. Alternatively, the server MAY
		// allow the DELETE command, but it sets the \Noselect mailbox name
		// attribute for that name.
		// TODO: implement

		// The value of the highest-used unique identifier of the deleted
		// mailbox MUST be preserved so that a new mailbox created with the same
		// name will not reuse the identifiers of the former incarnation, unless
		// the new incarnation has a different unique identifier validity value.
		// See the description of the UID command in Section 6.4.9 for more
		// detail.
		// TODO: implement

		// Mailboxes deleted in one IMAP session MAY be announced to other IMAP
		// sessions using an unsolicited LIST response, containing the
		// "\NonExistent" attribute.
		// TODO: implement

		mailbox.deleteFolder(folderName);

		// A tagged OK response is returned only if the mailbox has been deleted.
		// If the server returns an OK response, all messages in that mailbox
		// are removed by the DELETE command.
		client.writeLine(tag + " OK DELETE completed");
	}

}
