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
import net.markwalder.junit.mailserver.utils.Assert;

public class UNSUBSCRIBE extends ImapCommand {

	private final String folderName;

	public UNSUBSCRIBE(String folderName) {
		Assert.isNotEmpty(folderName, "folderName");
		this.folderName = folderName;
	}

	public static UNSUBSCRIBE parse(String parameters) throws ImapException {
		isNotEmpty(parameters);
		// TODO: support quoted mailbox name
		return new UNSUBSCRIBE(parameters);
	}

	@Override
	public String toString() {
		return "UNSUBSCRIBE " + folderName;
	}

	@Override
	protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {
		session.assertState(State.Authenticated, State.Selected);

		// The UNSUBSCRIBE command removes the specified mailbox name from the
		// server's set of "active" or "subscribed" mailboxes as returned by the
		// LIST (SUBSCRIBED) command.
		// see https://datatracker.ietf.org/doc/html/rfc9051#section-6.3.8

		session.unsubscribe(folderName);

		// This command returns a tagged OK response if the unsubscription is
		// successful or if the mailbox is not subscribed.
		client.writeLine(tag + " OK UNSUBSCRIBE completed");
	}

}
