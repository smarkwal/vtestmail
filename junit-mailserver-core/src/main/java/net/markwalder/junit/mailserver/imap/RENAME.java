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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.markwalder.junit.mailserver.store.Mailbox;
import net.markwalder.junit.mailserver.utils.Assert;
import net.markwalder.junit.mailserver.utils.StringUtils;

public class RENAME extends ImapCommand {

	private final String oldFolderName;
	private final String newFolderName;

	public RENAME(String oldFolderName, String newFolderName) {
		Assert.isNotEmpty(oldFolderName, "oldFolderName");
		Assert.isNotEmpty(newFolderName, "newFolderName");
		this.oldFolderName = oldFolderName;
		this.newFolderName = newFolderName;
	}

	public static RENAME parse(String parameters) throws ImapException {
		isNotEmpty(parameters);
		// TODO: support quoted mailbox names
		String[] parts = StringUtils.split(parameters, 2);
		String oldFolderName = parts[0];
		String newFolderName = parts[1];
		return new RENAME(oldFolderName, newFolderName);
	}

	@Override
	public String toString() {
		return "RENAME " + oldFolderName + " " + newFolderName;
	}

	@Override
	protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {
		session.assertState(State.Authenticated, State.Selected);

		// The RENAME command changes the name of a mailbox.
		// see https://datatracker.ietf.org/doc/html/rfc9051#section-6.3.6

		// If the server allows creation of mailboxes with names that are not
		// valid Net-Unicode names, the server normalizes both the existing
		// mailbox name parameter and the new mailbox name parameter. If the
		// normalized version of any of these 2 parameters differs from the
		// corresponding supplied version, the server SHOULD return an untagged
		// LIST response with an OLDNAME extended data item, with the OLDNAME
		// value being the supplied existing mailbox name and the name parameter
		// being the normalized new mailbox name (see Section 6.3.9.7). This
		// would allow the client to correlate the supplied name with the
		// normalized name.
		// TODO: implement

		// TODO: can the selected mailbox be renamed?

		// It is an error to attempt to rename from a mailbox name that does
		// not exist or to a mailbox name that already exists. Any error in
		// renaming will return a tagged NO response.
		Mailbox mailbox = session.getMailbox();
		if (!mailbox.hasFolder(oldFolderName)) {
			throw ImapException.MailboxNotFound();
		} else if (mailbox.hasFolder(newFolderName)) {
			throw ImapException.MailboxAlreadyExists();
		}

		// If the name has inferior hierarchical names, then the inferior
		// hierarchical names MUST also be renamed. For example, a rename of
		// "foo" to "zap" will rename "foo/bar" (assuming "/" is the hierarchy
		// delimiter character) to "zap/bar".
		List<String> folderNames = mailbox.getFolderNames();
		for (String folderName : folderNames) {
			if (folderName.startsWith(oldFolderName + HIERARCHY_SEPARATOR)) {
				String newFolderName = this.newFolderName + folderName.substring(oldFolderName.length());
				mailbox.renameFolder(folderName, newFolderName);
			}
		}

		// Renaming INBOX is permitted and does not result in a tagged BAD
		// response, and it has special behavior: It moves all messages in INBOX
		// to a new mailbox with the given name, leaving INBOX empty.  If the
		// server implementation supports inferior hierarchical names of INBOX,
		// these are unaffected by a rename of INBOX. (Note that some servers
		// disallow renaming INBOX by returning a tagged NO response, so clients
		// need to be able to handle the failure of such RENAME commands.)
		// TODO: implement

		// If the server's hierarchy separator character appears in the new
		// mailbox name, the server SHOULD create any superior hierarchical
		// names that are needed for the RENAME command to complete
		// successfully. In other words, an attempt to rename "foo/bar/zap" to
		// "baz/rag/zowie" on a server in which "/" is the hierarchy separator
		// character in the corresponding namespace SHOULD create "baz/" and
		// "baz/rag/" if they do not already exist.
		String[] folderPathElements = newFolderName.split(HIERARCHY_SEPARATOR);
		for (int i = 1; i < folderPathElements.length; i++) {
			String parentFolderName = Arrays.stream(folderPathElements).limit(i).collect(Collectors.joining(HIERARCHY_SEPARATOR));
			if (!mailbox.hasFolder(parentFolderName)) {
				mailbox.createFolder(parentFolderName);
			}
		}

		mailbox.renameFolder(oldFolderName, newFolderName);

		// The value of the highest-used unique identifier of the old mailbox
		// name MUST be preserved so that a new mailbox created with the same
		// name will not reuse the identifiers of the former incarnation, unless
		// the new incarnation has a different unique identifier validity value.
		// See the description of the UID command in Section 6.4.9 for more
		// detail.
		// TODO: implement

		// Mailboxes renamed in one IMAP session MAY be announced to other IMAP
		// sessions using an unsolicited LIST response with an OLDNAME extended
		// data item.
		// TODO: implement

		// In both of the above cases, if the server automatically subscribes a
		// mailbox when it is renamed, then the unsolicited LIST response for
		// each affected subscribed mailbox name MUST include the \Subscribed
		// attribute. No unsolicited LIST responses need to be sent for child
		// mailboxes. When INBOX is successfully renamed, it is assumed that a
		// new INBOX is created. No unsolicited LIST responses need to be sent
		// for INBOX in this case.
		// TODO: implement

		// A tagged OK response is returned only if the mailbox has been renamed.
		client.writeLine(tag + " OK RENAME completed");
	}

}
