package net.markwalder.junit.mailserver.auth;

import java.io.IOException;
import net.markwalder.junit.mailserver.Client;
import org.apache.commons.lang3.StringUtils;

/**
 * Implementation of PLAIN authentication.
 */
public class PlainAuthenticator implements Authenticator {

	@Override
	public Credentials authenticate(String parameters, Client client) throws IOException {

		// https://www.rfc-editor.org/rfc/rfc4616.html
		// https://mailtrap.io/blog/smtp-auth/

		if (parameters == null) {
			// ask client for credentials
			client.writeLine("334");
			parameters = client.readLine();
		}

		// decode credentials
		String data = AuthUtils.decodeBase64(parameters);
		if (data == null) {
			return null;
		}

		// extract username and password
		String[] parts = StringUtils.split(data, '\u0000');
		if (parts.length < 3) {
			return null;
		}

		String username = parts[1];
		String password = parts[2];

		return new Credentials(username, password);
	}

}
