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
import org.apache.commons.lang3.RandomStringUtils;

public class CramMd5Authenticator implements Authenticator {

	@Override
	public Credentials authenticate(String parameters, Client client, MailboxStore store) throws IOException {

		// https://mailtrap.io/blog/smtp-auth/

		if (parameters != null) {
			// CRAM-MD5 does not accept parameters
			return null;
		}

		// send random challenge to client
		String challenge = RandomStringUtils.randomAlphanumeric(9);
		client.writeContinue(AuthUtils.encodeBase64(challenge));

		// read response from client
		String response = client.readLine();

		// decode credentials
		String data = AuthUtils.decodeBase64(response);
		if (data == null) {
			return null;
		}

		// TODO: implement
		return null;
	}

}
