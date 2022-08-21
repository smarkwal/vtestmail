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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import net.markwalder.junit.mailserver.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Utility class to load and store mailboxes from/to XML streams.
 */
public class MailboxStoreUtils {

	/**
	 * Deserialize mailboxes from the given XML stream.
	 *
	 * @param xmlStream XML stream
	 * @return Mailbox store
	 * @throws IOException if an I/O or XML error occurs
	 */
	public static MailboxStore load(InputStream xmlStream) throws IOException {

		Document document = XMLUtils.readDocument(xmlStream);

		MailboxStore store = new MailboxStore();

		NodeList mailboxElements = document.getElementsByTagName("mailbox");
		for (int i = 0; i < mailboxElements.getLength(); i++) {
			Element mailboxElement = (Element) mailboxElements.item(i);

			String username = mailboxElement.getAttribute("username");
			String email = mailboxElement.getAttribute("email");
			String secret = mailboxElement.getAttribute("secret");
			Mailbox mailbox = new Mailbox(username, secret, email);
			store.addMailbox(mailbox);

			NodeList folderElements = mailboxElement.getElementsByTagName("folder");
			for (int j = 0; j < folderElements.getLength(); j++) {
				Element folderElement = (Element) folderElements.item(j);

				String name = folderElement.getAttribute("name");
				int uidNext = Integer.parseInt(folderElement.getAttribute("uidNext"));
				int uidValidity = Integer.parseInt(folderElement.getAttribute("uidValidity"));
				MailboxFolder folder = new MailboxFolder(name, uidNext, uidValidity);
				mailbox.addFolder(folder);

				NodeList messageElements = folderElement.getElementsByTagName("message");
				for (int k = 0; k < messageElements.getLength(); k++) {
					Element messageElement = (Element) messageElements.item(k);

					int uid = Integer.parseInt(messageElement.getAttribute("uid"));

					Element contentElement = (Element) messageElement.getElementsByTagName("content").item(0);
					String content = unescapeContent(contentElement.getTextContent());

					MailboxMessage message = new MailboxMessage(uid, content);
					folder.addMessage(message);

					NodeList flagElements = messageElement.getElementsByTagName("flag");
					for (int l = 0; l < flagElements.getLength(); l++) {
						Element flagElement = (Element) flagElements.item(l);
						String flag = flagElement.getTextContent();
						message.setFlag(flag);
					}

				}
			}
		}

		return store;
	}

	/**
	 * Serialize the given mailbox store to the given XML stream.
	 *
	 * @param store     Mailbox store
	 * @param xmlStream XML stream
	 * @throws IOException if an I/O or XML error occurs
	 */
	public static void store(MailboxStore store, OutputStream xmlStream) throws IOException {

		Document document = XMLUtils.createDocument();

		Element storeElement = document.createElement("store");
		document.appendChild(storeElement);
		storeElement.setAttribute("schema", "1.0.0");

		List<String> usernames = store.getUsernames();
		for (String username : usernames) {
			Element mailboxElement = document.createElement("mailbox");
			storeElement.appendChild(mailboxElement);

			Mailbox mailbox = store.getMailbox(username);
			mailboxElement.setAttribute("username", mailbox.getUsername());
			mailboxElement.setAttribute("secret", mailbox.getSecret());
			mailboxElement.setAttribute("email", mailbox.getEmail());

			List<String> folderNames = mailbox.getFolderNames();
			for (String folderName : folderNames) {
				Element folderElement = document.createElement("folder");
				mailboxElement.appendChild(folderElement);

				MailboxFolder folder = mailbox.getFolder(folderName);
				folderElement.setAttribute("name", folder.getName());
				folderElement.setAttribute("uidNext", String.valueOf(folder.getUIDNext()));
				folderElement.setAttribute("uidValidity", String.valueOf(folder.getUIDValidity()));

				List<MailboxMessage> messages = folder.getMessages();
				for (MailboxMessage message : messages) {
					int uid = message.getUID();
					List<String> flags = message.getFlags();
					String content = message.getContent();

					Element messageElement = document.createElement("message");
					folderElement.appendChild(messageElement);
					messageElement.setAttribute("uid", String.valueOf(uid));

					for (String flag : flags) {
						Element flagElement = document.createElement("flag");
						messageElement.appendChild(flagElement);
						flagElement.setTextContent(flag);
					}

					Element contentElement = document.createElement("content");
					messageElement.appendChild(contentElement);
					contentElement.setTextContent(escapeContent(content));

				}
			}
		}

		XMLUtils.writeDocument(document, xmlStream);
	}

	private static String escapeContent(String content) {
		// TODO: handle escape sequences if they appear in original content
		content = content.replace("\r\n", "[CRLF]");
		content = content.replace("\r", "[CR]");
		content = content.replace("\n", "[LF]");
		return content;
	}

	private static String unescapeContent(String content) {
		content = content.replace("[CRLF]", "\r\n");
		content = content.replace("[CR]", "\r");
		content = content.replace("[LF]", "\n");
		return content;
	}

}
