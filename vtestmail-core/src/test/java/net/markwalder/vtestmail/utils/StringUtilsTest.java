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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StringUtilsTest {

	@Test
	void substringBefore() {
		assertThat(StringUtils.substringBefore("", "a")).isEqualTo("");
		assertThat(StringUtils.substringBefore("abc", "a")).isEqualTo("");
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
		assertThat(StringUtils.substringAfter("abc", "c")).isEqualTo("");
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
}