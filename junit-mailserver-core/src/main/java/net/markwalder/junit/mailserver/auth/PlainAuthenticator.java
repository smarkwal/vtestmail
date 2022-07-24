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
import net.markwalder.junit.mailserver.Client;
import net.markwalder.junit.mailserver.MailboxStore;

/**
 * Implementation of PLAIN authentication.
 */
public class PlainAuthenticator implements Authenticator {

	private static final String NULL = "\u0000";

	@Override
	public Credentials authenticate(String parameters, Client client, MailboxStore store) throws IOException {

		// https://www.rfc-editor.org/rfc/rfc4616.html
		// https://mailtrap.io/blog/smtp-auth/

		if (parameters == null) {
			// ask client for credentials
			client.writeContinue(null);
			parameters = client.readLine();
		}

		// decode credentials
		String data = AuthUtils.decodeBase64(parameters);
		if (data == null) {
			return null;
		}

		// extract username and password
		String[] parts = data.split(NULL, -1);
		if (parts.length < 3) {
			return null;
		}

		// note: parts[0] is ignored // RFC 4616 authorization identity (authzid)
		String username = parts[1]; // RFC 4616 authentication identity (authcid)
		String password = parts[2]; // RFC 4616 password (passwd)

		return new Credentials(username, password);
	}

}
