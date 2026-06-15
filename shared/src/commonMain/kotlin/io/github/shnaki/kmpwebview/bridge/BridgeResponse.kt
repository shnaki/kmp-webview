package io.github.shnaki.kmpwebview.bridge

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Native → Web への応答。
 *
 * 成功:
 * ```json
 * { "id": "abc123", "ok": true, "payload": { ... } }
 * ```
 * 失敗:
 * ```json
 * { "id": "abc123", "ok": false, "error": { "code": "CANCELLED", "message": "..." } }
 * ```
 */
@Serializable
data class BridgeResponse(
    val id: String,
    val ok: Boolean,
    val payload: JsonObject? = null,
    val error: BridgeError? = null
)

@Serializable
data class BridgeError(
    val code: String,
    val message: String
)
