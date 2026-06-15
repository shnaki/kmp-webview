package io.github.shnaki.kmpwebview.bridge

/**
 * ブリッジ処理中に発生した既知のエラーを表す例外。
 *
 * [code] は Web 側で受け取る標準エラーコード:
 *   - PERMISSION_DENIED  … 権限拒否
 *   - CANCELLED          … ユーザーキャンセル
 *   - UNSUPPORTED        … 機能非対応
 *   - PARSE_ERROR        … JSON パース失敗
 *   - INTERNAL_ERROR     … その他の予期しないエラー
 */
class BridgeException(val code: String, message: String) : Exception(message)
