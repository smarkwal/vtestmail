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

public class SmtpTransaction {

	private String sender;
	private final List<String> recipients = new ArrayList<>();
	private String data;

	public String getSender() {
		return sender;
	}

	void setSender(String sender) {
		this.sender = sender;
	}

	public List<String> getRecipients() {
		return Collections.unmodifiableList(recipients);
	}

	void addRecipient(String recipient) {
		recipients.add(recipient);
	}

	public String getData() {
		return data;
	}

	void setData(String data) {
		this.data = data;
	}

}
