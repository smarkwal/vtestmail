/*
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

package net.markwalder.vtestmail.imap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.markwalder.vtestmail.utils.Assert;
import net.markwalder.vtestmail.utils.StringUtils;

public class SequenceSet {

	private final List<SequenceElement> elements = new ArrayList<>();

	public SequenceSet(String sequenceSet) {
		Assert.isNotEmpty(sequenceSet, "sequenceSet");
		String[] parts = StringUtils.split(sequenceSet, ",");
		for (String part : parts) {
			if (part.contains(":")) {
				elements.add(new SequenceRange(part));
			} else {
				int number = Integer.parseInt(part);
				elements.add(new SequenceNumber(number));
			}
		}
		Collections.sort(elements);
	}

	public boolean contains(int messageNumber) {
		Assert.isInRange(messageNumber, 1, Integer.MAX_VALUE, "messageNumber");
		for (SequenceElement element : elements) {
			if (element.contains(messageNumber)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		for (SequenceElement element : elements) {
			if (buffer.length() > 0) {
				buffer.append(",");
			}
			buffer.append(element.toString());
		}
		return buffer.toString();
	}

	private abstract static class SequenceElement implements Comparable<SequenceElement> {

		protected abstract boolean contains(int messageNumber);

		@Override
		public abstract String toString();

	}

	private static class SequenceNumber extends SequenceElement {

		private final int number;

		private SequenceNumber(int number) {
			Assert.isInRange(number, 1, Integer.MAX_VALUE, "number");
			this.number = number;
		}

		@Override
		protected boolean contains(int messageNumber) {
			return messageNumber == number;
		}

		@Override
		public String toString() {
			return String.valueOf(number);
		}

		@Override
		public int compareTo(SequenceElement element) {
			if (element instanceof SequenceNumber) {
				SequenceNumber number = (SequenceNumber) element;
				return this.number - number.number;
			}
			SequenceRange range = (SequenceRange) element;
			return this.number - range.start;
		}

	}

	private static class SequenceRange extends SequenceElement {

		private final int start;
		private final int end;

		private SequenceRange(String range) {
			Assert.isNotEmpty(range, "range");

			// split range into start and end
			int pos = range.indexOf(':');
			if (pos == -1) {
				throw new IllegalArgumentException("Invalid range: " + range);
			}
			String from = range.substring(0, pos);
			String to = range.substring(pos + 1);

			// parse start
			int start;
			if (from.equals("*")) {
				start = Integer.MAX_VALUE;
			} else {
				start = Integer.parseInt(from);
			}

			// parse end
			int end;
			if (to.equals("*")) {
				end = Integer.MAX_VALUE;
			} else {
				end = Integer.parseInt(to);
			}

			// switch if start > end
			if (start > end) {
				int tmp = start;
				start = end;
				end = tmp;
			}

			this.start = start;
			this.end = end;
		}

		@Override
		protected boolean contains(int messageNumber) {
			return messageNumber >= start && messageNumber <= end;
		}

		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder();
			if (start == Integer.MAX_VALUE) {
				buffer.append("*");
			} else {
				buffer.append(start);
			}
			buffer.append(":");
			if (end == Integer.MAX_VALUE) {
				buffer.append("*");
			} else {
				buffer.append(end);
			}
			return buffer.toString();
		}

		@Override
		public int compareTo(SequenceElement element) {
			if (element instanceof SequenceNumber) {
				SequenceNumber number = (SequenceNumber) element;
				return this.start - number.number;
			}
			SequenceRange range = (SequenceRange) element;
			int diff = this.start - range.start;
			if (diff != 0) return diff;
			return this.end - range.end;
		}

	}

}
