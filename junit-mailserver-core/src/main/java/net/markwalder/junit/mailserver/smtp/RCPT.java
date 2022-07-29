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
import org.apache.commons.lang3.StringUtils;

public class RCPT extends SmtpCommand {

	public RCPT(String line) {
		super(line);
	}

	@Override
	protected void execute(SmtpServer server, SmtpSession session, SmtpClient client) throws IOException, ProtocolException {

		if (server.isAuthenticationRequired()) {
			throw ProtocolException.AuthenticationRequired();
		}

		String email = StringUtils.substringBetween(line, "<", ">");
		if (email == null) {
			throw ProtocolException.SyntaxError();
		}

		// remember email address of recipient
		// note: email address is not validated
		session.addRecipient(email);

		client.writeLine("250 2.1.5 OK");

	}

}
