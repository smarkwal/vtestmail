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

public class STARTTLS extends SmtpCommand {

	public STARTTLS() {
		// command has no parameters
	}

	public static STARTTLS parse(String parameters) throws SmtpException {
		isNull(parameters);
		return new STARTTLS();
	}

	@Override
	public String toString() {
		return "STARTTLS";
	}

	@Override
	protected void execute(SmtpServer server, SmtpSession session, SmtpClient client) throws IOException, SmtpException {
		client.writeLine("220 Ready to start TLS");

		// start TLS handshake
		String sslProtocol = server.getSSLProtocol();
		client.startTLS(sslProtocol, session);
	}

}
