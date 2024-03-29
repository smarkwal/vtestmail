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

package net.markwalder.vtestmail.pop3;

import java.io.IOException;
import net.markwalder.vtestmail.utils.Assert;

public class USER extends Pop3Command {

	private final String username;

	public USER(String username) {
		Assert.isNotEmpty(username, "username");
		this.username = username;
	}

	public static USER parse(String parameters) throws Pop3Exception {
		isNotEmpty(parameters);
		return new USER(parameters);
	}

	@Override
	public String toString() {
		return "USER " + username;
	}

	@Override
	protected void execute(Pop3Server server, Pop3Session session, Pop3Client client) throws IOException, Pop3Exception {
		session.assertState(State.AUTHORIZATION);

		// remember user
		session.setUser(username);

		client.writeLine("+OK User accepted");
	}

}
