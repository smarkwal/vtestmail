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

package net.markwalder.vtestmail.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class StringUtils {

	public static Comparator<String> CASE_INSENSITIVE_ORDER = new CaseInsensitiveStringComparator();

	public static String substringBefore(String value, String separator) {
		Assert.isNotNull(value, "value");
		Assert.isNotEmpty(separator, "separator");
		int pos = value.indexOf(separator);
		if (pos < 0) {
			return value;
		}
		return value.substring(0, pos);
	}

	public static String substringAfter(String value, String separator) {
		Assert.isNotNull(value, "value");
		Assert.isNotEmpty(separator, "separator");
		int pos = value.indexOf(separator);
		if (pos < 0) {
			return null;
		}
		return value.substring(pos + separator.length());
	}

	public static String substringBetween(String value, String open, String close) {
		Assert.isNotNull(value, "value");
		Assert.isNotEmpty(open, "open");
		Assert.isNotEmpty(close, "close");
		int start = value.indexOf(open);
		if (start < 0) {
			return null;
		}
		int end = value.indexOf(close, start + open.length());
		if (end < 0) {
			return null;
		}
		return value.substring(start + open.length(), end);
	}

	public static String[] split(String value, String separator) {
		return split(value, separator, Integer.MAX_VALUE);
	}

	public static String[] split(String value, String separator, int limit) {
		Assert.isNotNull(value, "value");
		Assert.isNotNull(separator, "separator");
		Assert.isInRange(limit, 2, Integer.MAX_VALUE, "limit");

		List<String> parts = new ArrayList<>();

		int start = 0;
		while (parts.size() < limit - 1) {

			// try to find next separator
			int end = value.indexOf(separator, start);
			if (end < 0) {
				break;
			}

			// add substring up to separator
			String part = value.substring(start, end);
			parts.add(part);

			// continue search after separator
			start = end + separator.length();
		}

		// add remaining value
		String part = value.substring(start);
		parts.add(part);

		// return array
		return parts.toArray(new String[0]);
	}

	public static String join(Collection<String> values, String separator) {
		StringBuilder buffer = new StringBuilder();
		boolean first = true;
		for (String value : values) {
			if (first) {
				first = false;
			} else {
				buffer.append(separator);
			}
			buffer.append(value);
		}
		return buffer.toString();
	}

	private static class CaseInsensitiveStringComparator implements Comparator<String>, Serializable {

		private static final long serialVersionUID = 1L;

		private CaseInsensitiveStringComparator() {
		}

		@Override
		public int compare(String string1, String string2) {
			int diff = string1.compareToIgnoreCase(string2);
			if (diff == 0) {
				diff = string1.compareTo(string2);
			}
			return diff;
		}

	}

}
