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

package net.markwalder.junit.mailserver.store;

import java.util.HashMap;
import java.util.Map;
import net.markwalder.junit.mailserver.MailboxProvider;
import net.markwalder.junit.mailserver.utils.Assert;

public class MailboxStore implements MailboxProvider {

	private final Map<String, Mailbox> mailboxes = new HashMap<>();

	@Override
	public Mailbox getMailbox(String username) {
		Assert.isNotEmpty(username, "username");
		synchronized (mailboxes) {
			return mailboxes.get(username);
		}
	}

	public Mailbox findMailbox(String email) {
		Assert.isNotEmpty(email, "email");
		synchronized (mailboxes) {
			for (Mailbox mailbox : mailboxes.values()) {
				if (mailbox.getEmail().equals(email)) {
					return mailbox;
				}
			}
			return null;
		}
	}

	public Mailbox createMailbox(String username, String secret, String email) {
		Mailbox mailbox = new Mailbox(username, secret, email);
		synchronized (mailboxes) {
			mailboxes.put(username, mailbox);
		}
		return mailbox;
	}

	public void deleteMailbox(String username) {
		Assert.isNotEmpty(username, "username");
		synchronized (mailboxes) {
			mailboxes.remove(username);
		}
	}

}
