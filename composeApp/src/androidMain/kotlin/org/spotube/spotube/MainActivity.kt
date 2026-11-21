/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.spotube.spotube

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.spotube.spotube.core.deeplink.ExternalUriHandler
import org.spotube.spotube.core.newpipe.NewPipeDownloader
import org.spotube.spotube.core.paths.Paths
import org.spotube.spotube.media.PlaybackService
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

class MainActivity : ComponentActivity() {
    private var playbackServiceStartAttempted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        NewPipeDownloader.init(Paths())

        intent?.dataString?.let(ExternalUriHandler::onNewUri)
        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        if (playbackServiceStartAttempted) return
        playbackServiceStartAttempted = true
        runCatching {
            startService(Intent(this, PlaybackService::class.java))
        }.onFailure { error ->
            playbackServiceStartAttempted = false
            Log.w("MainActivity", "Could not start PlaybackService", error)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.dataString?.let(ExternalUriHandler::onNewUri)
    }
}