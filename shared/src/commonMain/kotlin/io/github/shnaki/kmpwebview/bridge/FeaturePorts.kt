package io.github.shnaki.kmpwebview.bridge

import io.github.shnaki.kmpwebview.bridge.model.LocationResult
import io.github.shnaki.kmpwebview.bridge.model.NdefRecordDto
import io.github.shnaki.kmpwebview.bridge.model.QrResult

/**
 * QR コードスキャン。
 * 実装は Android / iOS の各プラットフォームが提供し、BridgeRouter に注入する。
 */
interface QrScanner {
    /**
     * ネイティブスキャナーを起動してスキャン結果を返す。
     * @throws BridgeException CANCELLED / UNSUPPORTED / QR_ERROR
     */
    suspend fun scan(): QrResult
}

/**
 * 現在地取得。
 */
interface LocationProvider {
    /**
     * 現在の GPS 位置情報を返す。
     * @throws BridgeException PERMISSION_DENIED / LOCATION_UNAVAILABLE / LOCATION_ERROR
     */
    suspend fun getCurrent(): LocationResult
}

/**
 * NFC NDEF 読み取り。
 */
interface NfcReader {
    /**
     * NFC タグに含まれる NDEF レコード一覧を返す。
     * タグをかざすまで suspend する。
     * @throws BridgeException UNSUPPORTED / NFC_ERROR
     */
    suspend fun read(): List<NdefRecordDto>
}
