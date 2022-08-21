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

package net.markwalder.junit.mailserver.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Utility class for XML operations based on the W3C DOM API and Java API for XML Processing (JAXP).
 */
public class XMLUtils {

	/**
	 * Create a new empty XML document.
	 *
	 * @return New empty XML document
	 * @throws IOException if an I/O or XML error occurs
	 */
	public static Document createDocument() throws IOException {
		try {
			DocumentBuilder documentBuilder = createDocumentBuilder();
			return documentBuilder.newDocument();
		} catch (ParserConfigurationException e) {
			throw new IOException("XML I/O error", e);
		}
	}

	/**
	 * Parse the given XML stream and return an XML document.
	 *
	 * @param stream XML stream to parse
	 * @return XML document
	 * @throws IOException if an I/O or XML error occurs
	 */
	public static Document readDocument(InputStream stream) throws IOException {
		try {
			DocumentBuilder documentBuilder = createDocumentBuilder();
			return documentBuilder.parse(stream);
		} catch (SAXException | ParserConfigurationException e) {
			throw new IOException("XML I/O error", e);
		}
	}

	/**
	 * Write the given XML document to the given output stream.
	 *
	 * @param document XML document
	 * @param stream   Output stream
	 * @throws IOException if an I/O or XML error occurs
	 */
	public static void writeDocument(Document document, OutputStream stream) throws IOException {
		try {
			Transformer transformer = createTransformer();
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(stream);
			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw new IOException("XML I/O error", e);
		}
	}

	private static Transformer createTransformer() throws TransformerConfigurationException {

		TransformerFactory factory = TransformerFactory.newDefaultInstance();

		Transformer transformer = factory.newTransformer();
		transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

		return transformer;
	}

	private static DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
		factory.setNamespaceAware(false);
		factory.setValidating(false);

		// configure factory to prevent XXE attacks
		// see https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html

		try {
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		} catch (ParserConfigurationException e) {
			// ignore
		}

		try {
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		} catch (ParserConfigurationException e) {
			// ignore
		}

		try {
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		} catch (ParserConfigurationException e) {
			// ignore
		}

		try {
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		} catch (ParserConfigurationException e) {
			// ignore
		}

		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);

		return factory.newDocumentBuilder();
	}

}
