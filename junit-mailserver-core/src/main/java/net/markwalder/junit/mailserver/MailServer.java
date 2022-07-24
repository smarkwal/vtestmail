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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Security;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import net.markwalder.junit.mailserver.auth.Authenticator;
import net.markwalder.junit.mailserver.auth.CramMd5Authenticator;
import net.markwalder.junit.mailserver.auth.DigestMd5Authenticator;
import net.markwalder.junit.mailserver.auth.LoginAuthenticator;
import net.markwalder.junit.mailserver.auth.PlainAuthenticator;
import net.markwalder.junit.mailserver.auth.XOauth2Authenticator;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Skeleton for a simulated/virtual SMTP, IMAP, or POP3 server.
 */
@SuppressWarnings("unused")
public abstract class MailServer implements AutoCloseable {

	static {

		// enable SSLv3, TLSv1 and TLSv1.1
		Security.setProperty("jdk.tls.disabledAlgorithms", "");

		// enable Jakarta Mail API debug output
		// System.setProperty("mail.socket.debug", "true");
	}

	private final String protocol;
	protected final MailboxStore store;

	private int port = 0; // 0 = select a free port
	private boolean useSSL = false;
	private String sslProtocol = "TLSv1.2";
	private boolean authenticationRequired = false;
	//TODO: private boolean encryptionRequired = false;

	/**
	 * Registered authenticators.
	 */
	private final Map<String, Authenticator> authenticators = new HashMap<>();

	/**
	 * List of supported authentication types.
	 */
	private final LinkedList<String> authTypes = new LinkedList<>();

	/**
	 * Username of currently authenticated user.
	 */
	private String username = null;

	private ServerSocket serverSocket;
	private Thread thread;
	protected Client client;

	/**
	 * Flag used to tell the worker thread to stop processing new connections.
	 */
	private final AtomicBoolean stop = new AtomicBoolean(false);

	/**
	 * Communication log with all incoming and outgoing messages.
	 */
	private final StringBuilder log = new StringBuilder();

	protected MailServer(String protocol, MailboxStore store) {
		if (protocol == null) throw new IllegalArgumentException("protocol must not be null");
		if (store == null) throw new IllegalArgumentException("store must not be null");
		this.protocol = protocol;
		this.store = store;

		addAuthenticator(AuthType.LOGIN, new LoginAuthenticator());
		addAuthenticator(AuthType.PLAIN, new PlainAuthenticator());
		addAuthenticator(AuthType.CRAM_MD5, new CramMd5Authenticator());
		addAuthenticator(AuthType.DIGEST_MD5, new DigestMd5Authenticator());
		addAuthenticator(AuthType.XOAUTH2, new XOauth2Authenticator());
	}

	public MailboxStore getStore() {
		return store;
	}

	public boolean isUseSSL() {
		return useSSL;
	}

	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}

	public String getSSLProtocol() {
		return sslProtocol;
	}

	public void setSSLProtocol(String sslProtocol) {
		if (sslProtocol == null || sslProtocol.isEmpty()) throw new IllegalArgumentException("sslProtocol must not be null or empty");
		this.sslProtocol = sslProtocol;
	}

	// TODO: set cipher suites?

	public boolean isAuthenticationRequired() {
		return authenticationRequired && username == null;
	}

	public void setAuthenticationRequired(boolean authenticationRequired) {
		this.authenticationRequired = authenticationRequired;
	}

	public List<String> getAuthTypes() {
		return new ArrayList<>(authTypes);
	}

	public void setAuthTypes(String... authTypes) {
		this.authTypes.clear();
		for (String authType : authTypes) {
			addAuthType(authType);
		}
	}

	public void addAuthType(String authType) {
		if (!authenticators.containsKey(authType)) {
			throw new IllegalArgumentException("Authenticator not found: " + authType);
		}
		authTypes.remove(authType); // remove (if present)
		authTypes.add(authType); // add to end of list
	}

	public void removeAuthType(String authType) {
		authTypes.remove(authType);
	}

	public boolean isAuthTypeSupported(String authType) {
		return authTypes.contains(authType);
	}

	public Authenticator getAuthenticator(String authType) {
		return authenticators.get(authType);
	}

	protected void addAuthenticator(String authType, Authenticator authenticator) {
		this.authenticators.put(authType, authenticator);
	}

	protected void login(String username, String secret) {
		this.username = null;
		Mailbox mailbox = store.getMailbox(username);
		if (mailbox != null && mailbox.getSecret().equals(secret)) {
			this.username = username;
		}
	}

	protected void login(String username, String digest, String timestamp) {
		this.username = null;
		Mailbox mailbox = store.getMailbox(username);
		if (mailbox != null) {
			String hash = DigestUtils.md5Hex(timestamp + mailbox.getSecret());
			if (hash.equals(digest)) {
				this.username = username;
			}
		}
	}

	protected void logout() {
		username = null;
	}

	public boolean isAuthenticated() {
		return username != null;
	}

	public String getUsername() {
		return username;
	}

	public void start() throws IOException {
		System.out.println("Starting " + protocol + " server ...");

		// open a server socket on a free port
		ServerSocketFactory factory;
		if (useSSL) {
			factory = SSLUtils.createFactoryForSelfSignedCertificate(sslProtocol, 2048, "localhost", Duration.ofDays(365));
		} else {
			factory = ServerSocketFactory.getDefault();
		}
		InetAddress localhost = InetAddress.getLoopbackAddress();
		serverSocket = factory.createServerSocket(port, 1, localhost);

		if (serverSocket instanceof SSLServerSocket) {
			SSLServerSocket sslServerSocket = (SSLServerSocket) serverSocket;

			// disable all other SSL/TLS protocols
			sslServerSocket.setEnabledProtocols(new String[] { sslProtocol });

			// TODO: enable/disable cipher suites?
			// sslServerSocket.setEnabledCipherSuites(...);
		}

		// start a new thread to handle client connections
		thread = new Thread(this::run);
		thread.setDaemon(true);
		thread.setName(protocol + "-server-localhost-" + getPort());
		thread.start();

		System.out.println(protocol + " server started");
	}

	/**
	 * Get the port the server is listening on.
	 * If this method is called after the server has been started, the method
	 * will return the actual port the server is listening on.
	 * If this method is called before the server has been started, the method
	 * will return the port that has been configured by calling
	 * {@link #setPort(int)}.
	 *
	 * @return Port the server is listening on.
	 */
	public int getPort() {

		// if server is running, return the actual port
		if (serverSocket != null && serverSocket.isBound()) {
			return serverSocket.getLocalPort();
		}

		// return the configured port
		return port;
	}

	/**
	 * Set the port to use for the server.
	 * If set to {@code 0}, the server will find and use a free port.
	 * Note that this method has no immediate effect if the server has already
	 * been started. It will only take effect when the server is restarted.
	 *
	 * @param port Port to use for the server.
	 */
	public void setPort(int port) {
		if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("port must be between 0 and 65535");
		}
		if (serverSocket != null) {
			// TODO: close and reopen server socket?
		}
		this.port = port;
	}

	public void stop() throws IOException {
		System.out.println("Stopping " + protocol + " server ...");

		// signal thread to stop
		stop.set(true);

		// close server socket
		if (serverSocket != null) {
			serverSocket.close();
			serverSocket = null;
		}

		// wait for thread to have stopped (max 5 seconds)
		if (thread != null) {
			thread.interrupt();
			try {
				thread.join(5 * 1000);
			} catch (InterruptedException e) {
				// ignore
			}
			thread = null;
		}

		System.out.println(protocol + " server stopped");
	}

	@Override
	public void close() throws IOException {
		stop();
	}

	protected void run() {

		stop.set(false);

		// while the server has not been stopped ...
		while (!stop.get()) {

			// wait for incoming connection
			System.out.println("Waiting for " + protocol + " connection on localhost:" + getPort() + (useSSL ? " (" + sslProtocol + ")" : "") + " ...");

			// if (serverSocket instanceof SSLServerSocket) {
			// 	SSLServerSocket sslSocket = (SSLServerSocket) serverSocket;
			// 	System.out.println("Protocols: " + Arrays.toString(sslSocket.getEnabledProtocols()));
			// 	System.out.println("Cipher suites: " + Arrays.toString(sslSocket.getEnabledCipherSuites()));
			// }

			try (Socket socket = serverSocket.accept()) {

				String clientInfo = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
				if (socket instanceof SSLSocket) {
					SSLSocket sslSocket = (SSLSocket) socket;
					SSLSession sslSession = sslSocket.getSession();
					String sslProtocol = sslSession.getProtocol();
					String cipherSuite = sslSession.getCipherSuite();
					// TODO: make information available for assertions
					clientInfo += " (" + sslProtocol + ", " + cipherSuite + ")";
				}
				System.out.println(protocol + " connection from " + clientInfo);

				// clear log (if there has been a previous connection)
				log.setLength(0);

				client = new Client(socket, log);

				// greet client
				handleNewClient();

				// read and handle client commands
				while (true) {
					String command = client.readLine();
					if (command == null) {
						System.out.println(protocol + " client closed connection");
						break;
					} else if (command.isEmpty()) {
						// TODO: how should an empty line be handled?
						//  (sent after failed authentication)
					}
					boolean quit = handleCommand(command);
					if (quit) break;
				}

			} catch (IOException e) {

				if (!stop.get()) { // ignore exception if server has been stopped
					System.err.println("Unexpected " + protocol + " I/O error:");
					e.printStackTrace();
				}

			} finally {

				client = null;

			}

		}

	}

	protected abstract void handleNewClient() throws IOException;

	protected abstract boolean handleCommand(String command) throws IOException;

	protected void reset() {

		// reset authentication state
		logout();
	}

	public String getLog() {
		return log.toString();
	}

}
