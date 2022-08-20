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

import java.util.ArrayList;
import java.util.List;
import net.markwalder.junit.mailserver.MailSession;
import net.markwalder.junit.mailserver.Mailbox;
import net.markwalder.junit.mailserver.MailboxProvider;
import net.markwalder.junit.mailserver.utils.Assert;

public class ImapSession extends MailSession {

	// see https://datatracker.ietf.org/doc/html/rfc9051#section-3
	private State state = State.NotAuthenticated;

	private Mailbox mailbox = null;
	private boolean readOnly = false;

	private final List<ImapCommand> commands = new ArrayList<>();

	// TODO: implement mailbox size and message status updates
	// see https://datatracker.ietf.org/doc/html/rfc9051#section-5.2

	/**
	 * Check if session is currently in given state.
	 *
	 * @param expectedState Expected state.
	 */
	void assertState(State expectedState) throws ImapException {
		if (this.state != expectedState) {
			throw ImapException.IllegalState(this.state);
		}
	}

	State getState() {
		return state;
	}

	void setState(State state) {
		this.state = state;
	}

	public Mailbox getMailbox() {
		return mailbox;
	}

	/**
	 * Add a command to the list of commands executed in this session.
	 *
	 * @param command Command
	 */
	void addCommand(ImapCommand command) {
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
	public List<ImapCommand> getCommands() {
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

		// enter Authenticated state
		setState(State.Authenticated);

		mailbox = store.getMailbox(username);
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public void assertReadWrite() throws ImapException {
		if (isReadOnly()) {
			throw ImapException.MailboxIsReadOnly();
		}
	}

}
