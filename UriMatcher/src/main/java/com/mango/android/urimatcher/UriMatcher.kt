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
import androidx.annotation.VisibleForTesting

@VisibleForTesting
internal fun String?.removePrefix(prefix: Char): String? {
    this ?: return null
    return if (length > 1 && this[0] == prefix) {
        substring(1)
    } else {
        this
    }
}

@VisibleForTesting
internal fun String?.isNumber(): Boolean {
    this ?: return false
    return !this.chars()
        .anyMatch { c -> (c < 48 || c > 57) }
}

private sealed class Node<T>(
    var code: T? = null
) {
    val children = mutableMapOf<String, Node<T>>()
}
private class NodeExact<T>(val text: String) : Node<T>()
private class NodeNumber<T> : Node<T>()
private class NodeText<T> : Node<T>()

private fun <T> createNode(token: String): Node<T> {
    return when (token) {
        "#" -> NodeNumber()
        "*" -> NodeText()
        else -> NodeExact(token)
    }
}

/**
 * This is similar to [android.content.UriMatcher]. However it doesn't support multiple authorities.
 */
class UriMatcher<T>(
    authority: String
) {
    private val root: Node<T> = NodeExact(authority)

    fun addURI(path: String?, code: T) {
        val tokens = path.removePrefix('/')
            ?.split("/")
            ?.dropLastWhile { it.isEmpty() }
            ?: emptyList()

        var node: Node<T> = root
        for (token in tokens) {
            node = node.children.getOrPut(token) {
                createNode(token)
            }
        }
        node.code = code
    }

    fun match(uri: Uri): T? {
        val pathSegments = uri.pathSegments

        if (pathSegments.size == 0 && uri.authority == null) return null

        if ((root as NodeExact<T>).text != uri.authority) return null

        var node: Node<T>? = root
        pathSegments.forEach { segment ->
            val children = node!!.children
//            val children = node?.children ?: return@forEach
//            if (children.isEmpty()) return@forEach

            node = null
            children.values.forEach inner@ { n ->
                when (n) {
                    is NodeExact -> {
                        if (n.text == segment) {
                            node = n
                        }
                    }
                    is NodeNumber -> {
                        if (segment.isNumber()) {
                            node = n
                        }
                    }
                    is NodeText -> {
                        node = n
                    }
                }
                if (node != null) return@inner
            }
            if (node == null) {
                return null
            }
        }

        return node!!.code
    }
}
