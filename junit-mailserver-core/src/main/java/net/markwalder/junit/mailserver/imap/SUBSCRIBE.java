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
import net.markwalder.junit.mailserver.store.Mailbox;
import net.markwalder.junit.mailserver.utils.Assert;

public class SUBSCRIBE extends ImapCommand {

	private final String folderName;

	public SUBSCRIBE(String folderName) {
		Assert.isNotEmpty(folderName, "folderName");
		this.folderName = folderName;
	}

	public static SUBSCRIBE parse(String parameters) throws ImapException {
		isNotEmpty(parameters);
		// TODO: support quoted mailbox name
		return new SUBSCRIBE(parameters);
	}

	@Override
	public String toString() {
		return "SUBSCRIBE " + folderName;
	}

	@Override
	protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {
		session.assertState(State.Authenticated, State.Selected);

		// The SUBSCRIBE command adds the specified mailbox name to the server's
		// set of "active" or "subscribed" mailboxes as returned by the LIST
		// (SUBSCRIBED) command.
		// see https://datatracker.ietf.org/doc/html/rfc9051#section-6.3.7

		// A server MAY validate the mailbox argument to SUBSCRIBE to verify
		// that it exists. However, it SHOULD NOT unilaterally remove an
		// existing mailbox name from the subscription list even if a mailbox by
		// that name no longer exists.
		Mailbox mailbox = session.getMailbox();
		if (!mailbox.hasFolder(folderName)) {
			throw ImapException.MailboxNotFound();
		}

		session.subscribe(folderName);

		// This command returns a tagged OK response if the subscription is
		// successful or if the mailbox is already subscribed.
		client.writeLine(tag + " OK SUBSCRIBE completed");
	}

}
