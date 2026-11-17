package org.spotube.spotube.core.ui.component.spotify

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.InconsaxClock
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.search_clear_history
import spotube.composeapp.generated.resources.search_placeholder
import spotube.composeapp.generated.resources.search_recent_searches

@Composable
fun SpotifySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    recentSearches: List<String>,
    onRecentSearchClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberTextFieldState(query)
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val visibleSuggestions = remember(query, recentSearches) {
        if (query.isBlank()) {
            recentSearches
        } else {
            recentSearches.filter { it.contains(query, ignoreCase = true) }
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect(onQueryChange)
    }
    LaunchedEffect(query) {
        if (state.text.toString() != query) {
            state.edit { replace(0, length, query) }
        }
    }

    Column(modifier = modifier) {
        BasicTextField(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SpotifyTokens.Highlight)
                .onFocusChanged { isFocused = it.isFocused }
                .padding(horizontal = 12.dp),
            textStyle = AppTheme.typography.bodyLarge.copy(
                color = Color.White,
                fontSize = 16.sp
            ),
            cursorBrush = SolidColor(Color.White),
            lineLimits = TextFieldLineLimits.SingleLine,
            decorator = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MillenniumIcon(
                        imageVector = Iconsax.SpotifySearchGlyph,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isBlank()) {
                            Text(
                                text = stringResource(Res.string.search_placeholder),
                                style = AppTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = SpotifyTokens.Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable {
                                    state.edit { replace(0, length, "") }
                                    onQueryChange("")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            MillenniumIcon(
                                imageVector = Iconsax.SpotifyX,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = SpotifyTokens.Muted
                            )
                        }
                    }
                }
            }
        )

        AnimatedVisibility(
            visible = isFocused && visibleSuggestions.isNotEmpty()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SpotifyTokens.Sheet)
                    .padding(vertical = 8.dp)
            ) {
                if (query.isBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(Res.string.search_recent_searches),
                            style = AppTheme.typography.titleSmall.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = stringResource(Res.string.search_clear_history),
                            style = AppTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                color = SpotifyTokens.Muted
                            ),
                            modifier = Modifier.clickable(onClick = onClearHistory),
                            maxLines = 1
                        )
                    }
                }
                visibleSuggestions.take(5).forEach { recent ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onRecentSearchClick(recent)
                                focusManager.clearFocus()
                                keyboard?.hide()
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MillenniumIcon(
                            imageVector = Iconsax.InconsaxClock,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = SpotifyTokens.Muted
                        )
                        Text(
                            text = recent,
                            style = AppTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
