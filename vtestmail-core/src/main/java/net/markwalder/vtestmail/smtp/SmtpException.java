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

package net.markwalder.vtestmail.smtp;

import net.markwalder.vtestmail.core.MailException;

public class SmtpException extends MailException {

	public static SmtpException CommandNotImplemented() {
		// see https://datatracker.ietf.org/doc/html/rfc5321#section-4.2.4
		return new SmtpException("500", "5.5.1", "Command not implemented");
	}

	public static SmtpException CommandDisabled() {
		// see https://datatracker.ietf.org/doc/html/rfc5321#section-4.2.4
		return new SmtpException("502", "5.5.1", "Command disabled");
	}

	public static SmtpException SyntaxError() {
		return new SmtpException("501", "5.5.4", "Syntax error in parameters or arguments");
	}

	public static SmtpException AuthenticationRequired() {
		return new SmtpException("530", "5.7.0", "Authentication required");
	}

	public static SmtpException UnrecognizedAuthenticationType() {
		return new SmtpException("504", "5.5.4", "Unrecognized authentication type");
	}

	public static SmtpException AuthenticationFailed() {
		return new SmtpException("535", "5.7.8", "Authentication failed");
	}

	public SmtpException(String statusCode, String enhancedStatusCode, String message) {
		super(statusCode + " " + enhancedStatusCode + " " + message);
	}

}
