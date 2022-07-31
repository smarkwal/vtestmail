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
import java.util.ArrayList;
import java.util.List;

public class EHLO extends SmtpCommand {

	public EHLO(String parameters) {
		super(parameters);
	}

	@Override
	protected void execute(SmtpServer server, SmtpSession session, SmtpClient client) throws IOException, ProtocolException {

		// send greeting to client
		String greeting = session.getServerAddress() + " Hello " + session.getClientAddress();
		client.writeLine("250-" + greeting);

		// send supported extensions to client
		List<String> extensions = getSupportedExtensions(server);
		for (String extension : extensions) {
			client.writeLine("250-" + extension);
		}

		client.writeLine("250 OK");
	}

	// TODO: move this method into SmtpServer?
	private List<String> getSupportedExtensions(SmtpServer server) {

		List<String> extensions = new ArrayList<>();

		// support STARTTLS
		if (server.isCommandEnabled("STARTTLS")) {
			extensions.add("STARTTLS");
		}

		// supported authentication types
		if (server.isCommandEnabled("AUTH")) {
			List<String> authTypes = server.getAuthTypes();
			if (authTypes.size() > 0) {
				extensions.add("AUTH " + String.join(" ", authTypes));
			}
		}

		// support enhanced status codes (ESMPT)
		extensions.add("ENHANCEDSTATUSCODES");

		return extensions;
	}

}
