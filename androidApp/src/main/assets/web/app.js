/**
 * app.js — サンプルWebアプリのメインロジック
 *
 * bridge.js で定義された window.NativeBridge を呼び出し、
 * 結果を画面に表示する。
 */
(function () {
    'use strict';

    // ── DOM ヘルパー ──────────────────────────────────────────────────────────
    function el(id) { return document.getElementById(id); }

    function setResult(id, state, html) {
        var elem = el(id);
        elem.className = 'result ' + state;
        elem.innerHTML = html;
    }

    function setBusy(id, msg) { setResult(id, 'busy', '⏳ ' + msg); }
    function setOk(id, html)   { setResult(id, 'ok',   html); }
    function setErr(id, err) {
        var msg = err && err.message ? err.code + ': ' + err.message : String(err);
        setResult(id, 'error', '❌ ' + escHtml(msg));
    }

    function escHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    function disableBtn(id, disabled) {
        var btn = el(id);
        if (btn) btn.disabled = disabled;
    }

    // ── QR スキャン ───────────────────────────────────────────────────────────
    el('btn-qr').addEventListener('click', function () {
        disableBtn('btn-qr', true);
        setBusy('result-qr', 'スキャン中...');

        window.NativeBridge.scanQr()
            .then(function (payload) {
                setOk('result-qr', '✅ <strong>' + escHtml(payload.text) + '</strong>');
            })
            .catch(function (err) { setErr('result-qr', err); })
            .finally(function () { disableBtn('btn-qr', false); });
    });

    // ── GPS 現在地 ────────────────────────────────────────────────────────────
    el('btn-gps').addEventListener('click', function () {
        disableBtn('btn-gps', true);
        setBusy('result-gps', '取得中...');

        window.NativeBridge.getLocation()
            .then(function (payload) {
                var lat  = payload.latitude.toFixed(6);
                var lon  = payload.longitude.toFixed(6);
                var acc  = payload.accuracy ? payload.accuracy.toFixed(1) + 'm' : '—';
                var mapsUrl = 'https://maps.google.com/?q=' + lat + ',' + lon;
                setOk('result-gps',
                    '✅ 緯度: <strong>' + lat + '</strong>　経度: <strong>' + lon + '</strong><br>' +
                    '精度: ' + acc + '　' +
                    '<a href="' + mapsUrl + '" target="_blank" ' +
                    'style="color:var(--accent)">Google Maps で開く ↗</a>');
            })
            .catch(function (err) { setErr('result-gps', err); })
            .finally(function () { disableBtn('btn-gps', false); });
    });

    // ── NFC 読み取り ──────────────────────────────────────────────────────────
    el('btn-nfc').addEventListener('click', function () {
        disableBtn('btn-nfc', true);
        setBusy('result-nfc', 'NFC タグを近づけてください...');

        window.NativeBridge.readNfc()
            .then(function (payload) {
                var records = payload.records || [];
                if (records.length === 0) {
                    setOk('result-nfc', '✅ タグを読み取りましたが NDEF レコードが空です。');
                    return;
                }
                var items = records.map(function (r, i) {
                    return '<li>' +
                        '<div class="label">レコード ' + (i + 1) + ' / タイプ: ' + escHtml(r.type) + '</div>' +
                        '<div class="value">' + escHtml(r.payload) + '</div>' +
                        '</li>';
                }).join('');
                setOk('result-nfc', '✅ ' + records.length + ' 件のレコードを読み取りました。<ul class="nfc-records">' + items + '</ul>');
            })
            .catch(function (err) { setErr('result-nfc', err); })
            .finally(function () { disableBtn('btn-nfc', false); });
    });

    // ── 初期化 ───────────────────────────────────────────────────────────────
    console.info('[App] Sample web app ready.');
})();
