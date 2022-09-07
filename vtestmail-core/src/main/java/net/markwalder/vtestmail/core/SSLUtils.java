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

package net.markwalder.vtestmail.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509TrustManager;
import net.markwalder.vtestmail.utils.Assert;

class SSLUtils {

	private static final String KEYSTORE = "vtestmail.pfx";
	private static final String ALIAS = "localhost";
	private static final String PASSWORD = "changeit";

	private SSLUtils() {
		// utility class
	}

	static SSLSocketFactory createSSLSocketFactory(String protocol) throws IOException {
		SSLContext context = createSSLContext(protocol);
		return context.getSocketFactory();
	}

	static SSLServerSocketFactory createSSLServerSocketFactory(String protocol) throws IOException {
		SSLContext context = createSSLContext(protocol);
		return context.getServerSocketFactory();
	}

	static SSLContext createSSLContext(String protocol) throws IOException {
		Assert.isNotEmpty(protocol, "protocol");

		// TODO: support loading custom keystore and certificate

		try {

			// load keystore from resources
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			try (InputStream stream = SSLUtils.class.getClassLoader().getResourceAsStream(KEYSTORE)) {
				keyStore.load(stream, PASSWORD.toCharArray());
			}

			// get private key and self-signed certificate
			PrivateKey privateKey = (PrivateKey) keyStore.getKey(ALIAS, PASSWORD.toCharArray());
			Certificate[] certificateChain = keyStore.getCertificateChain(ALIAS);

			// create a key manager holding the private key and self-signed certificate
			KeyManager keyManager = new DummyKeyManager(ALIAS, privateKey, certificateChain);
			KeyManager[] keyManagers = new KeyManager[] { keyManager };

			// create a dummy trust manager accepting all clients
			TrustManager trustManager = new DummyTrustManager();
			TrustManager[] trustManagers = new TrustManager[] { trustManager };

			SSLContext context = SSLContext.getInstance(protocol);
			context.init(keyManagers, trustManagers, null);
			return context;

		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | KeyManagementException e) {
			throw new IOException("SSL initialization error", e);
		}

	}

	/**
	 * In-memory key manager containing a single private key and a self-signed certificate.
	 */
	static class DummyKeyManager extends X509ExtendedKeyManager {

		private final String alias;
		private final PrivateKey privateKey;
		private final X509Certificate[] certificateChain;

		public DummyKeyManager(String alias, PrivateKey privateKey, Certificate[] certificateChain) {
			Assert.isNotEmpty(alias, "alias");
			Assert.isNotNull(privateKey, "privateKey");
			Assert.isNotNull(certificateChain, "certificateChain");
			this.alias = alias;
			this.privateKey = privateKey;
			this.certificateChain = new X509Certificate[certificateChain.length];
			System.arraycopy(certificateChain, 0, this.certificateChain, 0, certificateChain.length);
		}

		@Override
		public String[] getClientAliases(String keyType, Principal[] issuers) {
			return null;
		}

		@Override
		public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
			return null;
		}

		@Override
		public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
			return null;
		}

		@Override
		public String[] getServerAliases(String keyType, Principal[] issuers) {
			return new String[] { alias };
		}

		@Override
		public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
			return alias;
		}

		@Override
		public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
			return alias;
		}

		@Override
		public X509Certificate[] getCertificateChain(String alias) {
			if (this.alias.equals(alias)) {
				return certificateChain;
			} else {
				return null;
			}
		}

		@Override
		public PrivateKey getPrivateKey(String alias) {
			if (this.alias.equals(alias)) {
				return privateKey;
			} else {
				return null;
			}
		}

	}

	static class DummyTrustManager implements X509TrustManager {

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) {
			// accept all clients
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) {
			// accept all servers
		}

	}

}
