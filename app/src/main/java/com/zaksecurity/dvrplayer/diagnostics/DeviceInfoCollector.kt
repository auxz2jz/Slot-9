package com.zaksecurity.dvrplayer.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import java.util.Locale

object DeviceInfoCollector {
    @Suppress("DEPRECATION")
    fun collect(context: Context): Map<String, String> {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo =
            ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val metrics = context.resources.displayMetrics
        val locale =
            context.resources.configuration.locales.get(0) ?: Locale.getDefault()

        return linkedMapOf(
            "appPackage" to context.packageName,
            "appVersionName" to (packageInfo.versionName ?: "unknown"),
            "appVersionCode" to packageInfo.longVersionCode.toString(),
            "androidRelease" to Build.VERSION.RELEASE,
            "apiLevel" to Build.VERSION.SDK_INT.toString(),
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "device" to Build.DEVICE,
            "product" to Build.PRODUCT,
            "supportedAbis" to Build.SUPPORTED_ABIS.joinToString(","),
            "availableProcessors" to Runtime.getRuntime().availableProcessors().toString(),
            "memoryClassMb" to activityManager.memoryClass.toString(),
            "largeMemoryClassMb" to activityManager.largeMemoryClass.toString(),
            "lowRamDevice" to activityManager.isLowRamDevice.toString(),
            "availableMemoryBytes" to memoryInfo.availMem.toString(),
            "totalMemoryBytes" to memoryInfo.totalMem.toString(),
            "displayWidthPx" to metrics.widthPixels.toString(),
            "displayHeightPx" to metrics.heightPixels.toString(),
            "density" to metrics.density.toString(),
            "densityDpi" to metrics.densityDpi.toString(),
            "locale" to locale.toLanguageTag()
        )
    }

    fun asText(info: Map<String, String>): String =
        info.entries.joinToString("\n") { (key, value) -> "$key=$value" } + "\n"
}
