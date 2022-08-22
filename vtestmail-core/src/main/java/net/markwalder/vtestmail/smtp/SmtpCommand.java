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

package net.markwalder.vtestmail.smtp;

import java.io.IOException;
import net.markwalder.vtestmail.core.MailCommand;

public abstract class SmtpCommand extends MailCommand {

	protected abstract void execute(SmtpServer server, SmtpSession session, SmtpClient client) throws IOException, SmtpException;

	protected static void isNull(String value) throws SmtpException {
		if (value != null) {
			throw SmtpException.SyntaxError();
		}
	}

	protected static void isNotEmpty(String value) throws SmtpException {
		if (value == null || value.isEmpty()) {
			throw SmtpException.SyntaxError();
		}
	}

	protected static void isValidDomain(String domain) throws SmtpException {
		isNotEmpty(domain);
		// TODO: validate domain
	}

	protected static void isValidEmail(String email) throws SmtpException {
		isNotEmpty(email);
		// TODO: validate email address
	}

}