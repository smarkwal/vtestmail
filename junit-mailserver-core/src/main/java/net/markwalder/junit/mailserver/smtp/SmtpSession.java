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

package net.markwalder.junit.mailserver.smtp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.markwalder.junit.mailserver.MailSession;

public class SmtpSession extends MailSession {

	private final List<SmtpCommand> commands = new ArrayList<>();

	/**
	 * Current active SMTP transaction.
	 */
	private SmtpTransaction transaction = null;

	/**
	 * History of completed SMTP transactions.
	 */
	private final List<SmtpTransaction> transactions = new ArrayList<>();

	void addCommand(SmtpCommand command) {
		if (transaction == null) {
			commands.add(command);
		} else {
			transaction.addCommand(command);
		}
	}

	public List<SmtpCommand> getCommands() {
		return Collections.unmodifiableList(commands);
	}

	void startTransaction(String sender) {
		transaction = new SmtpTransaction();
		transaction.setSender(sender);
	}

	void addRecipient(String email) {
		if (transaction != null) {
			transaction.addRecipient(email);
		}
	}

	List<String> getRecipients() {
		if (transaction != null) {
			return transaction.getRecipients();
		} else {
			return Collections.emptyList();
		}
	}

	void endTransaction(String data) {
		if (transaction != null) {
			transaction.setData(data);
			transactions.add(transaction);
			transaction = null;
		}
	}

	public List<SmtpTransaction> getTransactions() {
		return Collections.unmodifiableList(transactions);
	}

}
