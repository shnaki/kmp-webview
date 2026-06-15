/**
 * bridge.js — NativeBridge ラッパー
 *
 * 役割:
 *   - window.NativeBridge.scanQr() / getLocation() / readNfc() という統一 API を提供
 *   - Android では window.nativeBridge (WebMessageListener) 経由で通信
 *   - iOS では (将来) window.webkit.messageHandlers.nativeBridge 経由で通信
 *   - どちらも存在しない環境（PCブラウザ等）ではモック応答を返し、レイアウト確認が可能
 *
 * プロトコル（JSON エンベロープ）:
 *   送信: { "id": "<uuid>", "feature": "qr|gps|nfc", "action": "scan|getCurrent|read", "payload": {} }
 *   受信: { "id": "<uuid>", "ok": true,  "payload": { ... } }
 *      or { "id": "<uuid>", "ok": false, "error": { "code": "...", "message": "..." } }
 */
(function () {
    'use strict';

    /** 送信済みリクエストの { id: { resolve, reject } } マップ */
    var _pending = {};

    /** 単純な一意 ID 生成 */
    function _genId() {
        return Math.random().toString(36).slice(2, 11);
    }

    /** Android: window.nativeBridge が存在するか */
    function _isAndroid() {
        return !!(window.nativeBridge && typeof window.nativeBridge.postMessage === 'function');
    }

    /** iOS: window.webkit.messageHandlers が存在するか（将来拡張用） */
    function _isIos() {
        return !!(window.webkit &&
            window.webkit.messageHandlers &&
            window.webkit.messageHandlers.nativeBridge);
    }

    // ── Android 応答リスナー ────────────────────────────────────────────────
    if (_isAndroid()) {
        window.nativeBridge.addEventListener('message', function (event) {
            try {
                var resp = JSON.parse(event.data);
                var handler = _pending[resp.id];
                if (!handler) return;
                delete _pending[resp.id];
                if (resp.ok) {
                    handler.resolve(resp.payload);
                } else {
                    handler.reject(resp.error || { code: 'UNKNOWN', message: 'Unknown error' });
                }
            } catch (e) {
                console.error('[Bridge] Failed to parse response:', e);
            }
        });
    }

    /**
     * ネイティブへメッセージを送り、Promise で結果を受け取る。
     * @param {string} feature  "qr" | "gps" | "nfc"
     * @param {string} action   "scan" | "getCurrent" | "read"
     * @param {object} [payload]
     * @returns {Promise<object>}
     */
    function _send(feature, action, payload) {
        return new Promise(function (resolve, reject) {
            var id = _genId();
            var msg = JSON.stringify({ id: id, feature: feature, action: action, payload: payload || {} });

            if (_isAndroid()) {
                _pending[id] = { resolve: resolve, reject: reject };
                window.nativeBridge.postMessage(msg);

            } else if (_isIos()) {
                // iOS (将来実装): WKScriptMessageHandlerWithReply を使う場合はここを変更
                _pending[id] = { resolve: resolve, reject: reject };
                window.webkit.messageHandlers.nativeBridge.postMessage(msg);

            } else {
                // PCブラウザ等: モック応答で動作確認
                console.info('[Bridge Mock] feature=' + feature + ', action=' + action);
                setTimeout(function () {
                    resolve(_mockPayload(feature));
                }, 600);
            }
        });
    }

    /** モック応答（ブリッジなし環境用） */
    function _mockPayload(feature) {
        switch (feature) {
            case 'qr':  return { text: 'MOCK_QR_CONTENT_12345' };
            case 'gps': return { latitude: 35.6762, longitude: 139.6503, accuracy: 10.0 };
            case 'nfc': return { records: [{ type: 'T', payload: 'Mock NFC Text', id: '' }] };
            default:    return {};
        }
    }

    // ── 公開 API ──────────────────────────────────────────────────────────────
    window.NativeBridge = {
        /** QR コードをスキャンし { text } を返す */
        scanQr: function () { return _send('qr', 'scan'); },
        /** 現在地を取得し { latitude, longitude, accuracy } を返す */
        getLocation: function () { return _send('gps', 'getCurrent'); },
        /** NFC タグを読み取り { records: [{type, payload, id}] } を返す */
        readNfc: function () { return _send('nfc', 'read'); }
    };

    console.info('[Bridge] NativeBridge initialized. platform=' +
        (_isAndroid() ? 'Android' : _isIos() ? 'iOS' : 'Mock'));
})();
