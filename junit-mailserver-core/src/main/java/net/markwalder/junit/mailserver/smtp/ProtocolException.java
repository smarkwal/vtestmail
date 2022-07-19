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

public class ProtocolException extends Exception {

	public static ProtocolException AuthenticationRequired() {
		return new ProtocolException("530", "5.7.0", "Authentication required");
	}

	public static ProtocolException UnrecognizedAuthenticationType() {
		return new ProtocolException("504", "5.5.4", "Unrecognized authentication type");
	}

	public static ProtocolException AuthenticationFailed() {
		return new ProtocolException("535", "5.7.8", "Authentication failed");
	}

	public static ProtocolException SyntaxError() {
		return new ProtocolException("501", "5.5.4", "Syntax error in parameters or arguments");
	}

	public ProtocolException(String statusCode, String enhancedStatusCode, String message) {
		super(statusCode + " " + enhancedStatusCode + " " + message);
	}

}
