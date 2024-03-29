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

package net.markwalder.vtestmail.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.markwalder.vtestmail.utils.Assert;
import net.markwalder.vtestmail.utils.StringUtils;

public class Mailbox {

	private final String username;
	private final String secret;
	private final String email;

	public static final String INBOX = "INBOX";
	private final Map<String, MailboxFolder> folders = new TreeMap<>(FolderNameComparator.INSTANCE);

	Mailbox(String username, String secret, String email) {
		Assert.isNotEmpty(username, "username");
		Assert.isNotEmpty(secret, "secret");
		Assert.isNotEmpty(email, "email");

		this.username = username;
		this.secret = secret;
		this.email = email;
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
		MailboxFolder folder = new MailboxFolder(name);
		addFolder(folder);
		return folder;
	}

	void addFolder(MailboxFolder folder) {
		Assert.isNotNull(folder, "folder");
		String name = folder.getName();
		synchronized (folders) {
			Assert.isFalse(folders.containsKey(name), "Folder already exists: " + name);
			folders.put(name, folder);
		}
	}

	public void renameFolder(String oldName, String newName) {
		Assert.isNotEmpty(oldName, "oldName");
		Assert.isNotEmpty(newName, "newName");
		synchronized (folders) {
			Assert.isTrue(folders.containsKey(oldName), "Folder not found: " + oldName);
			Assert.isFalse(folders.containsKey(newName), "Folder already exists: " + newName);
			MailboxFolder folder = folders.remove(oldName);
			folder.setName(newName);
			folders.put(newName, folder);
		}
	}

	public void deleteFolder(String name) {
		Assert.isNotEmpty(name, "name");
		synchronized (folders) {
			Assert.isTrue(folders.containsKey(name), "Folder not found: " + name);
			folders.remove(name);
		}
	}

	/**
	 * Comparator for folder names.
	 * Prefers "INBOX" over other folders.
	 * Other folders are ordered alphabetically, case-insensitive.
	 * If two names only differ in case, they are ordered case-sensitive.
	 */
	private static class FolderNameComparator implements Comparator<String>, Serializable {

		private static final FolderNameComparator INSTANCE = new FolderNameComparator();

		private static final long serialVersionUID = 1L;

		@Override
		public int compare(String name1, String name2) {
			if (name1.equals(name2)) {
				return 0;
			} else if (name1.equals(INBOX)) {
				return -1;
			} else if (name2.equals(INBOX)) {
				return 1;
			}
			return StringUtils.CASE_INSENSITIVE_ORDER.compare(name1, name2);
		}
	}

}
