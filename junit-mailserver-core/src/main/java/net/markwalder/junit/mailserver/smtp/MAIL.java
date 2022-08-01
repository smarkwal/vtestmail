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

package net.markwalder.junit.mailserver.smtp;

import java.io.IOException;
import net.markwalder.junit.mailserver.utils.StringUtils;

public class MAIL extends SmtpCommand {

	private final String email;

	public MAIL(String email) {
		this.email = email;
	}

	public static MAIL parse(String parameters) throws SmtpException {
		if (parameters == null || parameters.isEmpty()) {
			throw SmtpException.SyntaxError();
		}
		String email = StringUtils.substringBetween(parameters, "<", ">");
		if (email == null) {
			throw SmtpException.SyntaxError();
		}
		// TODO: validate email address
		return new MAIL(email);
	}

	@Override
	public String toString() {
		return "MAIL FROM:<" + email + ">";
	}

	@Override
	protected void execute(SmtpServer server, SmtpSession session, SmtpClient client) throws IOException, SmtpException {

		if (server.isAuthenticationRequired()) {
			throw SmtpException.AuthenticationRequired();
		}

		session.startTransaction(email);

		client.writeLine("250 2.1.0 OK");

	}

}
