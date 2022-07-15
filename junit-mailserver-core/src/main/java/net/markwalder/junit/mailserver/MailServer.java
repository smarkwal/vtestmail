package net.markwalder.junit.mailserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ServerSocketFactory;
import net.markwalder.junit.mailserver.auth.Authenticator;
import net.markwalder.junit.mailserver.auth.CramMd5Authenticator;
import net.markwalder.junit.mailserver.auth.LoginAuthenticator;
import net.markwalder.junit.mailserver.auth.PlainAuthenticator;
import net.markwalder.junit.mailserver.auth.XOauth2Authenticator;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Skeleton for a simulated/virtual SMTP, IMAP, or POP3 server.
 */
@SuppressWarnings("unused")
abstract class MailServer {

	private final String protocol;
	protected final MailboxStore store;

	private boolean useSSL = false;
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
		addAuthenticator(AuthType.XOAUTH2, new XOauth2Authenticator());
	}

	public boolean isUseSSL() {
		return useSSL;
	}

	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}

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

	protected Authenticator getAuthenticator(String authType) {
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
			// TODO: use TLSv1.2 or TLSv1.3 instead of SSL? or make it configurable?
			factory = SSLUtils.createFactoryForSelfSignedCertificate("SSL", 2048, "localhost", Duration.ofDays(365));
		} else {
			factory = ServerSocketFactory.getDefault();
		}
		InetAddress localhost = InetAddress.getLoopbackAddress();
		serverSocket = factory.createServerSocket(0, 1, localhost);

		// start a new thread to handle client connections
		thread = new Thread(this::run);
		thread.setDaemon(true);
		thread.setName(protocol + "-server-localhost-" + getPort());
		thread.start();

		System.out.println(protocol + " server started");
	}

	public int getPort() {
		if (serverSocket == null) {
			throw new IllegalStateException(protocol + " server is not running");
		}
		return serverSocket.getLocalPort();
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

	protected void run() {

		stop.set(false);

		// while the server has not been stopped ...
		while (!stop.get()) {

			// wait for incoming connection
			System.out.println("Waiting for " + protocol + " connection on localhost:" + getPort() + (useSSL ? " (SSL)" : "") + " ...");
			try (Socket socket = serverSocket.accept()) {

				System.out.println(protocol + " connection from " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());

				// clear log (if there has been a previous connection)
				log.setLength(0);

				client = new Client(socket, log);

				// greet client
				handleCommand(null);

				// read and handle client commands
				while (true) {
					String command = client.readLine();
					if (command == null) {
						System.out.println(protocol + " client closed connection");
						break;
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

	protected abstract boolean handleCommand(String command) throws IOException;

	protected void reset() {

		// reset authentication state
		logout();
	}

	public String getLog() {
		return log.toString();
	}

}
