package net.markwalder.junit.mailserver.auth;

import java.io.IOException;
import net.markwalder.junit.mailserver.Client;
import org.apache.commons.lang3.RandomStringUtils;

public class CramMd5Authenticator implements Authenticator {

	@Override
	public Credentials authenticate(String parameters, Client client) throws IOException {

		// https://mailtrap.io/blog/smtp-auth/

		if (parameters != null) {
			// CRAM-MD5 does not accept parameters
			return null;
		}

		// send random challenge to client
		String challenge = RandomStringUtils.randomAlphanumeric(9);
		client.writeLine("334 " + AuthUtils.encodeBase64(challenge));

		// read response from client
		String response = client.readLine();

		// decode credentials
		String data = AuthUtils.decodeBase64(response);
		if (data == null) {
			return null;
		}

		// TODO: implement
		return null;
	}

}
