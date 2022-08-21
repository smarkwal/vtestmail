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

package net.markwalder.junit.mailserver.testutils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import net.markwalder.junit.mailserver.utils.Assert;

public class TestUtils {

	public static Clock createTestClock() {
		return createTestClock(2020, 1, 1, 0, 0, 0);
	}

	public static Clock createTestClock(int year, int month, int day, int hour, int minute, int second) {
		LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute, second);
		Instant instant = dateTime.toInstant(ZoneOffset.UTC);
		return Clock.fixed(instant, ZoneId.of("UTC"));
	}

	public static String readResource(String resource) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (InputStream in = openResource(resource)) {
			byte[] buffer = new byte[1024];
			while (true) {
				int len = in.read(buffer);
				if (len < 0) {
					break;
				}
				out.write(buffer, 0, len);
			}
		}
		return out.toString(StandardCharsets.UTF_8);
	}

	public static InputStream openResource(String resource) throws IOException {
		Assert.isNotNull(resource, "resource");
		InputStream stream = TestUtils.class.getClassLoader().getResourceAsStream(resource);
		if (stream == null) {
			throw new IOException("Resource not found: " + resource);
		}
		return stream;
	}

}
