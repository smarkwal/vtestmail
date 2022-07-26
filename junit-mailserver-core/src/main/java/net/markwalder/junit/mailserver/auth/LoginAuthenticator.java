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

package net.markwalder.junit.mailserver.auth;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import net.markwalder.junit.mailserver.Client;
import net.markwalder.junit.mailserver.MailboxStore;

public class LoginAuthenticator implements Authenticator {

	private static final Charset CHARSET = StandardCharsets.UTF_8;
	private static final String USERNAME_CHALLENGE = AuthUtils.encodeBase64("Username:", CHARSET);
	private static final String PASSWORD_CHALLENGE = AuthUtils.encodeBase64("Password:", CHARSET);

	@Override
	public Credentials authenticate(String parameters, Client client, MailboxStore store) throws IOException {

		// https://mailtrap.io/blog/smtp-auth/

		if (parameters != null) {
			// LOGIN does not accept parameters
			return null;
		}

		// ask client for username
		client.writeContinue(USERNAME_CHALLENGE);
		String response = client.readLine();
		String username = AuthUtils.decodeBase64(response, CHARSET);
		if (username == null) {
			return null;
		}

		// ask client for password
		client.writeContinue(PASSWORD_CHALLENGE);
		response = client.readLine();
		String password = AuthUtils.decodeBase64(response, CHARSET);
		if (password == null) {
			return null;
		}

		return new Credentials(username, password);
	}

}
