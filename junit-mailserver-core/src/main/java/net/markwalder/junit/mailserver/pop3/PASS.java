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
import org.apache.commons.lang3.StringUtils;

public class PASS extends Command {

	@Override
	protected void execute(String command, Pop3Server server, Pop3Session session, Pop3Client client) throws IOException, ProtocolException {
		session.assertState(State.AUTHORIZATION);

		// get username
		String user = session.getUser();
		if (user == null) {
			throw new ProtocolException("USER command not received");
		}

		// get password
		String password = StringUtils.substringAfter(command, "PASS ");

		// try to authenticate
		session.login("USER", user, password, server.getStore());

		if (!session.isAuthenticated()) {
			client.writeLine("-ERR Authentication failed");
			return;
		}

		client.writeLine("+OK Authentication successful");
	}

}
