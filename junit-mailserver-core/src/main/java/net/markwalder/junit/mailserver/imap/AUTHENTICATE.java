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
import net.markwalder.junit.mailserver.store.MailboxStore;
import net.markwalder.junit.mailserver.auth.Authenticator;
import net.markwalder.junit.mailserver.auth.Credentials;
import net.markwalder.junit.mailserver.utils.Assert;
import net.markwalder.junit.mailserver.utils.StringUtils;

public class AUTHENTICATE extends ImapCommand {

	private final String authType;
	private final String initialResponse;

	public AUTHENTICATE(String authType) {
		this(authType, null);
	}

	public AUTHENTICATE(String authType, String initialResponse) {
		Assert.isNotEmpty(authType, "authType");
		this.authType = authType;
		this.initialResponse = initialResponse;
	}

	public static AUTHENTICATE parse(String parameters) throws ImapException {
		isNotEmpty(parameters);
		String authType = StringUtils.substringBefore(parameters, " ");
		String initialResponse = StringUtils.substringAfter(parameters, " ");
		isNotEmpty(authType);
		return new AUTHENTICATE(authType, initialResponse);
	}

	@Override
	public String toString() {
		if (initialResponse == null) {
			return "AUTHENTICATE " + authType;
		} else {
			return "AUTHENTICATE " + authType + " " + initialResponse;
		}
	}

	@Override
	protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {
		session.assertState(State.NotAuthenticated);

		// check if authentication type is supported
		if (!server.isAuthTypeSupported(authType)) {
			throw ImapException.UnrecognizedAuthenticationType();
		}

		// get user credentials from client
		Authenticator authenticator = server.getAuthenticator(authType);
		MailboxStore store = server.getStore();
		Credentials credentials = authenticator.authenticate(null, client, store);
		if (credentials == null) {
			throw ImapException.AuthenticationFailed();
		}

		// try to authenticate user
		String username = credentials.getUsername();
		String secret = credentials.getSecret();
		session.login(authType, username, secret, store);

		if (!session.isAuthenticated()) {
			throw ImapException.AuthenticationFailed();
		}

		List<String> capabilities = server.getCapabilities(session);
		client.writeLine(tag + " OK [" + StringUtils.join(capabilities, " ") + "] Logged in");
	}

}
