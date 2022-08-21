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
import java.util.stream.Collectors;
import net.markwalder.junit.mailserver.store.Mailbox;
import net.markwalder.junit.mailserver.utils.Assert;

public class CREATE extends ImapCommand {

	private final String folderName;

	public CREATE(String folderName) {
		Assert.isNotEmpty(folderName, "folderName");
		this.folderName = folderName;
	}

	public static CREATE parse(String parameters) throws ImapException {
		isNotEmpty(parameters);
		// TODO: support quoted mailbox name
		return new CREATE(parameters);
	}

	@Override
	public String toString() {
		return "CREATE " + folderName;
	}

	@Override
	protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {
		session.assertState(State.Authenticated, State.Selected);

		// The CREATE command creates a mailbox with the given name.
		// see https://datatracker.ietf.org/doc/html/rfc9051#section-6.3.4

		// If the mailbox name is suffixed with the server's hierarchy separator
		// character (as returned from the server by a LIST command), this is a
		// declaration that the client intends to create mailbox names under
		// this name in the hierarchy. Server implementations that do not
		// require this declaration MUST ignore the declaration. In any case,
		// the name created is without the trailing hierarchy delimiter.
		String folderName = this.folderName;
		if (folderName.endsWith(HIERARCHY_SEPARATOR)) {
			folderName = folderName.substring(0, folderName.length() - 1);
		}

		// If a client attempts to create a UTF-8 mailbox name that is not a valid Net-Unicode name,
		// the server MUST reject the creation or convert the name to Net-Unicode prior to creating the mailbox.
		// If the server decides to convert (normalize) the name, it SHOULD return an untagged LIST with an OLDNAME extended data item,
		// with the OLDNAME value being the supplied mailbox name and the name parameter being the normalized mailbox name.
		// (See Section 6.3.9.7 for more details.)
		// TODO: implement

		// It is an error to attempt to create INBOX or a mailbox with a name
		// that refers to an extant mailbox. Any error in creation will return
		// a tagged NO response.
		Mailbox mailbox = session.getMailbox();
		if (mailbox.hasFolder(folderName)) {
			throw ImapException.MailboxAlreadyExists();
		}

		// If the server's hierarchy separator character appears elsewhere in
		// the name, the server SHOULD create any superior hierarchical names
		// that are needed for the CREATE command to be successfully completed.
		// In other words, an attempt to create "foo/bar/zap" on a server in
		// which "/" is the hierarchy separator character SHOULD create foo/ and
		// foo/bar/ if they do not already exist.
		String[] folderPathElements = folderName.split(HIERARCHY_SEPARATOR);
		for (int i = 1; i < folderPathElements.length; i++) {
			String parentFolderName = Arrays.stream(folderPathElements).limit(i).collect(Collectors.joining(HIERARCHY_SEPARATOR));
			if (!mailbox.hasFolder(parentFolderName)) {
				mailbox.createFolder(parentFolderName);
			}
		}

		mailbox.createFolder(folderName);

		// If a new mailbox is created with the same name as a mailbox that was
		// deleted, its unique identifiers MUST be greater than any unique
		// identifiers used in the previous incarnation of the mailbox unless
		// the new incarnation has a different unique identifier validity value.
		// See the description of the UID command in Section 6.4.9 for more
		// detail.
		// TODO: implement

		// An OK response is returned only if a new mailbox with that name has
		// been created.
		client.writeLine(tag + " OK CREATE completed");
	}

}
