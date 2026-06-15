package io.github.shnaki.kmpwebview.bridge

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Web → Native へのリクエスト。
 *
 * JS 側からは以下の形式で送られる:
 * ```json
 * { "id": "abc123", "feature": "qr", "action": "scan", "payload": {} }
 * ```
 *
 * @param id      応答を突き合わせるための一意 ID（JS 側で採番）
 * @param feature 機能名: "qr" | "gps" | "nfc"
 * @param action  操作名: "scan" | "getCurrent" | "read"
 * @param payload 追加パラメータ（現 MVP では不使用）
 */
@Serializable
data class BridgeMessage(
    val id: String,
    val feature: String,
    val action: String,
    val payload: JsonObject = JsonObject(emptyMap())
)
