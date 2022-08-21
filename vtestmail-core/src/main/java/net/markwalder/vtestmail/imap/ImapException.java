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

import net.markwalder.vtestmail.core.MailException;
import net.markwalder.vtestmail.utils.Assert;

public class ImapException extends MailException {

	public static ImapException CommandNotImplemented() {
		return new ImapException("BAD", "Command not implemented");
	}

	public static ImapException CommandDisabled() {
		return new ImapException("BAD", "Command disabled");
	}

	public static ImapException SyntaxError() {
		return new ImapException("BAD", "Syntax error");
	}

	public static ImapException UnrecognizedAuthenticationType() {
		return new ImapException("BAD", "Unrecognized authentication type"); // TODO: BAD or NO?
	}

	public static ImapException AuthenticationFailed() {
		return new ImapException("NO", "AUTHENTICATIONFAILED", "Authentication failed");
	}

	public static ImapException IllegalState(State state) {
		return new ImapException("BAD", "Command is not allowed in " + state.name() + " state");
	}

	public static ImapException LoginNotAllowed() {
		return new ImapException("NO", "LOGIN not allowed");
	}

	public static ImapException MailboxNotFound() {
		return new ImapException("NO", "TRYCREATE", "No such mailbox");
	}

	public static ImapException MailboxIsReadOnly() {
		return new ImapException("NO", "READ-ONLY", "Mailbox is read-only");
	}

	public static ImapException MailboxAlreadyExists() {
		return new ImapException("NO", "Mailbox already exists");
	}

	public static ImapException MailboxNotDeleted() {
		return new ImapException("NO", "Mailbox not deleted");
	}

	public static ImapException MailboxHasChildren() {
		return new ImapException("NO", "HASCHILDREN", "Mailbox has inferior hierarchical names");
	}

	public ImapException(String response, String message) {
		super(response + " " + message);
		Assert.isNotEmpty(response, "response");
		Assert.isNotEmpty(message, "message");
	}

	public ImapException(String response, String code, String message) {
		super(response + " [" + code + "] " + message);
		Assert.isNotEmpty(response, "response");
		Assert.isNotEmpty(code, "code");
		Assert.isNotEmpty(message, "message");
	}

}
