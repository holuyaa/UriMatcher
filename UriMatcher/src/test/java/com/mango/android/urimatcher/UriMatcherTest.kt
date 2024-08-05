/*
* Copyright (C) 2024 The Android Open Source Project
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

package com.mango.android.urimatcher

import android.net.Uri
import androidx.core.net.toUri
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UriMatcherTest {

    @Test
    fun removePrefix() {
        assertThat("external/audio/media".removePrefix('/'))
            .isEqualTo("external/audio/media")
        assertThat("/external/audio/media".removePrefix('/'))
            .isEqualTo("external/audio/media")
        assertThat(null.removePrefix('/'))
            .isNull()
    }

    @Test
    fun isNumber() {
        assertThat("123".isNumber())
            .isTrue()
        assertThat("aaa".isNumber())
            .isFalse()
        assertThat("12a".isNumber())
            .isFalse()
        assertThat(null.isNumber())
            .isFalse()
    }

    @Test
    fun addURI() {
        val matcher = createUriMatcher {
            addURI("*/audio/media", 1)
        }
        assertThat(matcher.match(Uri.parse("content://media/external/audio/media")))
            .isEqualTo(1)
    }

    fun addURI_withPrefix() {
        val matcher = createUriMatcher {
            addURI("/*/audio/media", 1)
        }
        assertThat(matcher.match(Uri.parse("content://media/external/audio/media")))
            .isEqualTo(1)
    }

    fun addURI_withEmptySuffix() {
        val matcher = createUriMatcher {
            addURI("*/audio/media/", 1)
        }
        assertThat(matcher.match(Uri.parse("content://media/external/audio/media")))
            .isEqualTo(1)
    }

    @Test
    fun volumes() {
        assertThat(matcher.match(Uri.parse("content://media")))
            .isEqualTo(UriCode.Volumes)
        assertThat(matcher.match(Uri.parse("content://media/")))
            .isEqualTo(UriCode.Volumes)
    }

    private fun testMatcher(
        uriFormat: String,
        expectedCode: UriCode
    ) {
        assertThat(matcher.match(uriFormat.format("external").toUri()))
            .isEqualTo(expectedCode)
    }

    @Test
    fun volumesId() {
        val uriFormat = "content://media/%s"
        testMatcher(
            uriFormat = uriFormat,
            expectedCode = UriCode.VolumesId
        )
    }

    @Test
    fun version() {
        val uriFormat = "content://media/%s/version"
        testMatcher(
            uriFormat = uriFormat,
            expectedCode = UriCode.Version
        )
    }

    @Test
    fun audioMedia() {
        val uriFormat = "content://media/%s/audio/media"
        testMatcher(
            uriFormat = uriFormat,
            expectedCode = UriCode.AudioMedia
        )
    }

    @Test
    fun audioMediaId() {
        val uriFormat = "content://media/%s/audio/media/1"
        testMatcher(
            uriFormat = uriFormat,
            expectedCode = UriCode.AudioMediaId
        )
    }

    @Test
    fun audioMediaIdGenres() {
        val uriFormat = "content://media/%s/audio/media/1/genres"
        testMatcher(
            uriFormat = uriFormat,
            expectedCode = UriCode.AudioMediaIdGenres
        )
    }

    @Test
    fun audioMediaIdGenresId() {
        val uriFormat = "content://media/%s/audio/media/1/genres/2"
        testMatcher(
            uriFormat = uriFormat,
            expectedCode = UriCode.AudioMediaIdGenresId
        )
    }

    @Test
    fun wrongUri_notSupportedUri() {
        val uri = "content://media/external/video/media".toUri()
        assertThat(matcher.match(uri))
            .isNull()
    }

    @Test
    fun wrongUri_nullAuthority() {
        val uri = "content://".toUri()
        assertThat(matcher.match(uri))
            .isNull()
    }

    @Test
    fun wrongUri_otherAuthority() {
        val uri = "content://other/external".toUri()
        assertThat(matcher.match(uri))
            .isNull()
    }

    @Test
    fun wrongUri_emptyUri() {
        assertThat(matcher.match("".toUri()))
            .isNull()
    }

    companion object {
        private const val TEST_AUTHORITY = "media"

        enum class UriCode(
            val path: String?
        ) {
            Volumes(null),
            VolumesId("*"),
            Version("*/version"),
            AudioMedia("*/audio/media"),
            AudioMediaId("*/audio/media/#"),
            AudioMediaIdGenres("*/audio/media/#/genres"),
            AudioMediaIdGenresId("*/audio/media/#/genres/#"),
        }

        private val matcher = createUriMatcher {
            UriCode.entries.forEach { code ->
                addURI(path = code.path, code = code)
            }
        }

        private fun <T> createUriMatcher(
            authority: String = TEST_AUTHORITY,
            block: UriMatcher<T>.() -> Unit
        ): UriMatcher<T> {
            return UriMatcher<T>(authority).apply {
                block(this)
            }
        }
    }
}
