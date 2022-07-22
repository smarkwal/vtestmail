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
import net.markwalder.junit.mailserver.Client;

public class CAPA extends Command {

	@Override
	protected void execute(String command, Pop3Server server, Client client) throws IOException, ProtocolException {

		client.writeLine("+OK Capability list follows");
		client.writeLine("USER"); // TODO: skip USER if at least one auth type is set?
		List<String> authTypes = server.getAuthTypes();
		if (authTypes.size() > 0) {
			client.writeLine("SASL " + String.join(" ", authTypes));
		}
		client.writeLine("TOP");
		client.writeLine("UIDL");
		client.writeLine("EXPIRE NEVER");
		// TODO: client.writeLine("RESP-CODES");
		client.writeLine("IMPLEMENTATION junit-mailserver");
		client.writeLine(".");

	}

}
