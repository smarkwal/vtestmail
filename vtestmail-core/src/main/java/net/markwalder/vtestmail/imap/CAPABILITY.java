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

package net.markwalder.vtestmail.imap;

import java.io.IOException;
import java.util.List;
import net.markwalder.vtestmail.utils.StringUtils;

public class CAPABILITY extends ImapCommand {

	public CAPABILITY() {
		// command has no parameters
	}

	public static CAPABILITY parse(String parameters) throws ImapException {
		isNull(parameters);
		return new CAPABILITY();
	}

	@Override
	public String toString() {
		return "CAPABILITY";
	}

	@Override
	protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {
		List<String> capabilities = server.getCapabilities(session);
		client.writeLine("* " + StringUtils.join(capabilities, " "));
		client.writeLine(tag + " OK CAPABILITY completed");
	}

}
