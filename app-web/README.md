# app-web

Android / iOS の両プラットフォームで共有する WebView フロントエンド資産。

| ファイル      | 役割                                                     |
|--------------|----------------------------------------------------------|
| `index.html` | エントリポイント                                          |
| `app.js`     | UI ロジック（ボタンクリック → `NativeBridge.*` 呼び出し）|
| `bridge.js`  | JS ↔ ネイティブ通信層（Android / iOS / PC モック に対応）|
| `style.css`  | スタイルシート                                            |

## プラットフォームごとの取り込み方法

### Android（設定済み）

`app-android/build.gradle.kts` の `sourceSets` でこのディレクトリを assets srcDir に指定済み。
AGP が APK 内 `assets/` 直下にマージし、`WebViewAssetLoader` が
`https://appassets.androidplatform.net/assets/index.html` として HTTPS 配信する。

### iOS（未実装・将来の手順メモ）

`app-ios` モジュールを作成する際は、Xcode プロジェクトの **Copy Bundle Resources** フェーズに
このディレクトリを追加するか、Xcode Build Phase でビルド前コピーを行う。
`WKWebView` から `WKURLSchemeHandler` または `loadFileURL:allowingReadAccessToURL:` でロードする。

`bridge.js` には既に `window.webkit.messageHandlers.nativeBridge` 経由の iOS 分岐が実装済みのため、
Web 側の追加修正は不要。
