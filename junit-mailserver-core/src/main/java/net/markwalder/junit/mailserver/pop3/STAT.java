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

public class STAT extends Pop3Command {

	public STAT() {
	}

	public static STAT parse(String parameters) throws Pop3Exception {
		if (parameters != null) {
			throw Pop3Exception.SyntaxError();
		}
		return new STAT();
	}

	@Override
	public String toString() {
		return "STAT";
	}

	@Override
	protected void execute(Pop3Server server, Pop3Session session, Pop3Client client) throws IOException, Pop3Exception {
		session.assertState(State.TRANSACTION);

		int count = session.getMessageCount();
		int totalSize = session.getTotalSize();
		client.writeLine("+OK " + count + " " + totalSize);
	}

}
