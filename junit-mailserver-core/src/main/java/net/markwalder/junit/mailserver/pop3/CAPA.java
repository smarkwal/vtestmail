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

package net.markwalder.junit.mailserver.pop3;

import java.io.IOException;
import java.util.List;

public class CAPA extends Pop3Command {

	public CAPA() {
		this(null);
	}

	CAPA(String parameters) {
		super(parameters);
	}

	@Override
	protected void execute(Pop3Server server, Pop3Session session, Pop3Client client) throws IOException, Pop3Exception {

		client.writeLine("+OK Capability list follows");
		if (server.isCommandEnabled("USER")) {
			client.writeLine("USER");
		}
		if (server.isCommandEnabled("APOP")) {
			client.writeLine("APOP");
		}
		List<String> authTypes = server.getAuthTypes();
		if (authTypes.size() > 0) {
			client.writeLine("SASL " + String.join(" ", authTypes));
		}
		if (server.isCommandEnabled("TOP")) {
			client.writeLine("TOP");
		}
		if (server.isCommandEnabled("UIDL")) {
			client.writeLine("UIDL");
		}
		client.writeLine("EXPIRE NEVER");
		// TODO: client.writeLine("RESP-CODES");
		client.writeLine("IMPLEMENTATION junit-mailserver");
		client.writeLine(".");

	}

}
