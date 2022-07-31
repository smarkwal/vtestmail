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
import net.markwalder.junit.mailserver.utils.StringUtils;

public class APOP extends Pop3Command {

	public APOP(String username, String digest) {
		this(username + " " + digest);
	}

	APOP(String parameters) {
		super(parameters);
	}

	@Override
	protected void execute(Pop3Server server, Pop3Session session, Pop3Client client) throws IOException, ProtocolException {
		session.assertState(State.AUTHORIZATION);

		String username = StringUtils.substringBefore(parameters, " ");
		String digest = StringUtils.substringAfter(parameters, " ");
		if (username == null || digest == null) {
			throw new ProtocolException("Invalid APOP command");
		}

		// try to authenticate
		String timestamp = session.getTimestamp();
		session.login("APOP", username, digest, timestamp, server.getStore());

		if (!session.isAuthenticated()) {
			client.writeLine("-ERR Authentication failed");
			return;
		}

		client.writeLine("+OK Authentication successful");
	}

}
