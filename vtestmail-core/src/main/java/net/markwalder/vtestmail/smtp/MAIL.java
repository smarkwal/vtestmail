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
import net.markwalder.vtestmail.utils.Assert;
import net.markwalder.vtestmail.utils.StringUtils;

public class MAIL extends SmtpCommand {

	private final String email;

	public MAIL(String email) {
		Assert.isNotEmpty(email, "email");
		this.email = email;
	}

	public static MAIL parse(String parameters) throws SmtpException {
		isNotEmpty(parameters);
		String email = StringUtils.substringBetween(parameters, "<", ">");
		isValidEmail(email);
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
