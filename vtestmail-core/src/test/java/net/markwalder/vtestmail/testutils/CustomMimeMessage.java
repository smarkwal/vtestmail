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

package net.markwalder.vtestmail.testutils;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import net.markwalder.vtestmail.utils.Assert;

/**
 * Subclass of {@link MimeMessage} that allows to set the {@code Message-ID} header.
 */
public class CustomMimeMessage extends MimeMessage {

	private String messageId = System.currentTimeMillis() + "@localhost";

	public CustomMimeMessage(Session session) {
		super(session);
	}

	public void setMessageId(String messageId) {
		Assert.isNotEmpty(messageId, "messageId");
		this.messageId = messageId;
	}

	@Override
	protected void updateMessageID() throws MessagingException {
		setHeader("Message-ID", "<" + messageId + ">");
	}

}
