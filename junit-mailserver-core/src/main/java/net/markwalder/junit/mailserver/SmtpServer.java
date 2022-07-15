package net.markwalder.junit.mailserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.markwalder.junit.mailserver.auth.Authenticator;
import net.markwalder.junit.mailserver.auth.Credentials;
import org.apache.commons.lang3.StringUtils;

/**
 * Virtual SMTP server for testing.
 * <p>
 * Limitations:
 * <ul>
 *     <li>All SMTP commands must be in uppercase.</li>
 *     <li>Only one client can connect to the server at a time.</li>
 *     <li>Support for SSL/TLS sockets and STARTTLS command is not implemented.</li>
 *     <li>The format of email addresses and messages is not validated.</li>
 *     <li>Server does not add a "Received" header to messages.</li>
 *     <li>Messages are not queued or relayed to another SMTP server.</li>
 *     <li>Messages are either delivered to a local mailbox, or silently discarded.</li>
 * </ul>
 */
public class SmtpServer extends MailServer {

	public SmtpServer(MailboxStore store) {
		super("SMTP", store);
	}

	private final List<String> recipients = new ArrayList<>();

	@Override
	protected void reset() {

		// discard all recipients
		recipients.clear();

		super.reset();
	}

	@Override
	protected boolean handleCommand(String command) throws IOException {

		// TODO: commands are case-insensitive

		boolean quit = false;
		if (command == null) {
			handleNewClient();
		} else if (command.equals("HELO") || command.startsWith("HELO ")) {
			handleHELO();
		} else if (command.equals("EHLO") || command.startsWith("EHLO ")) {
			handleEHLO();
		} else if (command.startsWith("STARTTLS")) {
			handleStartTLS();
		} else if (command.startsWith("AUTH ")) {
			handleAuth(command);
		} else if (command.startsWith("MAIL FROM:")) {
			handleMailFrom(command);
		} else if (command.startsWith("RCPT TO:")) {
			handleRcptTo(command);
		} else if (command.equals("DATA")) {
			handleData();
		} else if (command.equals("RSET")) {
			handleRset();
		} else if (command.equals("NOOP")) {
			handleNoop();
		} else if (command.equals("QUIT")) {
			handleQuit();
			quit = true;
		} else {
			handleUnknownCommand();
		}

		return quit;
	}

	private void handleNewClient() throws IOException {
		// send initial server greeting
		client.writeLine("220 localhost Service ready");
	}

	private void handleHELO() throws IOException {
		// nothing to do
		client.writeLine("250 OK");
	}

	private void handleEHLO() throws IOException {

		// support TLS
		client.writeLine("250-STARTTLS");

		// supported authentication types
		List<String> authTypes = getAuthTypes();
		if (authTypes.size() > 0) {
			client.writeLine("250-AUTH " + String.join(" ", authTypes));
		}

		// support enhanced status codes (ESMPT)
		client.writeLine("250-ENHANCEDSTATUSCODES");

		client.writeLine("250 OK");
	}

	private void handleStartTLS() throws IOException {
		client.writeLine("220 Ready to start TLS");
		// TODO: implement STARTTLS
	}

	private void handleAuth(String command) throws IOException {

		// reset authentication state
		logout();

		// https://datatracker.ietf.org/doc/html/rfc4954
		// https://www.iana.org/assignments/sasl-mechanisms/sasl-mechanisms.xhtml
		// https://datatracker.ietf.org/doc/html/rfc5248

		// split command into SMTP verb ("AUTH"), auth type, and optional parameters
		String[] parts = StringUtils.split(command, " ", 3);
		String authType = parts[1];
		String parameters = parts.length > 2 ? parts[2] : null;

		// check if authentication type is supported
		if (!isAuthTypeSupported(authType)) {
			client.writeLine("504 5.5.4 Unrecognized authentication type");
			return;
		}

		// get user credentials from client
		Authenticator authenticator = getAuthenticator(authType);
		Credentials credentials = authenticator.authenticate(parameters, client);
		if (credentials == null) {
			client.writeLine("535 5.7.8 Authentication failed");
			return;
		}

		// try to authenticate user
		String username = credentials.getUsername();
		String secret = credentials.getSecret();
		login(username, secret);

		if (isAuthenticated()) {
			client.writeLine("235 2.7.0 Authentication succeeded");
		} else {
			client.writeLine("535 5.7.8 Authentication failed");
		}
	}

	private void handleMailFrom(String command) throws IOException {
		if (isAuthenticationRequired()) {
			client.writeLine("530 5.7.0 Authentication required");
			return;
		}

		String email = StringUtils.substringBetween(command, "<", ">");
		if (email == null) {
			client.writeLine("501 5.5.4 Syntax error in parameters or arguments");
			return;
		}

		// note: email address is not validated

		client.writeLine("250 2.1.0 OK");
	}

	private void handleRcptTo(String command) throws IOException {
		if (isAuthenticationRequired()) {
			client.writeLine("530 5.7.0 Authentication required");
			return;
		}

		String email = StringUtils.substringBetween(command, "<", ">");
		if (email == null) {
			client.writeLine("501 5.5.4 Syntax error in parameters or arguments");
			return;
		}

		// remember email address of recipient
		// note: email address is not validated
		recipients.add(email);

		client.writeLine("250 2.1.5 OK");
	}

	private void handleData() throws IOException {

		if (isAuthenticationRequired()) {
			client.writeLine("530 5.7.0 Authentication required");
			return;
		}

		client.writeLine("354 Send message, end with <CRLF>.<CRLF>");

		// read message until a line with only a dot is received
		StringBuilder message = new StringBuilder();
		while (true) {

			String line = client.readLine();
			if (line.equals(".")) {
				// end of message detected
				break;
			}

			// remove leading dot (if present)
			if (line.startsWith(".")) {
				line = line.substring(1);
			}

			// add line to message
			if (message.length() > 0) {
				message.append("\r\n");
			}
			message.append(line);
		}

		// add message to all mailboxes of known recipients
		for (String email : recipients) {
			Mailbox mailbox = store.findMailbox(email);
			if (mailbox != null) {
				mailbox.addMessage(message.toString());
			}
		}

		client.writeLine("250 2.6.0 Message accepted");
	}

	private void handleRset() throws IOException {
		reset();
		client.writeLine("250 OK");
	}

	private void handleNoop() throws IOException {
		client.writeLine("250 OK");
	}

	private void handleQuit() throws IOException {
		client.writeLine("221 2.0.0 Goodbye");
	}

	private void handleUnknownCommand() throws IOException {
		client.writeLine("502 5.5.1 Command not implemented");
	}

}
