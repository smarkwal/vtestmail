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
import net.markwalder.junit.mailserver.core.MailCommand;

public abstract class ImapCommand extends MailCommand {

	public static final String HIERARCHY_SEPARATOR = "/";

	protected abstract void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException;

	protected static void isNull(String value) throws ImapException {
		if (value != null) {
			throw ImapException.SyntaxError();
		}
	}

	protected static void isNotEmpty(String value) throws ImapException {
		if (value == null || value.isEmpty()) {
			throw ImapException.SyntaxError();
		}
	}

}
