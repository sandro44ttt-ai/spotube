package org.spotube.spotube.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.spotube.spotube.core.ui.component.spotify.SpotifyTokens
import org.spotube.spotube.core.ui.theming.AppTheme
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.mix_lab_android_only
import spotube.composeapp.generated.resources.mix_lab_close
import spotube.composeapp.generated.resources.mix_lab_title

@Composable
actual fun MixLabDialog(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpotifyTokens.Base)
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.mix_lab_title),
                    style = AppTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = stringResource(Res.string.mix_lab_android_only),
                    style = AppTheme.typography.bodyMedium,
                    color = SpotifyTokens.Muted,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    text = stringResource(Res.string.mix_lab_close),
                    style = AppTheme.typography.labelLarge,
                    color = SpotifyTokens.Muted,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SpotifyTokens.Surface)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
