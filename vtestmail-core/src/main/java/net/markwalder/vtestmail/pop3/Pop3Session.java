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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.markwalder.vtestmail.core.MailSession;
import net.markwalder.vtestmail.store.Mailbox;
import net.markwalder.vtestmail.store.MailboxFolder;
import net.markwalder.vtestmail.store.MailboxMessage;
import net.markwalder.vtestmail.store.MailboxProvider;
import net.markwalder.vtestmail.utils.Assert;

public class Pop3Session extends MailSession {

	/**
	 * Timestamp for APOP authentication
	 */
	private final String timestamp;

	private final List<Pop3Command> commands = new ArrayList<>();

	private State state = State.AUTHORIZATION;
	private String user = null;
	private Mailbox mailbox = null;

	/**
	 * Numbers of messages marked as deleted.
	 */
	private final Set<Integer> deleted = new HashSet<>();

	Pop3Session(String timestamp) {
		Assert.isNotEmpty(timestamp, "timestamp");
		this.timestamp = timestamp;
	}

	/**
	 * Add a command to the list of commands executed in this session.
	 *
	 * @param command Command
	 */
	void addCommand(Pop3Command command) {
		Assert.isNotNull(command, "command");
		synchronized (commands) {
			commands.add(command);
		}
	}

	/**
	 * Returns the list of commands that have been sent to the server so far in
	 * this session. The list is a copy and can be modified without affecting
	 * the session.
	 *
	 * @return List of commands.
	 */
	public List<Pop3Command> getCommands() {
		synchronized (commands) {
			return new ArrayList<>(commands);
		}
	}

	@Override
	public void login(String authType, String username, String secret, MailboxProvider store) {
		super.login(authType, username, secret, store);

		if (isAuthenticated()) {
			postLogin(username, store);
		}
	}

	@Override
	public void login(String authType, String username, String digest, String timestamp, MailboxProvider store) {
		super.login(authType, username, digest, timestamp, store);

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
	void assertState(State expectedState) throws Pop3Exception {
		if (this.state != expectedState) {
			throw Pop3Exception.IllegalState(this.state);
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

	/**
	 * Get numbers of messages marked as deleted.
	 *
	 * @return Numbers of messages marked as deleted in ascending order.
	 */
	public int[] getDeleted() {
		return deleted.stream().mapToInt(Integer::intValue).sorted().toArray();
	}

	public boolean isDeleted(int messageNumber) {
		Assert.isInRange(messageNumber, 1, Integer.MAX_VALUE, "messageNumber");
		return deleted.contains(messageNumber);
	}

	void setDeleted(int messageNumber) {
		Assert.isInRange(messageNumber, 1, Integer.MAX_VALUE, "messageNumber");
		deleted.add(messageNumber);
	}

	void clearDeleted() {
		deleted.clear();
	}

	// helper methods ----------------------------------------------------------

	List<MailboxMessage> getMessages() {
		if (mailbox == null) {
			// mailbox not found -> return empty list
			return Collections.emptyList();
		}
		MailboxFolder folder = mailbox.getInbox();
		return folder.getMessages();
	}

	MailboxMessage getMessage(int msg) {

		List<MailboxMessage> messages = getMessages();
		if (msg < 1 || msg > messages.size()) {
			// index out of range -> message not found
			return null;
		}

		return messages.get(msg - 1);

	}

	/**
	 * Get the total number of non-deleted messages in the mailbox of the given
	 * user. If the mailbox does not exist, 0 is returned.
	 *
	 * @return Number of messages in the mailbox.
	 */
	int getMessageCount() {
		int count = 0;
		List<MailboxMessage> messages = getMessages();
		for (int i = 0; i < messages.size(); i++) {
			MailboxMessage message = messages.get(i);
			if (message.isDeleted() || isDeleted(i + 1)) {
				continue; // ignore deleted messages
			}
			count++;
		}
		return count;
	}

	/**
	 * Get the total size of all non-deleted messages in the mailbox of the
	 * given user. If the mailbox does not exist, 0 is returned.
	 *
	 * @return Size to all messages in the mailbox.
	 */
	int getTotalSize() {
		int size = 0;
		List<MailboxMessage> messages = getMessages();
		for (int i = 0; i < messages.size(); i++) {
			MailboxMessage message = messages.get(i);
			if (message.isDeleted() || isDeleted(i + 1)) {
				continue; // ignore deleted messages
			}
			size += message.getSize();
		}
		return size;
	}

}
