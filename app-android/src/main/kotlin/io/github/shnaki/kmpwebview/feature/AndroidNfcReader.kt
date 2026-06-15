package io.github.shnaki.kmpwebview.feature

import android.app.Activity
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import io.github.shnaki.kmpwebview.bridge.BridgeException
import io.github.shnaki.kmpwebview.bridge.NfcReader
import io.github.shnaki.kmpwebview.bridge.model.NdefRecordDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * NfcAdapter.enableReaderMode を使った NDEF 読み取り実装。
 *
 * read() が呼ばれるとリーダーモードを開始し、タグがかざされるまで suspend する。
 * 読み取り完了後（または例外発生時）はリーダーモードを自動的に解除する。
 *
 * ライフサイクル注意:
 *   - enableReaderMode / disableReaderMode は Activity が RESUMED 状態で呼ぶこと。
 *   - onPause で Activity が止まった場合、コルーチンがキャンセルされると
 *     finally ブロックで disableReaderMode が呼ばれる。
 */
class AndroidNfcReader(private val activity: Activity) : NfcReader {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    /**
     * NFC タグがかざされるまで suspend し、NDEF レコードの一覧を返す。
     * enableReaderMode / disableReaderMode はメインスレッドで呼ぶ必要があるため、
     * Dispatchers.Main に切り替えて実行する。
     */
    override suspend fun read(): List<NdefRecordDto> = withContext(Dispatchers.Main) {
        if (nfcAdapter == null) {
            throw BridgeException("UNSUPPORTED", "NFC is not supported on this device")
        }
        if (!nfcAdapter.isEnabled) {
            throw BridgeException("UNSUPPORTED", "NFC is disabled. Please enable NFC in Settings.")
        }

        val deferred = CompletableDeferred<List<NdefRecordDto>>()

        val readerCallback = NfcAdapter.ReaderCallback { tag: Tag ->
            // NFC システムスレッドから呼ばれる
            val records = parseTag(tag)
            deferred.complete(records)
        }

        nfcAdapter.enableReaderMode(
            activity,
            readerCallback,
            NfcAdapter.FLAG_READER_NFC_A
                    or NfcAdapter.FLAG_READER_NFC_B
                    or NfcAdapter.FLAG_READER_NFC_F
                    or NfcAdapter.FLAG_READER_NFC_V
                    or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, // NDEF かどうかは自前でチェック
            null
        )

        try {
            deferred.await()
        } finally {
            nfcAdapter.disableReaderMode(activity)
        }
    }

    // ── NDEF パース ───────────────────────────────────────────────────────────

    private fun parseTag(tag: Tag): List<NdefRecordDto> {
        val ndef = Ndef.get(tag) ?: return emptyList()
        return try {
            ndef.connect()
            val ndefMessage = ndef.ndefMessage ?: return emptyList()
            ndefMessage.records.map { record -> record.toDto() }
        } catch (e: Exception) {
            emptyList()
        } finally {
            runCatching { ndef.close() }
        }
    }

    private fun NdefRecord.toDto(): NdefRecordDto {
        val typeName = when (tnf) {
            NdefRecord.TNF_WELL_KNOWN -> when {
                type.contentEquals(NdefRecord.RTD_TEXT) -> "T"
                type.contentEquals(NdefRecord.RTD_URI)  -> "U"
                else -> "WK:${type.decodeToString()}"
            }
            NdefRecord.TNF_MIME_MEDIA        -> type.decodeToString()
            NdefRecord.TNF_ABSOLUTE_URI      -> "URI"
            NdefRecord.TNF_EXTERNAL_TYPE     -> type.decodeToString()
            else -> tnf.toString()
        }
        val recordId = if (id.isNotEmpty()) id.decodeToString() else ""
        return NdefRecordDto(type = typeName, payload = decodePayload(), id = recordId)
    }

    private fun NdefRecord.decodePayload(): String = when {
        tnf == NdefRecord.TNF_WELL_KNOWN && type.contentEquals(NdefRecord.RTD_TEXT) -> {
            // RTD_TEXT: payload[0] のbit7=エンコード(0=UTF-8,1=UTF-16), bit5-0=言語コード長
            val statusByte = payload[0].toInt()
            val isUtf16 = statusByte and 0x80 != 0
            val langLen = statusByte and 0x3F
            val charset = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8
            String(payload, 1 + langLen, payload.size - 1 - langLen, charset)
        }
        tnf == NdefRecord.TNF_WELL_KNOWN && type.contentEquals(NdefRecord.RTD_URI) -> {
            // RTD_URI: toUri() で完全 URI を組み立て
            toUri()?.toString() ?: String(payload, Charsets.UTF_8)
        }
        else -> String(payload, Charsets.UTF_8)
    }
}
