package io.github.shnaki.kmpwebview

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewCompat
import io.github.shnaki.kmpwebview.bridge.BridgeRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * WebView を Compose でホストする薄いラッパー。
 *
 * - [WebViewAssetLoader] で `app-web/`（リポジトリ直下の共通 Web 資産）を
 *   `https://appassets.androidplatform.net/assets/` として HTTPS 配信
 *   （WebMessageListener が HTTPS オリジンを要求するため file:// は使わない）
 *
 * - [WebViewCompat.addWebMessageListener] でブリッジチャネル "nativeBridge" を登録。
 *   JS 側: `window.nativeBridge.postMessage(json)` → Native 処理 →
 *   `replyProxy.postMessage(response)` → JS の `window.nativeBridge` に message イベント
 *
 * - 受信した JSON を IO スレッドで [BridgeRouter.handle] に渡し、
 *   応答をメインスレッドで reply する。
 */
@Composable
fun WebViewHost(bridgeRouter: BridgeRouter) {
    val scope = rememberCoroutineScope()

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            // WebViewAssetLoader: assets/ を HTTPS で配信
            val assetLoader = WebViewAssetLoader.Builder()
                .setDomain("appassets.androidplatform.net")
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                .build()

            WebView(ctx).apply {
                // デバッグ時は chrome://inspect で確認できるよう有効化
                WebView.setWebContentsDebuggingEnabled(true)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled  = true
                    // assets 配信なのでファイルアクセスは不要
                    allowFileAccess    = false
                }

                webViewClient = object : WebViewClientCompat() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)
                }

                // ブリッジ登録: JS → Native メッセージ受信
                WebViewCompat.addWebMessageListener(
                    /* webView    = */ this,
                    /* jsObjectName = */ "nativeBridge",
                    /* allowedOriginRules = */ setOf("https://appassets.androidplatform.net"),
                    /* listener   = */ object : WebViewCompat.WebMessageListener {
                        override fun onPostMessage(
                            view: WebView,
                            message: WebMessageCompat,
                            sourceOrigin: Uri,
                            isMainFrame: Boolean,
                            replyProxy: JavaScriptReplyProxy
                        ) {
                            val json = message.data ?: return
                            // IO スレッドで BridgeRouter を実行し、結果をメインスレッドで返送
                            scope.launch(Dispatchers.IO) {
                                val response = bridgeRouter.handle(json)
                                withContext(Dispatchers.Main) {
                                    replyProxy.postMessage(response)
                                }
                            }
                        }
                    }
                )

                loadUrl("https://appassets.androidplatform.net/assets/index.html")
            }
        }
    )
}
