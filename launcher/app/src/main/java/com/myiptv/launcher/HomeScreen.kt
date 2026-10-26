package com.myiptv.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val FavoriteTileWidth = 168.dp
private val FavoriteTileHeight = 95.dp
private val AppTileWidth = 120.dp
private val AppTileHeight = 68.dp

@Composable
fun HomeScreen(viewModel: LauncherViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }
    val firstTile = remember { FocusRequester() }
    var focusedOnce by remember { mutableStateOf(false) }

    val launch: (AppEntry) -> Unit = { app ->
        viewModel.launchIntent(app.packageName)?.let { context.startSafely(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BoxColors.Background)
            .padding(horizontal = 48.dp, vertical = 28.dp),
    ) {
        TopBar(modifier = Modifier.padding(horizontal = 12.dp))
        Spacer(Modifier.weight(1f))
        Clock(modifier = Modifier.padding(horizontal = 12.dp))
        Spacer(Modifier.weight(1f))

        SectionTitle(stringResource(R.string.favorites))
        if (state.favorites.isEmpty() && !state.loading) {
            Text(
                text = stringResource(R.string.no_favorites_hint),
                color = BoxColors.TextSecondary,
                fontSize = 16.sp,
                modifier = Modifier
                    .height(FavoriteTileHeight + 44.dp)
                    .padding(horizontal = 12.dp, vertical = 24.dp),
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
                itemsIndexed(state.favorites, key = { _, app -> app.packageName }) { index, app ->
                    AppTile(
                        app = app,
                        width = FavoriteTileWidth,
                        height = FavoriteTileHeight,
                        quiet = false,
                        onClick = { launch(app) },
                        onLongClick = { menuFor = app },
                        modifier = if (index == 0) Modifier.focusRequester(firstTile) else Modifier,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        SectionTitle(stringResource(R.string.apps))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            itemsIndexed(state.otherApps, key = { _, app -> app.packageName }) { index, app ->
                AppTile(
                    app = app,
                    width = AppTileWidth,
                    height = AppTileHeight,
                    quiet = true,
                    onClick = { launch(app) },
                    onLongClick = { menuFor = app },
                    modifier = if (index == 0 && state.favorites.isEmpty()) {
                        Modifier.focusRequester(firstTile)
                    } else {
                        Modifier
                    },
                )
            }
        }
    }

    // Put focus on the first tile once apps have loaded
    LaunchedEffect(state.loading) {
        if (!state.loading && !focusedOnce) {
            delay(100)
            runCatching { firstTile.requestFocus() }
            focusedOnce = true
        }
    }

    menuFor?.let { app ->
        val isFavorite = viewModel.isFavorite(app.packageName)
        AppMenu(
            app = app,
            isFavorite = isFavorite,
            onDismiss = { menuFor = null },
            onToggleFavorite = {
                viewModel.toggleFavorite(app.packageName)
                menuFor = null
            },
            onMove = { delta ->
                viewModel.moveFavorite(app.packageName, delta)
                menuFor = null
            },
            onAppInfo = {
                context.startSafely(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                menuFor = null
            },
        )
    }
}

@Composable
private fun TopBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val fallbackName = stringResource(R.string.box_name)
    // box.conf's BOX_NAME is written to the device name on first boot
    val boxName = remember {
        Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            ?.takeIf { it.isNotBlank() } ?: fallbackName
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = boxName,
            color = BoxColors.TextMuted,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        RoundIconButton(R.drawable.ic_wifi, stringResource(R.string.wifi)) {
            context.startSafely(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Spacer(Modifier.width(8.dp))
        RoundIconButton(R.drawable.ic_settings, stringResource(R.string.settings)) {
            context.startSafely(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

@Composable
private fun RoundIconButton(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.size(40.dp),
        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = BoxColors.TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = description,
            colorFilter = ColorFilter.tint(if (focused) BoxColors.Background else BoxColors.TextMuted),
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.Center),
        )
    }
}

@Composable
private fun Clock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000 - now % 60_000 + 50) // wake just after the minute changes
        }
    }
    val locale = Locale.getDefault()
    val timePattern = if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm"
    val datePattern = DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd")
    Column(modifier = modifier) {
        Text(
            text = SimpleDateFormat(timePattern, locale).format(Date(now)),
            color = BoxColors.TextPrimary,
            fontSize = 84.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-2).sp,
        )
        Text(
            text = SimpleDateFormat(datePattern, locale).format(Date(now)),
            color = BoxColors.TextSecondary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = BoxColors.TextSecondary,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

/**
 * One app: its TV banner if it has one, otherwise its icon and name.
 * Grows and shows its name underneath when focused.
 */
@Composable
private fun AppTile(
    app: AppEntry,
    width: Dp,
    height: Dp,
    quiet: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(if (quiet) 10.dp else 14.dp)
    Column(modifier = Modifier.width(width)) {
        Surface(
            onClick = onClick,
            onLongClick = onLongClick,
            interactionSource = interaction,
            modifier = modifier
                .width(width)
                .height(height),
            shape = ClickableSurfaceDefaults.shape(shape = shape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = if (quiet) BoxColors.TileQuiet else BoxColors.Tile,
                focusedContainerColor = BoxColors.TextPrimary,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(3.dp, BoxColors.Accent), shape = shape),
            ),
        ) {
            if (app.banner != null) {
                Image(
                    bitmap = app.banner,
                    contentDescription = app.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    app.icon?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            modifier = Modifier.size(if (quiet) 30.dp else 40.dp),
                        )
                    }
                    if (!quiet) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = app.label,
                            color = if (focused) BoxColors.Background else BoxColors.TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        Text(
            text = app.label,
            color = BoxColors.TextPrimary,
            fontSize = if (quiet) 12.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 12.dp)
                .alpha(if (focused) 1f else 0f),
        )
    }
}

/** Shown when OK is held on an app. */
@Composable
private fun AppMenu(
    app: AppEntry,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMove: (Int) -> Unit,
    onAppInfo: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        val first = remember { FocusRequester() }
        Column(
            modifier = Modifier
                .width(320.dp)
                .background(BoxColors.Menu, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = app.label,
                color = BoxColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp),
            )
            MenuItem(
                text = stringResource(if (isFavorite) R.string.remove_favorite else R.string.add_favorite),
                onClick = onToggleFavorite,
                modifier = Modifier.focusRequester(first),
            )
            if (isFavorite) {
                MenuItem(stringResource(R.string.move_left), onClick = { onMove(-1) })
                MenuItem(stringResource(R.string.move_right), onClick = { onMove(1) })
            }
            MenuItem(stringResource(R.string.app_info), onClick = onAppInfo)
            MenuItem(stringResource(R.string.cancel), onClick = onDismiss)
        }
        LaunchedEffect(Unit) {
            delay(50)
            runCatching { first.requestFocus() }
        }
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = BoxColors.TextPrimary,
            focusedContainerColor = BoxColors.TextPrimary,
            focusedContentColor = BoxColors.Background,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Nothing installed handles it (e.g. no Wi-Fi settings screen); ignore
    } catch (e: SecurityException) {
        // App can't be started from outside; ignore
    }
}
