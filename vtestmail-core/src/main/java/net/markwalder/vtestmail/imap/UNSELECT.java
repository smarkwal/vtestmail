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

public class UNSELECT extends ImapCommand {

	public UNSELECT() {
	}

	public static UNSELECT parse(String parameters) throws ImapException {
		isNull(parameters);
		return new UNSELECT();
	}

	@Override
	public String toString() {
		return "UNSELECT";
	}

	@Override
	protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {

		// see https://datatracker.ietf.org/doc/html/rfc9051#section-6.4.2

		session.assertState(State.Selected);

		// The UNSELECT command frees a session's resources associated with the
		// selected mailbox and returns the server to the authenticated state.
		// This command performs the same actions as CLOSE, except that no
		// messages are permanently removed from the currently selected mailbox.
		session.unselectFolder();

		client.writeLine(tag + " OK UNSELECT completed");
	}

}
