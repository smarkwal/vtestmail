/*
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

package net.markwalder.vtestmail.smtp;

import java.io.IOException;
import net.markwalder.vtestmail.auth.Authenticator;
import net.markwalder.vtestmail.auth.Credentials;
import net.markwalder.vtestmail.store.MailboxStore;
import net.markwalder.vtestmail.utils.Assert;
import net.markwalder.vtestmail.utils.StringUtils;

public class AUTH extends SmtpCommand {

	private final String authType;
	private final String initialResponse;

	public AUTH(String authType) {
		this(authType, null);
	}

	public AUTH(String authType, String initialResponse) {
		Assert.isNotEmpty(authType, "authType");
		this.authType = authType;
		this.initialResponse = initialResponse;
	}

	public static AUTH parse(String parameters) throws SmtpException {
		isNotEmpty(parameters);
		String authType = StringUtils.substringBefore(parameters, " ");
		String initialResponse = StringUtils.substringAfter(parameters, " ");
		isNotEmpty(authType);
		return new AUTH(authType, initialResponse);
	}

	@Override
	public String toString() {
		if (initialResponse == null) {
			return "AUTH " + authType;
		} else {
			return "AUTH " + authType + " " + initialResponse;
		}
	}

	@Override
	protected void execute(SmtpServer server, SmtpSession session, SmtpClient client) throws IOException, SmtpException {

		// https://datatracker.ietf.org/doc/html/rfc4954
		// https://www.iana.org/assignments/sasl-mechanisms/sasl-mechanisms.xhtml
		// https://datatracker.ietf.org/doc/html/rfc5248

		// check if authentication type is supported
		if (!server.isAuthTypeSupported(authType)) {
			throw SmtpException.UnrecognizedAuthenticationType();
		}

		// get user credentials from client
		Authenticator authenticator = server.getAuthenticator(authType);
		MailboxStore store = server.getStore();
		Credentials credentials = authenticator.authenticate(initialResponse, client, store);
		if (credentials == null) {
			throw SmtpException.AuthenticationFailed();
		}

		// try to authenticate user
		String username = credentials.getUsername();
		String secret = credentials.getSecret();
		session.login(authType, username, secret, store);

		if (!session.isAuthenticated()) {
			throw SmtpException.AuthenticationFailed();
		}

		client.writeLine("235 2.7.0 Authentication succeeded");
	}

}
