package net.markwalder.junit.mailserver.auth;

import java.io.IOException;
import net.markwalder.junit.mailserver.Client;

public interface Authenticator {

	Credentials authenticate(String parameters, Client client) throws IOException;

}
