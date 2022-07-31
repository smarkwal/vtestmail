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

package net.markwalder.junit.mailserver.pop3;

public class ProtocolException extends Exception {

	public static ProtocolException MessageNotFound() {
		return new ProtocolException("No such message");
	}

	public static ProtocolException IllegalState(State state) {
		return new ProtocolException("Command is not allowed in " + state.name() + " state");
	}

	public static ProtocolException UnrecognizedAuthenticationType() {
		return new ProtocolException("Unrecognized authentication type");
	}

	public static ProtocolException AuthenticationFailed() {
		return new ProtocolException("Authentication failed");
	}

	public static ProtocolException SyntaxError() {
		return new ProtocolException("Syntax error");
	}

	public ProtocolException(String message) {
		super(message);
	}
}
