package net.markwalder.junit.mailserver.auth;

public class Credentials {

	private final String username;
	private final String secret;

	public Credentials(String username, String secret) {
		if (username == null) throw new IllegalArgumentException("username must not be null");
		if (secret == null) throw new IllegalArgumentException("secret must not be null");
		this.username = username;
		this.secret = secret;
	}

	public String getUsername() {
		return username;
	}

	public String getSecret() {
		return secret;
	}

}
