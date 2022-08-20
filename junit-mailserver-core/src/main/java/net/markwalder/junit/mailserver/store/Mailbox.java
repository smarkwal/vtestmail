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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.markwalder.junit.mailserver.utils.Assert;

public class Mailbox {

	private final String username;
	private final String secret;
	private final String email;

	public static final String INBOX = "INBOX";
	private final Map<String, MailboxFolder> folders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	public Mailbox(String username, String secret, String email) {
		Assert.isNotEmpty(username, "username");
		Assert.isNotEmpty(secret, "secret");
		Assert.isNotEmpty(email, "email");

		this.username = username;
		this.secret = secret;
		this.email = email;

		// create default INBOX folder
		createFolder(INBOX);
	}

	public String getUsername() {
		return username;
	}

	public String getSecret() {
		return secret;
	}

	public String getEmail() {
		return email;
	}

	public List<String> getFolderNames() {
		synchronized (folders) {
			return new ArrayList<>(folders.keySet());
		}
	}

	public MailboxFolder getInbox() {
		return getFolder(INBOX);
	}

	public MailboxFolder getFolder(String name) {
		Assert.isNotEmpty(name, "name");
		synchronized (folders) {
			return folders.get(name);
		}
	}

	public boolean hasFolder(String name) {
		Assert.isNotEmpty(name, "name");
		synchronized (folders) {
			return folders.containsKey(name);
		}
	}

	public MailboxFolder createFolder(String name) {
		Assert.isNotEmpty(name, "name");
		synchronized (folders) {
			Assert.isFalse(folders.containsKey(name), "Folder already exists: " + name);
			MailboxFolder folder = new MailboxFolder(name);
			folders.put(name, folder);
			return folder;
		}
	}

	public void deleteFolder(String name) {
		Assert.isNotEmpty(name, "name");
		synchronized (folders) {
			Assert.isTrue(folders.containsKey(name), "Folder not found: " + name);
			folders.remove(name);
		}
	}

}
