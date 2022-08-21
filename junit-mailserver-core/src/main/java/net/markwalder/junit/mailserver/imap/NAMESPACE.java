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

public class NAMESPACE extends ImapCommand {

	public NAMESPACE() {
	}

	public static NAMESPACE parse(String parameters) throws ImapException {
		isNull(parameters);
		return new NAMESPACE();
	}

	@Override
	public String toString() {
		return "NAMESPACE";
	}

	@Override
	protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {
		session.assertState(State.Authenticated, State.Selected);

		// see https://datatracker.ietf.org/doc/html/rfc9051#section-6.3.10

		// The NAMESPACE command causes a single untagged NAMESPACE response to
		// be returned.  The untagged NAMESPACE response contains the prefix and
		// hierarchy delimiter to the server's Personal Namespace(s), Other
		// Users' Namespace(s), and Shared Namespace(s) that the server wishes
		// to expose. The response will contain a NIL for any namespace class
		// that is not available. The namespace-response-extensions ABNF
		// non-terminal is defined for extensibility and MAY be included in
		// the NAMESPACE response.

		// This server supports a single Personal Namespace. No leading prefix
		// is used on personal mailboxes, and "/" is the hierarchy delimiter.
		client.writeLine("* NAMESPACE ((\"\" \"" + HIERARCHY_SEPARATOR + "\")) NIL NIL");

		// TODO: support other namespaces

		client.writeLine(tag + " OK NAMESPACE completed");
	}

}
