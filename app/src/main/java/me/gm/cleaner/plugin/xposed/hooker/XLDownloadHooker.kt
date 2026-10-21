/*
 * Copyright 2021 Green Mushroom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.gm.cleaner.plugin.xposed.hooker

import io.github.libxposed.api.XposedInterface.Chain
import io.github.libxposed.api.XposedInterface.Hooker
import me.gm.cleaner.plugin.util.L
import java.io.IOException

/**
 * Blocks the Xunlei engine's storage-root `.xlDownload` folder inside Xiaomi's download
 * manager (`com.android.providers.downloads`).
 *
 * Verified against the decompiled download manager builds 134.24.11.800060 /
 * 135.24.11.800080 / 136.25.11.800010 / 137.26.07.800040 (Android 14–17): the relevant
 * class surface is identical on all four versions. The folder is created at application
 * startup — `DownloadApplication.init()` → `XLConfig.initLog()` → `XLConfig.setDebug()`
 * → `FileUtil.createFile(sdcard/.xlDownload/dp_so.log)` — and per download thread via
 * `XLDownloadThread` → `XLConfig.setSoDebug()` → `XLDownloadManager.setReleaseLog()`
 * (native write). The existing mkdir/mkdirs hooks install after `Application.onCreate`
 * and cannot observe native writes at all, so these funnel methods are skipped instead.
 * Equivalent to HyperCeiler's RemoveXlDownload rule.
 */

/** Skips XLConfig.setDebug/setSoDebug entirely; returning null is a no-op for void. */
class XLConfigHooker : Hooker {
    override fun intercept(chain: Chain): Any? {
        L.d("blocked ${chain.executable.name} to keep .xlDownload away")
        return null
    }
}

/**
 * Guard on FileUtil.createFile: paths under `.xlDownload` fail with the IOException the
 * method already declares, so callers fall back to their own error handling.
 */
class XLCreateFileHooker : Hooker {
    override fun intercept(chain: Chain): Any? {
        val path = chain.args.firstOrNull() as? String
        if (path != null && path.contains(".xlDownload")) {
            L.d("blocked createFile for $path")
            throw IOException(".xlDownload is blocked")
        }
        return chain.proceed()
    }
}
