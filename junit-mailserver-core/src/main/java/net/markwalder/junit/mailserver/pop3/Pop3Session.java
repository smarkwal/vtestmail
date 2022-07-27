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

import java.util.Collections;
import java.util.List;
import net.markwalder.junit.mailserver.MailSession;
import net.markwalder.junit.mailserver.Mailbox;
import net.markwalder.junit.mailserver.MailboxProvider;

public class Pop3Session extends MailSession {

	/**
	 * Timestamp for APOP authentication
	 */
	private final String timestamp = "<" + System.currentTimeMillis() + "@localhost>";

	private State state = State.AUTHORIZATION;
	private String user = null;
	private Mailbox mailbox = null;

	@Override
	public void login(String username, String secret, MailboxProvider store) {
		super.login(username, secret, store);

		if (isAuthenticated()) {
			postLogin(username, store);
		}
	}

	@Override
	public void login(String username, String digest, String timestamp, MailboxProvider store) {
		super.login(username, digest, timestamp, store);

		if (isAuthenticated()) {
			postLogin(username, store);
		}
	}

	private void postLogin(String username, MailboxProvider store) {

		// enter transaction state
		state = State.TRANSACTION;

		// TODO: acquire exclusive lock on mailbox
		mailbox = store.getMailbox(username);
	}

	/**
	 * Check if session is currently in given state.
	 *
	 * @param expectedState Expected state.
	 */
	void assertState(State expectedState) throws ProtocolException {
		if (this.state != expectedState) {
			throw ProtocolException.IllegalState(this.state);
		}
	}

	void setState(State state) {
		this.state = state;
	}

	String getTimestamp() {
		return timestamp;
	}

	String getUser() {
		return user;
	}

	void setUser(String user) {
		this.user = user;
	}

	Mailbox getMailbox() {
		return mailbox;
	}

	// helper methods ----------------------------------------------------------

	List<Mailbox.Message> getMessages() {
		if (mailbox == null) {
			// mailbox not found -> return empty list
			return Collections.emptyList();
		}
		return mailbox.getMessages();
	}

	Mailbox.Message getMessage(String msg) {

		// try to parse parameter "msg"
		int idx;
		try {
			idx = Integer.parseInt(msg) - 1;
		} catch (NumberFormatException e) {
			// not a number -> message not found
			return null;
		}

		List<Mailbox.Message> messages = getMessages();
		if (idx < 0 || idx >= messages.size()) {
			// index out of range -> message not found
			return null;
		}

		return messages.get(idx);

	}

	/**
	 * Get the total number of non-deleted messages in the mailbox of the given
	 * user. If the mailbox does not exist, 0 is returned.
	 *
	 * @return Number of messages in the mailbox.
	 */
	int getMessageCount() {
		List<Mailbox.Message> messages = getMessages();
		long count = messages.stream()
				.filter(m -> !m.isDeleted()) // ignore deleted messages
				.count();
		return (int) count;
	}

	/**
	 * Get the total size of all non-deleted messages in the mailbox of the
	 * given user. If the mailbox does not exist, 0 is returned.
	 *
	 * @return Size to all messages in the mailbox.
	 */
	int getTotalSize() {
		List<Mailbox.Message> messages = getMessages();
		return messages.stream()
				.filter(m -> !m.isDeleted()) // ignore deleted messages
				.mapToInt(Mailbox.Message::getSize)
				.sum();
	}

}
