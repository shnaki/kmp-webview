package io.github.shnaki.kmpwebview.bridge.model

import kotlinx.serialization.Serializable

/**
 * NDEFレコード1件のDTO。
 *
 * @param type  レコード種別を表す短縮文字列。
 *              "T" = RTD_TEXT, "U" = RTD_URI, MIME型の文字列 など。
 * @param payload デコード済みのペイロード文字列（テキストまたは URI）。
 * @param id    レコードID（多くのタグでは空文字）。
 */
@Serializable
data class NdefRecordDto(
    val type: String,
    val payload: String,
    val id: String = ""
)
