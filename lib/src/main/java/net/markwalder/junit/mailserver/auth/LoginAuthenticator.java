package net.markwalder.junit.mailserver.auth;

import java.io.IOException;
import net.markwalder.junit.mailserver.Client;

public class LoginAuthenticator implements Authenticator {

	private static final String USERNAME_CHALLENGE = AuthUtils.encodeBase64("Username:");
	private static final String PASSWORD_CHALLENGE = AuthUtils.encodeBase64("Password:");

	@Override
	public Credentials authenticate(String parameters, Client client) throws IOException {

		// https://mailtrap.io/blog/smtp-auth/

		if (parameters != null) {
			// LOGIN does not accept parameters
			return null;
		}

		// ask client for username
		client.writeLine("334 " + USERNAME_CHALLENGE);
		String response = client.readLine();
		String username = AuthUtils.decodeBase64(response);
		if (username == null) {
			return null;
		}

		// ask client for password
		client.writeLine("334 " + PASSWORD_CHALLENGE);
		response = client.readLine();
		String password = AuthUtils.decodeBase64(response);
		if (password == null) {
			return null;
		}

		return new Credentials(username, password);
	}

}
