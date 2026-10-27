package com.myiptv.carlauncher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val Gap = 16.dp
private val DockHeight = 112.dp
private val PanelShape = RoundedCornerShape(24.dp)

@Composable
fun HomeScreen(viewModel: LauncherViewModel, onRequestLocation: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val drawerOpen by viewModel.drawerOpen.collectAsState()
    val context = LocalContext.current
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }

    val launch: (String) -> Unit = { packageName ->
        viewModel.launchIntent(packageName)?.let { context.startSafely(it) }
    }

    // This is the home screen: Back only closes the app drawer
    BackHandler { viewModel.setDrawerOpen(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CarColors.Background)
            .padding(Gap),
    ) {
        if (drawerOpen) {
            AppDrawer(
                apps = state.allApps,
                onLaunch = { app ->
                    viewModel.setDrawerOpen(false)
                    launch(app.packageName)
                },
                onLongPress = { menuFor = it },
                onClose = { viewModel.setDrawerOpen(false) },
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Gap),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(Gap),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(Gap),
                    ) {
                        ClockPanel(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                        SpeedPanel(
                            viewModel = viewModel,
                            onRequestLocation = onRequestLocation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        )
                    }
                    NowPlayingPanel(
                        viewModel = viewModel,
                        onOpen = launch,
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight(),
                    )
                }
                Dock(
                    apps = state.dock,
                    loading = state.loading,
                    onLaunch = { launch(it.packageName) },
                    onLongPress = { menuFor = it },
                    onAllApps = { viewModel.setDrawerOpen(true) },
                )
            }
        }
    }

    menuFor?.let { app ->
        val inDock = viewModel.isInDock(app.packageName)
        AppMenu(
            app = app,
            inDock = inDock,
            onDismiss = { menuFor = null },
            onToggleDock = {
                viewModel.toggleDock(app.packageName)
                menuFor = null
            },
            onMove = { delta ->
                viewModel.moveInDock(app.packageName, delta)
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

/** A rounded dark card; tappable when [onClick] is given. */
@Composable
private fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(PanelShape)
            .background(CarColors.Card)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content,
    )
}

@Composable
private fun ClockPanel(modifier: Modifier = Modifier) {
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
    Panel(
        modifier = modifier,
        onClick = {
            context.startSafely(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 24.dp),
        ) {
            Text(
                text = SimpleDateFormat(timePattern, locale).format(Date(now)),
                color = CarColors.TextPrimary,
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp,
                maxLines = 1,
            )
            Text(
                text = SimpleDateFormat(datePattern, locale).format(Date(now)),
                color = CarColors.TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** GPS speed. Tap to switch between mph and km/h. */
@Composable
private fun SpeedPanel(
    viewModel: LauncherViewModel,
    onRequestLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasPermission by viewModel.speed.hasPermission.collectAsState()
    val speed by viewModel.speed.speed.collectAsState()
    val mph by viewModel.useMph.collectAsState()
    Panel(
        modifier = modifier,
        onClick = if (hasPermission) viewModel::toggleSpeedUnit else onRequestLocation,
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 24.dp),
        ) {
            if (!hasPermission) {
                Text(
                    text = stringResource(R.string.speed_no_permission),
                    color = CarColors.TextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
            } else {
                val value = speed?.let { (it * if (mph) MPS_TO_MPH else MPS_TO_KMH).roundToInt().toString() }
                Row {
                    Text(
                        text = value ?: "--",
                        color = CarColors.TextPrimary,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(if (mph) R.string.unit_mph else R.string.unit_kmh),
                        color = CarColors.Accent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
                if (value == null) {
                    Text(
                        text = stringResource(R.string.speed_waiting),
                        color = CarColors.TextSecondary,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingPanel(
    viewModel: LauncherViewModel,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val hasAccess by viewModel.media.hasAccess.collectAsState()
    val nowPlaying by viewModel.media.nowPlaying.collectAsState()
    val playing = nowPlaying

    if (!hasAccess) {
        Panel(
            modifier = modifier,
            onClick = {
                val opened = context.startSafely(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                // Some head unit firmware hides that screen; app info has a way in from there
                if (!opened) {
                    context.startSafely(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            },
        ) {
            MediaMessage(stringResource(R.string.media_access_title), stringResource(R.string.media_access_hint))
        }
        return
    }
    if (playing == null) {
        Panel(modifier = modifier) {
            MediaMessage(stringResource(R.string.nothing_playing), stringResource(R.string.nothing_playing_hint))
        }
        return
    }

    Panel(modifier = modifier, onClick = { onOpen(playing.packageName) }) {
        // Album art, dimmed, behind everything
        playing.art?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.2f),
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            // Leave the text and buttons room on narrow screens
            val artSize = min(maxHeight, maxWidth * 0.4f)
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(artSize)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CarColors.CardRaised),
                    contentAlignment = Alignment.Center,
                ) {
                    if (playing.art != null) {
                        Image(
                            bitmap = playing.art,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_music),
                            contentDescription = null,
                            tint = CarColors.TextSecondary,
                            modifier = Modifier.size(artSize * 0.4f),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playing.title,
                        color = CarColors.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (playing.artist.isNotBlank()) {
                        Text(
                            text = playing.artist,
                            color = CarColors.TextSecondary,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RoundButton(R.drawable.ic_previous, stringResource(R.string.previous), 56.dp) {
                            viewModel.media.previous()
                        }
                        RoundButton(
                            icon = if (playing.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                            description = stringResource(if (playing.isPlaying) R.string.pause else R.string.play),
                            size = 72.dp,
                            background = CarColors.Accent,
                            tint = CarColors.OnAccent,
                        ) {
                            viewModel.media.playPause()
                        }
                        RoundButton(R.drawable.ic_next, stringResource(R.string.next), 56.dp) {
                            viewModel.media.next()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.MediaMessage(title: String, hint: String) {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_music),
            contentDescription = null,
            tint = CarColors.Accent,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            color = CarColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = hint,
            color = CarColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RoundButton(
    @DrawableRes icon: Int,
    description: String,
    size: Dp,
    background: Color = CarColors.CardRaised,
    tint: Color = CarColors.TextPrimary,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(size * 0.45f),
        )
    }
}

/** Bottom row: the user's dock apps, then the All apps button. */
@Composable
private fun Dock(
    apps: List<AppEntry>,
    loading: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onAllApps: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DockHeight),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            if (apps.isEmpty() && !loading) {
                Text(
                    text = stringResource(R.string.empty_dock_hint),
                    color = CarColors.TextSecondary,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(horizontal = 12.dp),
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(apps, key = { it.packageName }) { app ->
                        AppTile(
                            label = app.label,
                            onClick = { onLaunch(app) },
                            onLongClick = { onLongPress(app) },
                            modifier = Modifier
                                .width(100.dp)
                                .fillMaxHeight(),
                        ) {
                            AppIcon(app, 52.dp)
                        }
                    }
                }
            }
        }
        AppTile(
            label = stringResource(R.string.all_apps),
            onClick = onAllApps,
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight(),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_apps),
                contentDescription = null,
                tint = CarColors.Accent,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Composable
private fun AppDrawer(
    apps: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, bottom = Gap),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.all_apps),
                color = CarColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            RoundButton(R.drawable.ic_settings, stringResource(R.string.settings), 52.dp) {
                context.startSafely(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            RoundButton(R.drawable.ic_close, stringResource(R.string.close), 52.dp, onClick = onClose)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(112.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppTile(
                    label = app.label,
                    onClick = { onLaunch(app) },
                    onLongClick = { onLongPress(app) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp),
                ) {
                    AppIcon(app, 56.dp)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CarColors.Card)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = CarColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AppIcon(app: AppEntry, size: Dp) {
    if (app.icon != null) {
        Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(size))
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(CarColors.CardRaised)
        )
    }
}

/** Shown when an app is held down. */
@Composable
private fun AppMenu(
    app: AppEntry,
    inDock: Boolean,
    onDismiss: () -> Unit,
    onToggleDock: () -> Unit,
    onMove: (Int) -> Unit,
    onAppInfo: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CarColors.Menu)
                .padding(12.dp),
        ) {
            Text(
                text = app.label,
                color = CarColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            )
            MenuItem(stringResource(if (inDock) R.string.remove_from_dock else R.string.add_to_dock), onToggleDock)
            if (inDock) {
                MenuItem(stringResource(R.string.move_left)) { onMove(-1) }
                MenuItem(stringResource(R.string.move_right)) { onMove(1) }
            }
            MenuItem(stringResource(R.string.app_info), onAppInfo)
            MenuItem(stringResource(R.string.cancel), onDismiss)
        }
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(text = text, color = CarColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Starts [intent], returning false if nothing could handle it. */
private fun Context.startSafely(intent: Intent): Boolean =
    try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

private const val MPS_TO_MPH = 2.23694f
private const val MPS_TO_KMH = 3.6f
