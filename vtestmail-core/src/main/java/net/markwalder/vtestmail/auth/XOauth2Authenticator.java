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

package net.markwalder.vtestmail.auth;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import net.markwalder.vtestmail.core.MailClient;
import net.markwalder.vtestmail.store.MailboxProvider;
import net.markwalder.vtestmail.utils.StringUtils;

public class XOauth2Authenticator implements Authenticator {

	private static final Charset CHARSET = StandardCharsets.UTF_8;

	@Override
	public Credentials authenticate(String parameters, MailClient client, MailboxProvider store) {

		// https://developers.google.com/gmail/imap/xoauth2-protocol

		if (parameters == null) {
			// XOAUTH2 requires parameters
			return null;
		}

		// decode credentials
		String data = AuthUtils.decodeBase64(parameters, CHARSET);
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
