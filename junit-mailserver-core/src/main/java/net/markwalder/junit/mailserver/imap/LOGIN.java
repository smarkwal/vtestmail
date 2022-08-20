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
import java.util.List;
import net.markwalder.junit.mailserver.utils.Assert;
import net.markwalder.junit.mailserver.utils.StringUtils;

public class LOGIN extends ImapCommand {

	private final String username;
	private final String password;

	public LOGIN(String username, String password) {
		Assert.isNotEmpty(username, "username");
		Assert.isNotEmpty(password, "password");
		this.username = username;
		this.password = password;
	}

	public static LOGIN parse(String parameters) throws ImapException {
		isNotEmpty(parameters);
		String username = StringUtils.substringBefore(parameters, " ");
		String digest = StringUtils.substringAfter(parameters, " ");
		isNotEmpty(username);
		isNotEmpty(digest);
		return new LOGIN(username, digest);
	}

	@Override
	public String toString() {
		return "LOGIN " + username + " " + password;
	}

	@Override
	protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {
		session.assertState(State.NotAuthenticated);

		// TODO: implemenmt LOGINDISABLED
		// if (!session.isEncrypted()) {
		// 	throw ImapException.LoginNotAllowed();
		// }

		session.login("LOGIN", username, password, server.getStore());

		if (!session.isAuthenticated()) {
			throw ImapException.AuthenticationFailed();
		}

		List<String> capabilities = server.getCapabilities(session);
		client.writeLine(tag + " OK [" + StringUtils.join(capabilities, " ") + "] LOGIN completed");
	}

}
