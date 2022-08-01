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
import net.markwalder.junit.mailserver.Mailbox;
import net.markwalder.junit.mailserver.MailboxStore;
import net.markwalder.junit.mailserver.utils.Assert;

public class VRFY extends SmtpCommand {

	private final String email;

	public VRFY(String email) {
		Assert.isNotEmpty(email, "email");
		this.email = email;
	}

	public static VRFY parse(String parameters) throws SmtpException {
		isValidEmail(parameters);
		return new VRFY(parameters);
	}

	@Override
	public String toString() {
		return "VRFY " + email;
	}

	@Override
	protected void execute(SmtpServer server, SmtpSession session, SmtpClient client) throws IOException, SmtpException {

		if (server.isAuthenticationRequired()) {
			throw SmtpException.AuthenticationRequired();
		}

		// check if mailbox exists
		MailboxStore store = server.getStore();
		Mailbox mailbox = store.findMailbox(email);
		if (mailbox != null) {
			client.writeLine("250 2.1.0 <" + email + "> User OK");
		} else {
			// https://datatracker.ietf.org/doc/html/rfc5321#section-3.5.3
			// Cannot VRFY user, but will accept message and attempt delivery
			client.writeLine("252 2.1.0 <" + email + "> No such user");
		}

	}

}
