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

package net.markwalder.vtestmail.smtp;

import java.util.ArrayList;
import java.util.List;

public class SmtpTransaction {

	private final List<SmtpCommand> commands = new ArrayList<>();
	private String sender;
	private final List<String> recipients = new ArrayList<>();
	private String data;

	void addCommand(SmtpCommand command) {
		synchronized (commands) {
			commands.add(command);
		}
	}

	/**
	 * Returns the list of commands that have been sent to the server so far in
	 * this transaction. The list is a copy and can be modified without
	 * affecting the transaction.
	 *
	 * @return List of commands.
	 */
	public List<SmtpCommand> getCommands() {
		synchronized (commands) {
			return new ArrayList<>(commands);
		}
	}

	public String getSender() {
		return sender;
	}

	void setSender(String sender) {
		this.sender = sender;
	}

	public List<String> getRecipients() {
		synchronized (recipients) {
			return new ArrayList<>(recipients);
		}
	}

	void addRecipient(String recipient) {
		synchronized (recipients) {
			recipients.add(recipient);
		}
	}

	public String getData() {
		return data;
	}

	void setData(String data) {
		this.data = data;
	}

}
