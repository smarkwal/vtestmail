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
import java.math.BigInteger;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Date;
import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509TrustManager;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

class SSLUtils {

	private static final String KEY_ALGORITHM = "RSA";
	private static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";

	public static ServerSocketFactory createFactoryForSelfSignedCertificate(String protocol, int keySize, String domain, Duration duration) throws IOException {

		// make sure that Bouncy Castle crypto provider is installed
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}

		try {

			// create a new RSA key pair
			KeyPair keyPair = createKeyPair(keySize);

			// create a self-signed certificate for the given key pair
			X509Certificate certificate = createSelfSignedCertificate(keyPair, domain, duration);

			// use a key manager holding the private key and self-signed certificate
			KeyManager keyManager = new DummyKeyManager("default", keyPair.getPrivate(), certificate);
			KeyManager[] keyManagers = new KeyManager[] { keyManager };

			// use a dummy trust manager accepting all clients
			TrustManager trustManager = new DummyTrustManager();
			TrustManager[] trustManagers = new TrustManager[] { trustManager };

			SSLContext context = SSLContext.getInstance(protocol);
			context.init(keyManagers, trustManagers, null);
			return context.getServerSocketFactory();

		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | OperatorCreationException e) {
			throw new IOException("SSL initialization error", e);
		}

	}

	private static KeyPair createKeyPair(int keySize) throws NoSuchAlgorithmException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
		keyPairGenerator.initialize(keySize);
		return keyPairGenerator.generateKeyPair();
	}

	private static X509Certificate createSelfSignedCertificate(KeyPair keyPair, String domain, Duration duration) throws OperatorCreationException, CertificateException {

		PrivateKey privateKey = keyPair.getPrivate();
		ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).build(privateKey);

		X500Name dnName = new X500Name("cn=" + domain);
		BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
		Date notBefore = new Date();
		Date notAfter = new Date(notBefore.getTime() + duration.toMillis());
		PublicKey publicKey = keyPair.getPublic();
		JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(dnName, serial, notBefore, notAfter, dnName, publicKey);
		X509CertificateHolder certificateHolder = certificateBuilder.build(signer);

		JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
		converter.setProvider(BouncyCastleProvider.PROVIDER_NAME);

		return converter.getCertificate(certificateHolder);

	}

	/**
	 * In-memory key manager containing a single private key and a self-signed certificate.
	 */
	static class DummyKeyManager extends X509ExtendedKeyManager {

		private final String alias;
		private final PrivateKey privateKey;
		private final X509Certificate certificate;

		public DummyKeyManager(String alias, PrivateKey privateKey, X509Certificate certificate) {
			if (alias == null || alias.isEmpty()) throw new IllegalArgumentException("alias must not be null or empty");
			if (privateKey == null) throw new IllegalArgumentException("privateKey must not be null");
			if (certificate == null) throw new IllegalArgumentException("certificate must not be null");
			this.alias = alias;
			this.privateKey = privateKey;
			this.certificate = certificate;
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
				return new X509Certificate[] { certificate };
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
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) {
		}

	}

}
