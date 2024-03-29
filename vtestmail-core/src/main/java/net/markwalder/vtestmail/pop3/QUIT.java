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
import net.markwalder.vtestmail.store.Mailbox;
import net.markwalder.vtestmail.store.MailboxFolder;

public class QUIT extends Pop3Command {

	public QUIT() {
		// command has no parameters
	}

	public static QUIT parse(String parameters) throws Pop3Exception {
		isNull(parameters);
		return new QUIT();
	}

	@Override
	public String toString() {
		return "QUIT";
	}

	@Override
	protected void execute(Pop3Server server, Pop3Session session, Pop3Client client) throws IOException, Pop3Exception {

		// enter update state
		session.setState(State.UPDATE);

		// delete messages marked as deleted
		Mailbox mailbox = session.getMailbox();
		if (mailbox != null) {
			MailboxFolder folder = mailbox.getInbox();
			int[] messageNumbers = session.getDeleted();
			if (messageNumbers.length > 0) {
				// loop in reverse order to avoid index changes
				for (int i = messageNumbers.length - 1; i >= 0; i--) {
					int messageNumber = messageNumbers[i];
					folder.removeMessage(messageNumber);
				}
			}
		}

		// set "closed" flag in session
		session.close();

		client.writeLine("+OK Goodbye");
	}

}
