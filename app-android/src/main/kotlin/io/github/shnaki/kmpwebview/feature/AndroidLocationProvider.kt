package io.github.shnaki.kmpwebview.feature

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.github.shnaki.kmpwebview.bridge.BridgeException
import io.github.shnaki.kmpwebview.bridge.LocationProvider
import io.github.shnaki.kmpwebview.bridge.model.LocationResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * FusedLocationProviderClient を使った現在地取得実装。
 *
 * 権限チェックのみをここで行い、権限未取得の場合は [BridgeException] を投げる。
 * 権限リクエスト自体は MainActivity 起動時に処理する。
 */
class AndroidLocationProvider(private val context: Context) : LocationProvider {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    override suspend fun getCurrent(): LocationResult {
        // 権限確認（ランタイム権限が付与されていなければ即エラー）
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            throw BridgeException("PERMISSION_DENIED", "Location permission not granted")
        }

        return suspendCancellableCoroutine { cont ->
            @Suppress("MissingPermission") // 上記 if ブロックで確認済み
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        cont.resume(
                            LocationResult(
                                latitude  = location.latitude,
                                longitude = location.longitude,
                                accuracy  = location.accuracy
                            )
                        )
                    } else {
                        cont.resumeWithException(
                            BridgeException("LOCATION_UNAVAILABLE", "Location is currently unavailable")
                        )
                    }
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(
                        BridgeException("LOCATION_ERROR", e.message ?: "Failed to get location")
                    )
                }
        }
    }
}
