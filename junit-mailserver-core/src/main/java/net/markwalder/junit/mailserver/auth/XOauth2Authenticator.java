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

import net.markwalder.junit.mailserver.Client;
import net.markwalder.junit.mailserver.MailboxStore;
import org.apache.commons.lang3.StringUtils;

public class XOauth2Authenticator implements Authenticator {

	@Override
	public Credentials authenticate(String parameters, Client client, MailboxStore store) {

		// https://developers.google.com/gmail/imap/xoauth2-protocol

		if (parameters == null) {
			// XOAUTH2 requires parameters
			return null;
		}

		// decode credentials
		String data = AuthUtils.decodeBase64(parameters);
		if (data == null) {
			return null;
		}

		// remove trailing 0x01 characters
		data = data.trim();

		// extract username and access token
		String username = StringUtils.substringBetween(data, "user=", "\u0001auth=");
		String accessToken = StringUtils.substringAfter(data, "auth=Bearer ");

		return new Credentials(username, accessToken);
	}

}
