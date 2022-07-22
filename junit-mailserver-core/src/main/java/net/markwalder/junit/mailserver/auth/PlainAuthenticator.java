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
import org.apache.commons.lang3.StringUtils;

/**
 * Implementation of PLAIN authentication.
 */
public class PlainAuthenticator implements Authenticator {

	@Override
	public Credentials authenticate(String parameters, Client client, MailboxStore store) throws IOException {

		// https://www.rfc-editor.org/rfc/rfc4616.html
		// https://mailtrap.io/blog/smtp-auth/

		if (parameters == null) {
			// ask client for credentials
			client.writeLine("334"); // TODO: support POP3-style auth
			parameters = client.readLine();
		}

		// decode credentials
		String data = AuthUtils.decodeBase64(parameters);
		if (data == null) {
			return null;
		}

		// extract username and password
		String[] parts = StringUtils.split(data, '\u0000');
		if (parts.length < 3) {
			return null;
		}

		String username = parts[1];
		String password = parts[2];

		return new Credentials(username, password);
	}

}
