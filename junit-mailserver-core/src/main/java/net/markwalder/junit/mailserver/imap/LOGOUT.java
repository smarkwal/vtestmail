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

public class LOGOUT extends ImapCommand {

	public LOGOUT() {
	}

	public static LOGOUT parse(String parameters) throws ImapException {
		isNull(parameters);
		return new LOGOUT();
	}

	@Override
	public String toString() {
		return "LOGOUT";
	}

	@Override
	protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {

		// TODO: are there any cleanup actions to be done?

		session.logout();

		// set "closed" flag in session
		session.close();

		client.writeLine("* BYE IMAP4rev2 Server logging out");
		client.writeLine(tag + " OK LOGOUT completed");
	}

}
