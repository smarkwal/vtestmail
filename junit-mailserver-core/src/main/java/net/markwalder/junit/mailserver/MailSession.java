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

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import org.apache.commons.codec.digest.DigestUtils;

public class MailSession {

	private String serverAddress = null;
	private int serverPort = -1;
	private String clientAddress = null;
	private int clientPort = -1;
	private String sslProtocol = null;
	private String cipherSuite = null;

	private String authType = null;
	private String username = null;

	// TODO: keep session log

	void setSocketData(Socket socket) {

		// get server and client address and port
		serverAddress = socket.getLocalAddress().getHostAddress();
		serverPort = socket.getLocalPort();
		clientAddress = socket.getInetAddress().getHostAddress();
		clientPort = socket.getPort();

		// get SSL settings
		if (socket instanceof SSLSocket) {
			SSLSocket sslSocket = (SSLSocket) socket;
			SSLSession sslSession = sslSocket.getSession();
			sslProtocol = sslSession.getProtocol();
			cipherSuite = sslSession.getCipherSuite();
		}
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public int getServerPort() {
		return serverPort;
	}

	public String getClientAddress() {
		return clientAddress;
	}

	public int getClientPort() {
		return clientPort;
	}

	public String getSSLProtocol() {
		return sslProtocol;
	}

	public String getCipherSuite() {
		return cipherSuite;
	}

	public String getAuthType() {
		return authType;
	}

	public String getUsername() {
		return username;
	}

	public boolean isAuthenticated() {
		return username != null;
	}

	// TODO: move login methods somewhere else?

	public void login(String authType, String username, String secret, MailboxProvider store) {
		if (isAuthenticated()) {
			throw new IllegalStateException("Already authenticated"); // TODO: return protocol error?
		}

		// check if mailbox exists (user is known)
		Mailbox mailbox = store.getMailbox(username);
		if (mailbox != null) {

			// compare password
			if (mailbox.getSecret().equals(secret)) {

				// remember authenticated user
				this.authType = authType;
				this.username = username;
			}
		}
	}

	public void login(String authType, String username, String digest, String timestamp, MailboxProvider store) {
		if (isAuthenticated()) {
			throw new IllegalStateException("Already authenticated"); // TODO: return protocol error?
		}

		// check if mailbox exists (user is known)
		Mailbox mailbox = store.getMailbox(username);
		if (mailbox != null) {

			// compare password hash
			String data = timestamp + mailbox.getSecret();
			String hash = DigestUtils.md5Hex(data.getBytes(StandardCharsets.ISO_8859_1));
			if (hash.equals(digest)) {

				// remember authenticated user
				this.authType = authType;
				this.username = username;
			}
		}
	}

}
