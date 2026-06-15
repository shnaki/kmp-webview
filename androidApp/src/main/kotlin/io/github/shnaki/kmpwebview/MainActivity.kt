package io.github.shnaki.kmpwebview

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import io.github.shnaki.kmpwebview.bridge.BridgeRouter
import io.github.shnaki.kmpwebview.feature.AndroidLocationProvider
import io.github.shnaki.kmpwebview.feature.AndroidNfcReader
import io.github.shnaki.kmpwebview.feature.AndroidQrScanner
import io.github.shnaki.kmpwebview.ui.theme.KmpWebViewTheme

/**
 * アプリのエントリポイント。
 *
 * 役割:
 *  1. 位置情報のランタイム権限をリクエスト
 *  2. 各ネイティブ機能実装 (Android*) を生成
 *  3. それらを [BridgeRouter] に注入（Manual DI）
 *  4. [WebViewHost] Composable をホスト
 *
 * NFC は android.permission.NFC が危険権限ではないため、
 * Manifest 宣言のみで権限リクエスト不要。
 * QR は Google Code Scanner がカメラ権限を内部管理するためこちらで不要。
 */
class MainActivity : ComponentActivity() {

    // ── 権限リクエスト ─────────────────────────────────────────────────────────
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* 結果は AndroidLocationProvider 側でチェックする。ここでは起動だけ。 */ }

    // ── ブリッジ（Activity 生存期間と同じスコープ）───────────────────────────────
    private lateinit var bridgeRouter: BridgeRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 位置情報権限を事前リクエスト
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        // Manual DI: ネイティブ実装を生成して BridgeRouter へ注入
        bridgeRouter = BridgeRouter(
            qrScanner        = AndroidQrScanner(this),
            locationProvider = AndroidLocationProvider(this),
            nfcReader        = AndroidNfcReader(this)   // Activity 参照が必要
        )

        setContent {
            KmpWebViewTheme {
                WebViewHost(bridgeRouter = bridgeRouter)
            }
        }
    }
}
