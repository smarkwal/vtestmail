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
import net.markwalder.junit.mailserver.core.MailClient;
import net.markwalder.junit.mailserver.store.Mailbox;
import net.markwalder.junit.mailserver.store.MailboxProvider;
import net.markwalder.junit.mailserver.utils.RandomStringUtils;

public class CramMd5Authenticator implements Authenticator {

	private static final Charset CHARSET = StandardCharsets.ISO_8859_1;

	@Override
	public Credentials authenticate(String parameters, MailClient client, MailboxProvider store) throws IOException {

		// https://mailtrap.io/blog/smtp-auth/
		// https://datatracker.ietf.org/doc/html/rfc2195

		if (parameters != null) {
			// CRAM-MD5 does not accept parameters
			return null;
		}

		// send random challenge to client
		String challenge = RandomStringUtils.randomAscii(16); // TODO: inject random number generator
		client.writeContinue(AuthUtils.encodeBase64(challenge, CHARSET));

		// read response from client
		String response = client.readLine();

		// decode response
		String data = AuthUtils.decodeBase64(response, CHARSET);
		if (data == null) {
			return null;
		}

		// extract username and password hash
		int pos = data.lastIndexOf(' ');
		if (pos < 0) {
			return null;
		}
		String username = data.substring(0, pos);
		String clientHash = data.substring(pos + 1);

		// check if mailbox exists
		Mailbox mailbox = store.getMailbox(username);
		if (mailbox == null) {
			return null;
		}

		// compare password hashes
		String password = mailbox.getSecret();
		String serverHash = AuthUtils.calculateHmacMD5Hex(challenge, password);
		if (!clientHash.equals(serverHash)) {
			return null;
		}

		return new Credentials(username, password);
	}

}
