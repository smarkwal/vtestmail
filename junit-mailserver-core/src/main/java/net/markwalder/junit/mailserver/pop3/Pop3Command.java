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
import net.markwalder.junit.mailserver.MailCommand;

public abstract class Pop3Command extends MailCommand {

	protected abstract void execute(Pop3Server server, Pop3Session session, Pop3Client client) throws IOException, Pop3Exception;

	protected static int parseMessageNumber(String text) throws Pop3Exception {
		return parseNumber(text, 1);
	}

	protected static int parseNumber(String text, int minValue) throws Pop3Exception {
		if (text == null || text.isEmpty()) {
			throw Pop3Exception.SyntaxError();
		}
		try {
			// TODO: do not accept values starting with "+"
			int number = Integer.parseInt(text);
			if (number < minValue) {
				throw Pop3Exception.SyntaxError();
			}
			return number;
		} catch (NumberFormatException e) {
			throw Pop3Exception.SyntaxError();
		}
	}

}
