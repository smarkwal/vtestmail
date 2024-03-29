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
import java.util.List;
import net.markwalder.vtestmail.utils.Assert;

public class EHLO extends SmtpCommand {

	private final String domain;

	public EHLO(String domain) {
		Assert.isNotEmpty(domain, "domain");
		this.domain = domain;
	}

	public static EHLO parse(String parameters) throws SmtpException {
		isValidDomain(parameters);
		return new EHLO(parameters);
	}

	@Override
	public String toString() {
		return "EHLO " + domain;
	}

	@Override
	protected void execute(SmtpServer server, SmtpSession session, SmtpClient client) throws IOException, SmtpException {

		// send greeting to client
		String greeting = session.getServerAddress() + " Hello " + domain;
		client.writeLine("250-" + greeting);

		// send supported extensions to client
		List<String> extensions = server.getSupportedExtensions(session);
		for (String extension : extensions) {
			client.writeLine("250-" + extension);
		}

		client.writeLine("250 OK");
	}


}
