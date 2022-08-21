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
import net.markwalder.junit.mailserver.utils.Assert;

public class MailboxFolder {

	private final String name;

	private final List<MailboxMessage> messages = new ArrayList<>();

	// see https://datatracker.ietf.org/doc/html/rfc9051#section-2.3.1.1
	private int uidNext = 1;
	private int uidValidity = 1;

	MailboxFolder(String name) {
		Assert.isNotEmpty(name, "name");
		this.name = name;
	}

	MailboxFolder(String name, int uidNext, int uidValidity) {
		Assert.isNotEmpty(name, "name");
		Assert.isInRange(uidNext, 1, Integer.MAX_VALUE, "uidNext");
		Assert.isInRange(uidValidity, 1, Integer.MAX_VALUE, "uidValidity");
		this.name = name;
		this.uidNext = uidNext;
		this.uidValidity = uidValidity;
	}

	public String getName() {
		return name;
	}

	public List<MailboxMessage> getMessages() {
		synchronized (messages) {
			return new ArrayList<>(messages);
		}
	}

	public MailboxMessage addMessage(String content) {
		Assert.isNotEmpty(content, "content");
		synchronized (messages) {
			MailboxMessage message = new MailboxMessage(content);

			// auto-generate UID
			int uid = generateNextUID();
			message.setUID(uid);

			messages.add(message);
			return message;
		}
	}

	void addMessage(MailboxMessage message) {
		Assert.isNotNull(message, "message");
		synchronized (messages) {
			messages.add(message);

			// check if uidNext must be updated
			int uid = message.getUID();
			if (uid >= uidNext) {
				uidNext = uid + 1;
			}
		}
	}

	public void removeMessage(int messageNumber) {
		synchronized (messages) {
			Assert.isInRange(messageNumber, 1, messages.size(), "messageNumber");
			messages.remove(messageNumber - 1);
		}
	}

	public void removeDeletedMessages() {
		synchronized (messages) {
			messages.removeIf(MailboxMessage::isDeleted);
		}
	}

	public int getUIDNext() {
		return uidNext;
	}

	public void setUIDNext(int uidNext) {
		this.uidNext = uidNext;
	}

	private int generateNextUID() {
		return uidNext++;
	}

	public int getUIDValidity() {
		return uidValidity;
	}

	public void setUIDValidity(int uidValidity) {
		this.uidValidity = uidValidity;
	}

}
