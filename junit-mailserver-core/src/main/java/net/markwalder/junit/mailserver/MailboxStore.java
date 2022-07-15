package net.markwalder.junit.mailserver;

import java.util.HashMap;
import java.util.Map;

public class MailboxStore {

	private final Map<String, Mailbox> mailboxes = new HashMap<>();

	public Mailbox getMailbox(String username) {
		return mailboxes.get(username);
	}

	public Mailbox findMailbox(String email) {
		for (Mailbox mailbox : mailboxes.values()) {
			if (mailbox.getEmail().equals(email)) {
				return mailbox;
			}
		}
		return null;
	}

	public Mailbox createMailbox(String username, String secret, String email) {
		Mailbox mailbox = new Mailbox(username, secret, email);
		mailboxes.put(username, mailbox);
		return mailbox;
	}

	public void deleteMailbox(String username) {
		mailboxes.remove(username);
	}

}
