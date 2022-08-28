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

package net.markwalder.vtestmail.imap;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import net.markwalder.vtestmail.store.MailboxFolder;
import net.markwalder.vtestmail.store.MailboxMessage;
import net.markwalder.vtestmail.utils.Assert;
import net.markwalder.vtestmail.utils.StringUtils;

public class STORE extends ImapCommand {

	private final String sequenceSet;
	private final String messageDataItemName;
	private final String messageDataItemValue;

	public STORE(String sequenceSet, String messageDataItemName, String messageDataItemValue) {
		Assert.isNotEmpty(sequenceSet, "sequenceSet");
		Assert.isNotEmpty(messageDataItemName, "messageDataItemName");
		Assert.isNotEmpty(messageDataItemValue, "messageDataItemValue");
		this.sequenceSet = sequenceSet;
		this.messageDataItemName = messageDataItemName;
		this.messageDataItemValue = messageDataItemValue;
	}

	public static STORE parse(String parameters) throws ImapException {
		isNotEmpty(parameters);

		// "STORE" SP sequence-set SP store-att-flags

		ImapCommandParser parser = new ImapCommandParser(parameters);
		String sequenceSet = parser.readSequenceSet();
		parser.assertMoreArguments();
		String[] storeAttFlags = parser.readStoreAttFlags();
		parser.assertNoMoreArguments();

		String messageDataItemName = storeAttFlags[0];
		String messageDataItemValue = storeAttFlags[1];

		isNotEmpty(sequenceSet);
		isNotEmpty(messageDataItemName);
		isNotEmpty(messageDataItemValue);

		return new STORE(sequenceSet, messageDataItemName, messageDataItemValue);
	}

	@Override
	public String toString() {
		return "STORE " + sequenceSet + " " + messageDataItemName + " " + messageDataItemValue;
	}

	@Override
	protected void execute(ImapServer server, ImapSession session, ImapClient client, String tag) throws IOException, ImapException {

		// see https://datatracker.ietf.org/doc/html/rfc9051#section-6.4.6

		// The STORE command alters data associated with a message in the
		// mailbox. Normally, STORE will return the updated value of the data
		// with an untagged FETCH response. A suffix of ".SILENT" in the data
		// item name prevents the untagged FETCH, and the server SHOULD assume
		// that the client has determined the updated value itself or does not
		// care about the updated value.

		// The currently defined data items that can be stored are:
		//
		//   FLAGS <flag list>
		//      Replace the flags for the message with the argument.  The new
		//      value of the flags is returned as if a FETCH of those flags was
		//      done.
		//
		//   FLAGS.SILENT <flag list>
		//      Equivalent to FLAGS, but without returning a new value.
		//
		//   +FLAGS <flag list>
		//      Add the argument to the flags for the message.  The new value of
		//      the flags is returned as if a FETCH of those flags was done.
		//
		//   +FLAGS.SILENT <flag list>
		//      Equivalent to +FLAGS, but without returning a new value.
		//
		//   -FLAGS <flag list>
		//      Remove the argument from the flags for the message.  The new value
		//      of the flags is returned as if a FETCH of those flags was done.
		//
		//   -FLAGS.SILENT <flag list>
		//      Equivalent to -FLAGS, but without returning a new value.

		// Example:
		//
		//     C: A003 STORE 2:4 +FLAGS (\Deleted)
		//     S: * 2 FETCH (FLAGS (\Deleted \Seen))
		//     S: * 3 FETCH (FLAGS (\Deleted))
		//     S: * 4 FETCH (FLAGS (\Deleted \Flagged \Seen))
		//     S: A003 OK STORE completed

		session.assertState(State.Selected);
		session.assertReadWrite();

		SequenceSet sequenceSet = new SequenceSet(this.sequenceSet);

		MailboxFolder folder = session.getFolder();
		List<MailboxMessage> messages = folder.getMessages();

		String operation = messageDataItemName;
		boolean silent = false;
		if (operation.endsWith(".SILENT")) {
			operation = operation.substring(0, operation.length() - 7);
			silent = true;
		}

		final String[] flags;
		if (messageDataItemValue.equals("()")) {
			flags = new String[0]; // empty flag list
		} else if (messageDataItemValue.startsWith("(") && messageDataItemValue.endsWith(")")) {
			String list = messageDataItemValue.substring(1, messageDataItemValue.length() - 1);
			flags = StringUtils.split(list, " ");
		} else {
			flags = StringUtils.split(messageDataItemValue, " ");
		}

		Consumer<MailboxMessage> action;
		switch (operation) {
			case "FLAGS":
				action = message -> setFlags(message, flags);
				break;
			case "+FLAGS":
				action = message -> addFlags(message, flags);
				break;
			case "-FLAGS":
				action = message -> removeFlags(message, flags);
				break;
			default:
				throw ImapException.SyntaxError();
		}

		for (int i = 0; i < messages.size(); i++) {
			MailboxMessage message = messages.get(i);
			int messageNumber = i + 1;
			if (sequenceSet.contains(messageNumber)) {
				action.accept(message);
				if (!silent) {
					client.writeLine("* " + messageNumber + " FETCH (FLAGS (" + StringUtils.join(message.getFlags(), " ") + "))");
				}
			}
		}

		client.writeLine(tag + " OK STORE completed");
	}

	private static void setFlags(MailboxMessage message, String[] flags) {
		// remove all flags
		for (String flag : message.getFlags()) {
			message.removeFlag(flag);
		}
		// add new flags
		for (String flag : flags) {
			message.setFlag(flag);
		}
	}

	private static void addFlags(MailboxMessage message, String[] flags) {
		for (String flag : flags) {
			if (!message.hasFlag(flag)) {
				message.setFlag(flag);
			}
		}
	}

	private static void removeFlags(MailboxMessage message, String[] flags) {
		for (String flag : flags) {
			if (message.hasFlag(flag)) {
				message.removeFlag(flag);
			}
		}
	}

}
