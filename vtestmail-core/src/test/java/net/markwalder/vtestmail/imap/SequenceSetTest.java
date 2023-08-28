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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SequenceSetTest {

	@Test
	void contains() {

		// prepare
		SequenceSet set = new SequenceSet("1,*:12,2,8:7,4:5,10:10");

		// test & assert
		assertTrue(set.contains(1));
		assertTrue(set.contains(2));
		assertFalse(set.contains(3));
		assertTrue(set.contains(4));
		assertTrue(set.contains(5));
		assertFalse(set.contains(6));
		assertTrue(set.contains(7));
		assertTrue(set.contains(8));
		assertFalse(set.contains(9));
		assertTrue(set.contains(10));
		assertFalse(set.contains(11));
		assertTrue(set.contains(12));
		assertTrue(set.contains(13));
		assertTrue(set.contains(14));
		assertTrue(set.contains(Integer.MAX_VALUE));

	}

	@Test
	void test_toString() {

		// prepare
		SequenceSet set = new SequenceSet("1,*:12,2,8:7,4:5,10:10");

		// test
		String result = set.toString();

		// assert
		assertEquals("1,2,4:5,7:8,10:10,12:*", result);

	}

}