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

package net.markwalder.junit.mailserver.core;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.net.Socket;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import net.markwalder.junit.mailserver.store.Mailbox;
import net.markwalder.junit.mailserver.store.MailboxProvider;
import net.markwalder.junit.mailserver.utils.Assert;
import net.markwalder.junit.mailserver.utils.DigestUtils;

public abstract class MailSession {

	private String serverAddress = null;
	private int serverPort = -1;
	private String clientAddress = null;
	private int clientPort = -1;
	private String sslProtocol = null;
	private String cipherSuite = null;

	private String authType = null;
	private String username = null;

	private volatile boolean closed = false;

	/**
	 * Session log with all messages exchanged between client and server.
	 */
	protected final StringBuilder log = new StringBuilder();

	void setSocketData(Socket socket) {
		Assert.isNotNull(socket, "socket");

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

	public boolean isEncrypted() {
		return sslProtocol != null;
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
		Assert.isNotEmpty(authType, "authType");
		Assert.isNotEmpty(username, "username");
		Assert.isNotNull(secret, "secret");
		Assert.isNotNull(store, "store");

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
		Assert.isNotEmpty(authType, "authType");
		Assert.isNotEmpty(username, "username");
		Assert.isNotEmpty(digest, "digest");
		Assert.isNotEmpty(timestamp, "timestamp");
		Assert.isNotNull(store, "store");

		if (isAuthenticated()) {
			throw new IllegalStateException("Already authenticated"); // TODO: return protocol error?
		}

		// check if mailbox exists (user is known)
		Mailbox mailbox = store.getMailbox(username);
		if (mailbox != null) {

			// compare password hash
			String data = timestamp + mailbox.getSecret();
			String hash = DigestUtils.md5Hex(data, ISO_8859_1);
			if (hash.equals(digest)) {

				// remember authenticated user
				this.authType = authType;
				this.username = username;
			}
		}
	}

	/**
	 * Mark this session as closed. This method is expected to be called in
	 * SMTP and POP3 commands like QUIT. The server will stop waiting for new
	 * commands and close the connection to the client.
	 */
	public void close() {
		closed = true;
	}

	/**
	 * Check if this session has been closed. If the server detects that the
	 * session has been closed, it should stop waiting for new commands and
	 * close the connection to the client.
	 *
	 * @return true if this session has been closed, false otherwise.
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * Wait until this session has been closed.
	 * This method is intended to be used in tests before checking any
	 * assertions which depend on the server-side completion of the session.
	 *
	 * @param timeout Maximum time to wait in milliseconds.
	 * @throws InterruptedException If the current thread has been interrupted
	 *                              or the timeout has been reached.
	 */
	@SuppressWarnings("BusyWait")
	public void waitUntilClosed(long timeout) throws InterruptedException {
		Assert.isInRange(timeout, 1, Long.MAX_VALUE, "timeout");

		// quick check if already closed
		if (closed) return;

		// remember start time
		long start = System.currentTimeMillis();

		while (!closed) {

			// check if timeout has been reached
			long time = System.currentTimeMillis();
			if (time - start >= timeout) {
				throw new InterruptedException("Timeout");
			}

			// sleep for a while
			Thread.sleep(100);
		}
	}

	/**
	 * Get session log.
	 *
	 * @return Session log.
	 */
	public String getLog() {
		return log.toString();
	}

}
