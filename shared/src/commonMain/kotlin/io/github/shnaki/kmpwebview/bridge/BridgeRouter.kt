package io.github.shnaki.kmpwebview.bridge

import io.github.shnaki.kmpwebview.bridge.model.NdefRecordDto
import io.github.shnaki.kmpwebview.bridge.model.LocationResult
import io.github.shnaki.kmpwebview.bridge.model.QrResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

/**
 * WebView からのメッセージを受け取り、適切な機能ポートへ委譲して応答 JSON を返す。
 *
 * - JSON のパース / シリアライズを一元管理
 * - BridgeException → 正規化されたエラーコードへ変換
 * - FeaturePorts のインターフェースに依存し、Android/iOS 実装には依存しない（テスト容易）
 *
 * @param qrScanner       QR コードスキャン実装
 * @param locationProvider 現在地取得実装
 * @param nfcReader       NFC 読み取り実装
 */
class BridgeRouter(
    private val qrScanner: QrScanner,
    private val locationProvider: LocationProvider,
    private val nfcReader: NfcReader
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * WebView から届いた JSON 文字列を処理し、応答 JSON 文字列を返す。
     * この関数は suspend なので任意のコルーチンから呼び出せる。
     */
    suspend fun handle(jsonString: String): String {
        val msg = try {
            json.decodeFromString<BridgeMessage>(jsonString)
        } catch (e: Exception) {
            return errorResponse("unknown", "PARSE_ERROR", "Failed to parse request: ${e.message}")
        }

        return try {
            dispatch(msg)
        } catch (e: BridgeException) {
            errorResponse(msg.id, e.code, e.message ?: "Unknown error")
        } catch (e: Exception) {
            errorResponse(msg.id, "INTERNAL_ERROR", e.message ?: "Unknown error")
        }
    }

    // ── dispatcher ────────────────────────────────────────────────────────────

    private suspend fun dispatch(msg: BridgeMessage): String = when (msg.feature) {
        "qr"  -> handleQr(msg)
        "gps" -> handleGps(msg)
        "nfc" -> handleNfc(msg)
        else  -> errorResponse(msg.id, "UNSUPPORTED", "Unknown feature: ${msg.feature}")
    }

    private suspend fun handleQr(msg: BridgeMessage): String {
        if (msg.action != "scan") return errorResponse(msg.id, "UNSUPPORTED", "Unknown action: ${msg.action}")
        val result: QrResult = qrScanner.scan()
        return successResponse(msg.id, json.encodeToJsonElement(result) as JsonObject)
    }

    private suspend fun handleGps(msg: BridgeMessage): String {
        if (msg.action != "getCurrent") return errorResponse(msg.id, "UNSUPPORTED", "Unknown action: ${msg.action}")
        val result: LocationResult = locationProvider.getCurrent()
        return successResponse(msg.id, json.encodeToJsonElement(result) as JsonObject)
    }

    private suspend fun handleNfc(msg: BridgeMessage): String {
        if (msg.action != "read") return errorResponse(msg.id, "UNSUPPORTED", "Unknown action: ${msg.action}")
        val records: List<NdefRecordDto> = nfcReader.read()
        val payload = buildJsonObject {
            put("records", json.encodeToJsonElement(records))
        }
        return successResponse(msg.id, payload)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun successResponse(id: String, payload: JsonObject): String =
        json.encodeToString(BridgeResponse(id = id, ok = true, payload = payload))

    private fun errorResponse(id: String, code: String, message: String): String =
        json.encodeToString(BridgeResponse(id = id, ok = false, error = BridgeError(code, message)))
}
