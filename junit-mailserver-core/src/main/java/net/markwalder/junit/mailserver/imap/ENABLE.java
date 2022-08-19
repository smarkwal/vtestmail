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

package net.markwalder.junit.mailserver.imap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.markwalder.junit.mailserver.utils.StringUtils;

public class ENABLE extends ImapCommand {

	private final List<String> extensions;

	public ENABLE(List<String> extensions) {
		this.extensions = new ArrayList<>(extensions);
	}

	public static ENABLE parse(String parameters) throws ImapException {
		isNotEmpty(parameters);
		String[] extensions = StringUtils.split(parameters, Integer.MAX_VALUE);
		return new ENABLE(Arrays.asList(extensions));
	}

	@Override
	public String toString() {
		return "ENABLE " + StringUtils.join(extensions, " ");
	}

	@Override
	protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {
		session.assertState(State.Authenticated);

		// see https://datatracker.ietf.org/doc/html/rfc9051#section-6.3.1

		for (String extension : extensions) {
			// TODO: enable extension in session (if supported)
		}

		client.writeLine(tag + " OK ENABLE completed");
	}

}
