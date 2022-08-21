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

package net.markwalder.vtestmail.pop3;

import net.markwalder.vtestmail.core.MailException;

public class Pop3Exception extends MailException {

	public static Pop3Exception CommandNotImplemented() {
		return new Pop3Exception("Unknown command");
	}

	public static Pop3Exception CommandDisabled() {
		return new Pop3Exception("Disabled command");
	}

	public static Pop3Exception SyntaxError() {
		return new Pop3Exception("Syntax error");
	}

	public static Pop3Exception IllegalState(State state) {
		return new Pop3Exception("Command is not allowed in " + state.name() + " state");
	}

	public static Pop3Exception UnrecognizedAuthenticationType() {
		return new Pop3Exception("Unrecognized authentication type");
	}

	public static Pop3Exception AuthenticationFailed() {
		return new Pop3Exception("Authentication failed");
	}

	public static Pop3Exception UserCommandNotReceived() {
		return new Pop3Exception("USER command not received");
	}

	public static Pop3Exception MessageNotFound() {
		return new Pop3Exception("No such message");
	}

	public Pop3Exception(String message) {
		super(message);
	}

}
