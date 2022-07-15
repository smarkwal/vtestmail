package net.markwalder.junit.mailserver.auth;

import net.markwalder.junit.mailserver.Client;
import org.apache.commons.lang3.StringUtils;

public class XOauth2Authenticator implements Authenticator {

	@Override
	public Credentials authenticate(String parameters, Client client) {

		// https://developers.google.com/gmail/imap/xoauth2-protocol

		if (parameters == null) {
			// XOAUTH2 requires parameters
			return null;
		}

		// decode credentials
		String data = AuthUtils.decodeBase64(parameters);
		if (data == null) {
			return null;
		}

		// remove trailing 0x01 characters
		data = data.trim();

		// extract username and access token
		String username = StringUtils.substringBetween(data, "user=", "\u0001auth=");
		String accessToken = StringUtils.substringAfter(data, "auth=Bearer ");

		return new Credentials(username, accessToken);
	}

}
