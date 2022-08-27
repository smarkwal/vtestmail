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

package net.markwalder.vtestmail.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.markwalder.vtestmail.utils.Assert;
import net.markwalder.vtestmail.utils.StringUtils;

public class MailboxStore implements MailboxProvider {

	private final Map<String, Mailbox> mailboxes = new TreeMap<>(StringUtils.CASE_INSENSITIVE_ORDER);

	public List<String> getUsernames() {
		synchronized (mailboxes) {
			return new ArrayList<>(mailboxes.keySet());
		}
	}

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

		// create mailbox with default INBOX folder
		Mailbox mailbox = new Mailbox(username, secret, email);
		mailbox.createFolder("INBOX");

		addMailbox(mailbox);
		return mailbox;
	}

	void addMailbox(Mailbox mailbox) {
		Assert.isNotNull(mailbox, "mailbox");
		synchronized (mailboxes) {
			mailboxes.put(mailbox.getUsername(), mailbox);
		}
	}

	public void deleteMailbox(String username) {
		Assert.isNotEmpty(username, "username");
		synchronized (mailboxes) {
			mailboxes.remove(username);
		}
	}

}
