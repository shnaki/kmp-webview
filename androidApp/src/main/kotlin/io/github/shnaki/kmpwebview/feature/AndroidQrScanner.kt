package io.github.shnaki.kmpwebview.feature

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import io.github.shnaki.kmpwebview.bridge.BridgeException
import io.github.shnaki.kmpwebview.bridge.QrScanner
import io.github.shnaki.kmpwebview.bridge.model.QrResult
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google Code Scanner (play-services-code-scanner) を使った QR スキャン実装。
 *
 * - カメラ権限不要（Google が提供するシステム UI が権限を管理）
 * - QR コード形式のみに絞り込み（他のバーコードは対象外）
 * - 差し替え可能なインターフェース ([QrScanner]) に隠蔽しており、
 *   将来的に CameraX + ML Kit Barcode へ切り替えても BridgeRouter 側は変更不要。
 */
class AndroidQrScanner(context: Context) : QrScanner {

    private val client = GmsBarcodeScanning.getClient(
        context,
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    override suspend fun scan(): QrResult = suspendCancellableCoroutine { cont ->
        client.startScan()
            .addOnSuccessListener { barcode ->
                cont.resume(QrResult(text = barcode.rawValue.orEmpty()))
            }
            .addOnCanceledListener {
                cont.resumeWithException(BridgeException("CANCELLED", "QR scan was cancelled"))
            }
            .addOnFailureListener { e ->
                val code = if (e.message?.contains("cancel", ignoreCase = true) == true) {
                    "CANCELLED"
                } else {
                    "QR_ERROR"
                }
                cont.resumeWithException(BridgeException(code, e.message ?: "QR scan failed"))
            }
    }
}
