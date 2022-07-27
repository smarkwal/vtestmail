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

package net.markwalder.junit.mailserver.pop3;

import java.io.IOException;
import net.markwalder.junit.mailserver.MailboxStore;
import net.markwalder.junit.mailserver.auth.Authenticator;
import net.markwalder.junit.mailserver.auth.Credentials;
import org.apache.commons.lang3.StringUtils;

public class AUTH extends Command {

	@Override
	protected void execute(String command, Pop3Server server, Pop3Session session, Pop3Client client) throws IOException, ProtocolException {
		session.assertState(State.AUTHORIZATION);

		// https://datatracker.ietf.org/doc/html/rfc4954
		// https://www.iana.org/assignments/sasl-mechanisms/sasl-mechanisms.xhtml
		// https://datatracker.ietf.org/doc/html/rfc5248

		// split command into POP3 verb ("AUTH"), auth type, and optional parameters
		String[] parts = StringUtils.split(command, " ", 3);
		String authType = parts[1];
		String parameters = parts.length > 2 ? parts[2] : null;

		// check if authentication type is supported
		if (!server.isAuthTypeSupported(authType)) {
			throw ProtocolException.UnrecognizedAuthenticationType();
		}

		// get user credentials from client
		Authenticator authenticator = server.getAuthenticator(authType);
		MailboxStore store = server.getStore();
		Credentials credentials = authenticator.authenticate(parameters, client, store);
		if (credentials == null) {
			throw ProtocolException.AuthenticationFailed();
		}

		// try to authenticate user
		String username = credentials.getUsername();
		String secret = credentials.getSecret();
		session.login(username, secret, store);

		if (!session.isAuthenticated()) {
			throw ProtocolException.AuthenticationFailed();
		}

		client.writeLine("+OK Authentication successful");
	}

}
