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
import net.markwalder.vtestmail.store.MailboxFolder;
import net.markwalder.vtestmail.utils.Assert;
import net.markwalder.vtestmail.utils.StringUtils;

public class SELECT extends ImapCommand {

	protected final String folderName;

	public SELECT(String folderName) {
		Assert.isNotEmpty(folderName, "folderName");
		this.folderName = folderName;
	}

	public static SELECT parse(String parameters) throws ImapException {
		isNotEmpty(parameters);
		ImapCommandParser parser = new ImapCommandParser(parameters);
		String folderName = parser.readMailbox();
		parser.assertNoMoreArguments();
		return new SELECT(folderName);
	}

	@Override
	public String toString() {
		return "SELECT " + folderName;
	}

	@Override
	protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {

		// see https://datatracker.ietf.org/doc/html/rfc9051#section-6.3.2

		select(server, session, client);

		// TODO: are there other ways to enable read-only mode?
		session.setReadOnly(false);

		if (session.isReadOnly()) {
			client.writeLine(tag + " OK [READ-ONLY] SELECT completed");
		} else {
			client.writeLine(tag + " OK [READ-WRITE] SELECT completed");
		}
	}

	protected void select(ImapServer server, ImapSession session, ImapClient client) throws IOException, ImapException {

		// Only one mailbox can be selected at a time in a connection;
		// simultaneous access to multiple mailboxes requires multiple connections.
		// The SELECT command automatically deselects any currently selected mailbox before attempting the new selection.
		// Consequently, if a mailbox is selected and a SELECT command that fails is attempted, no mailbox is selected.
		// When deselecting a selected mailbox, the server MUST return an untagged OK response
		// with the "[CLOSED]" response code when the currently selected mailbox is closed (see Section 7.1).
		if (session.getState() == State.Selected) {
			session.unselectFolder();
			client.writeLine("* OK [CLOSED] Previous mailbox is now closed");
		}

		session.assertState(State.Authenticated);

		// The case-insensitive mailbox name INBOX is a special name reserved to
		// mean "the primary mailbox for this user on this server".
		MailboxFolder folder = session.selectFolder(folderName);

		// The number of messages in the mailbox.
		// See the description of the EXISTS response in Section 7.4.1 for more detail.
		client.writeLine("* " + folder.getMessages().size() + " EXISTS");

		// The unique identifier validity value.
		// Refer to Section 2.3.1.1 for more information.
		client.writeLine("* OK [UIDVALIDITY " + folder.getUIDValidity() + "] UIDs valid");

		// The next unique identifier value.
		// Refer to Section 2.3.1.1 for more information.
		client.writeLine("* OK [UIDNEXT " + folder.getUIDNext() + "] Predicted next UID");

		// Defined flags in the mailbox.
		// The FLAGS response occurs as a result of a SELECT or EXAMINE command.
		// The flag parenthesized list identifies the flags (at a minimum, the
		// system-defined flags) that are applicable for this mailbox.  Flags
		// other than the system flags can also exist, depending on server
		// implementation.
		client.writeLine("* FLAGS (" + StringUtils.join(server.getFlags(), " ") + ")");

		// A list of message flags that the client can change permanently.
		// If this is missing, the client should assume that all flags can be changed permanently.
		// Any flags that are in the FLAGS untagged response, but not in the PERMANENTFLAGS
		// list, cannot be set permanently.  The PERMANENTFLAGS list can also
		// include the special flag \*, which indicates that it is possible
		// to create new keywords by attempting to store those keywords in
		// the mailbox.
		client.writeLine("* OK [PERMANENTFLAGS (" + StringUtils.join(server.getPermanentFlags(), " ") + " \\*)] Limited");

		// The server MUST return a LIST response with the mailbox name.
		// The list of mailbox attributes MUST be accurate.
		// If the server allows denormalized UTF-8 mailbox names (see Section 5.1)
		// and the supplied mailbox name differs from the normalized version,
		// the server MUST return LIST with the OLDNAME extended data item.
		// See Section 6.3.9.7 for more details.
		client.writeLine("* LIST () \"" + HIERARCHY_SEPARATOR + "\" " + folder.getName()); // TODO: implement LIST response

	}

}
