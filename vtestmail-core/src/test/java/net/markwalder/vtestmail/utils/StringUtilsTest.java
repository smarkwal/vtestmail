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

package net.markwalder.vtestmail.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class StringUtilsTest {

	@Test
	void substringBefore() {
		assertThat(StringUtils.substringBefore("", "a")).isEmpty();
		assertThat(StringUtils.substringBefore("abc", "a")).isEmpty();
		assertThat(StringUtils.substringBefore("abc", "b")).isEqualTo("a");
		assertThat(StringUtils.substringBefore("abc", "c")).isEqualTo("ab");
		assertThat(StringUtils.substringBefore("abc", "d")).isEqualTo("abc");

		assertThat(StringUtils.substringBefore("UIDL", " ")).isEqualTo("UIDL");
		assertThat(StringUtils.substringBefore("USER john", " ")).isEqualTo("USER");
		assertThat(StringUtils.substringBefore("AUTH XOAUTH2 abc", " ")).isEqualTo("AUTH");
	}

	@Test
	void substringAfter() {
		assertThat(StringUtils.substringAfter("", "a")).isNull();
		assertThat(StringUtils.substringAfter("abc", "a")).isEqualTo("bc");
		assertThat(StringUtils.substringAfter("abc", "b")).isEqualTo("c");
		assertThat(StringUtils.substringAfter("abc", "c")).isEmpty();
		assertThat(StringUtils.substringAfter("abc", "d")).isNull();

		assertThat(StringUtils.substringAfter("UIDL", " ")).isNull();
		assertThat(StringUtils.substringAfter("USER john", " ")).isEqualTo("john");
		assertThat(StringUtils.substringAfter("AUTH XOAUTH2 abc", " ")).isEqualTo("XOAUTH2 abc");
	}

	@Test
	void substringBetween() {
		assertThat(StringUtils.substringBetween("", "<", ">")).isNull();
		assertThat(StringUtils.substringBetween("abc", "<", ">")).isNull();
		assertThat(StringUtils.substringBetween("a<bc", "<", ">")).isNull();
		assertThat(StringUtils.substringBetween("ab>c", "<", ">")).isNull();
		assertThat(StringUtils.substringBetween("a>b<c", "<", ">")).isNull();
		assertThat(StringUtils.substringBetween("a<b>c", "<", ">")).isEqualTo("b");
		assertThat(StringUtils.substringBetween("<abc>", "<", ">")).isEqualTo("abc");

		assertThat(StringUtils.substringBetween("MAIL bob@localhost", "<", ">")).isNull();
		assertThat(StringUtils.substringBetween("MAIL FROM:<alice@localhost>", "<", ">")).isEqualTo("alice@localhost");
	}

	@Test
	void split() {
		assertThat(StringUtils.split("", ",")).containsExactly("");
		assertThat(StringUtils.split("1,2,3", ",")).containsExactly("1", "2", "3");
		assertThat(StringUtils.split("1\r\n2\r\n\r\n3\r\n", "\r\n")).containsExactly("1", "2", "", "3", "");
	}

	@Test
	void split_withLimit() {
		assertThat(StringUtils.split("1\r\n2\r\n\r\n3\r\n", "\r\n", 2)).containsExactly("1", "2\r\n\r\n3\r\n");
		assertThat(StringUtils.split("1\r\n2\r\n\r\n3\r\n", "\r\n", 3)).containsExactly("1", "2", "\r\n3\r\n");
		assertThat(StringUtils.split("1\r\n2\r\n\r\n3\r\n", "\r\n", 4)).containsExactly("1", "2", "", "3\r\n");
		assertThat(StringUtils.split("1\r\n2\r\n\r\n3\r\n", "\r\n", 5)).containsExactly("1", "2", "", "3", "");
		assertThat(StringUtils.split("1\r\n2\r\n\r\n3\r\n", "\r\n", 6)).containsExactly("1", "2", "", "3", "");
	}

	@Test
	void join() {
		assertThat(StringUtils.join(List.of(), ",")).isEmpty();
		assertThat(StringUtils.join(List.of("A"), ",")).isEqualTo("A");
		assertThat(StringUtils.join(List.of("A", "B"), ",")).isEqualTo("A,B");
		assertThat(StringUtils.join(List.of("A", "B", "C"), ",")).isEqualTo("A,B,C");
		assertThat(StringUtils.join(List.of("A", "", "C"), ",")).isEqualTo("A,,C");
		assertThat(StringUtils.join(List.of("", "", ""), ",")).isEqualTo(",,");
		assertThat(StringUtils.join(List.of("", "", ""), "\r\n")).isEqualTo("\r\n\r\n");
	}

	@Test
	@SuppressWarnings("EqualsWithItself")
	void comparator() {
		assertThat(StringUtils.CASE_INSENSITIVE_ORDER.compare("AA", "AA")).isZero();
		assertThat(StringUtils.CASE_INSENSITIVE_ORDER.compare("aa", "aa")).isZero();
		assertThat(StringUtils.CASE_INSENSITIVE_ORDER.compare("aa", "AA")).isPositive();
		assertThat(StringUtils.CASE_INSENSITIVE_ORDER.compare("AA", "aa")).isNegative();

		assertThat(StringUtils.CASE_INSENSITIVE_ORDER.compare("AA", "BB")).isNegative();
		assertThat(StringUtils.CASE_INSENSITIVE_ORDER.compare("AA", "bb")).isNegative();
		assertThat(StringUtils.CASE_INSENSITIVE_ORDER.compare("aa", "BB")).isNegative();
		assertThat(StringUtils.CASE_INSENSITIVE_ORDER.compare("aa", "bb")).isNegative();

		assertThat(StringUtils.CASE_INSENSITIVE_ORDER.compare("BB", "AA")).isPositive();
		assertThat(StringUtils.CASE_INSENSITIVE_ORDER.compare("BB", "aa")).isPositive();
		assertThat(StringUtils.CASE_INSENSITIVE_ORDER.compare("bb", "AA")).isPositive();
		assertThat(StringUtils.CASE_INSENSITIVE_ORDER.compare("bb", "aa")).isPositive();
	}

}