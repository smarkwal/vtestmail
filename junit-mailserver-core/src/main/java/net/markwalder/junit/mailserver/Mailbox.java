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

package net.markwalder.junit.mailserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.markwalder.junit.mailserver.utils.Assert;

public class Mailbox {

	private final String username;
	private final String secret;
	private final String email;
	private final List<Message> messages = new ArrayList<>();

	// see https://datatracker.ietf.org/doc/html/rfc9051#section-2.3.1.1
	private int uidNext = 1000000001;
	private int uidValidity = 1000000000;

	public Mailbox(String username, String secret, String email) {
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

	public List<Message> getMessages() {
		synchronized (messages) {
			return new ArrayList<>(messages);
		}
	}

	public void addMessage(String content) {
		Assert.isNotEmpty(content, "content");
		synchronized (messages) {
			Message message = new Message(content);
			message.setUID(getNextUID());
			messages.add(message);
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
			messages.removeIf(Message::isDeleted);
		}
	}

	private int getNextUID() {
		return uidNext++;
	}

	public static class Message {

		private static final String CRLF = "\r\n";

		public static final String FLAG_SEEN = "\\Seen";
		public static final String FLAG_ANSWERED = "\\Answered";
		public static final String FLAG_FLAGGED = "\\Flagged";
		public static final String FLAG_DELETED = "\\Deleted";
		public static final String FLAG_DRAFT = "\\Draft";
		public static final String FLAG_RECENT = "\\Recent"; // this flag was in use in IMAP4rev1 and is now deprecated

		public static final String KEYWORD_FORWARDED = "$Forwarded";
		public static final String KEYWORD_MDNSENT = "$MDNSent";
		public static final String KEYWORD_JUNK = "$Junk";
		public static final String KEYWORD_NOTJUNK = "$NotJunk";
		public static final String KEYWORD_PHISHING = "$Phishing";

		private final String content;

		private int uid;

		// see https://datatracker.ietf.org/doc/html/rfc9051#section-2.3.2
		// TODO: a flag can be permanent or session-only on a per-flag basis.
		private final Set<String> flags = new TreeSet<>();

		// see https://datatracker.ietf.org/doc/html/rfc9051#section-2.3.3
		// TODO: implement internal date
		private long internalDate;

		public Message(String content) {
			Assert.isNotEmpty(content, "content");
			this.content = content;
			this.uid = Math.abs(content.hashCode());
		}

		public String getContent() {
			return content;
		}

		public int getUID() {
			return uid;
		}

		public void setUID(int uid) {
			this.uid = uid;
		}

		/**
		 * Get size of message in octets.
		 * Also known as RFC822.SIZE in IMAP.
		 *
		 * @return Size of message in octets.
		 */
		public int getSize() {
			// see https://datatracker.ietf.org/doc/html/rfc9051#section-2.3.4
			// TODO: return number of bytes with UTF-8 encoding?
			return content.length();
		}

		// flags ---------------------------------------------------------------

		public List<String> getFlags() {
			synchronized (flags) {
				return new ArrayList<>(flags);
			}
		}

		public boolean hasFlag(String flag) {
			Assert.isNotEmpty(flag, "flag");
			synchronized (flags) {
				return flags.contains(flag);
			}
		}

		public void setFlag(String flag) {
			Assert.isNotEmpty(flag, "flag");
			synchronized (flags) {

				// keywords $Junk and $NotJunk are mutually exclusive
				if (flag.equals(KEYWORD_JUNK)) {
					flags.remove(KEYWORD_NOTJUNK);
				} else if (flag.equals(KEYWORD_NOTJUNK)) {
					flags.remove(KEYWORD_JUNK);
				}

				flags.add(flag);
			}
		}

		public void removeFlag(String flag) {
			Assert.isNotEmpty(flag, "flag");
			synchronized (flags) {
				flags.remove(flag);
			}
		}

		// special methods to get/set flags ------------------------------------

		public boolean isSeen() {
			return hasFlag(FLAG_SEEN);
		}

		public void setSeen(boolean seen) {
			if (seen) {
				setFlag(FLAG_SEEN);
			} else {
				removeFlag(FLAG_SEEN);
			}
		}

		public boolean isAnswered() {
			return hasFlag(FLAG_ANSWERED);
		}

		public void setAnswered(boolean answered) {
			if (answered) {
				setFlag(FLAG_ANSWERED);
			} else {
				removeFlag(FLAG_ANSWERED);
			}
		}

		public boolean isFlagged() {
			return hasFlag(FLAG_FLAGGED);
		}

		public void setFlagged(boolean flagged) {
			if (flagged) {
				setFlag(FLAG_FLAGGED);
			} else {
				removeFlag(FLAG_FLAGGED);
			}
		}

		public boolean isDeleted() {
			return hasFlag(FLAG_DELETED);
		}

		public void setDeleted(boolean deleted) {
			if (deleted) {
				setFlag(FLAG_DELETED);
			} else {
				removeFlag(FLAG_DELETED);
			}
		}

		public boolean isDraft() {
			return hasFlag(FLAG_DRAFT);
		}

		public void setDraft(boolean draft) {
			if (draft) {
				setFlag(FLAG_DRAFT);
			} else {
				removeFlag(FLAG_DRAFT);
			}
		}

		public boolean isRecent() {
			return hasFlag(FLAG_RECENT);
		}

		public void setRecent(boolean recent) {
			if (recent) {
				setFlag(FLAG_RECENT);
			} else {
				removeFlag(FLAG_RECENT);
			}
		}

		/**
		 * Get the first n lines of the message, or the complete message if
		 * n is greater than the total number of lines.
		 *
		 * @param n Number of lines.
		 * @return The first n lines of the message.
		 */
		public String getTop(int n) {

			// split message into lines
			String[] lines = content.split(CRLF, -1);

			// add all headers
			boolean headers = true;

			StringBuilder buffer = new StringBuilder();
			for (int i = 0; i < lines.length; i++) {

				String line = lines[i];

				if (headers) {
					if (line.isEmpty()) {
						// empty line found -> end of headers
						headers = false;
						if (n == 0) {
							// no lines requested -> return only headers
							break;
						}
					}
				} else {
					// countdown of body lines
					if (n-- <= 0) {
						break;
					}
				}

				if (i > 0) {
					buffer.append(CRLF);
				}
				buffer.append(line);
			}

			return buffer.toString();
		}

	}

}
