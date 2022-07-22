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
import net.markwalder.junit.mailserver.Client;
import org.apache.commons.lang3.StringUtils;

public class APOP extends Command {

	@Override
	protected void execute(String command, Pop3Server server, Client client) throws IOException, ProtocolException {
		server.assertState(Pop3Server.State.AUTHORIZATION);

		String[] parts = StringUtils.split(command, " ");
		if (parts.length != 3) {
			throw new ProtocolException("Invalid APOP command");
		}

		String username = parts[1];
		String digest = parts[2];

		// try to authenticate
		String timestamp = server.getTimestamp();
		server.login(username, digest, timestamp);

		if (!server.isAuthenticated()) {
			client.writeLine("-ERR Authentication failed");
			return;
		}

		// enter transaction state
		server.setState(Pop3Server.State.TRANSACTION);

		client.writeLine("+OK Authentication successful");
	}

}
