package com.crexative.exoplayercompose

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.media3.ui.PlayerView.SHOW_BUFFERING_WHEN_PLAYING
import com.crexative.exoplayercompose.ui.theme.ExoplayerComposeTheme

val video_Url: String =
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
val video_Url_HLS: String = "https://assets.afcdn.com/video49/20210722/v_645516.m3u8"

class MainActivity : ComponentActivity() {
    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExoplayerComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    ExoPlayerScreen(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    @UnstableApi
    @Composable
    private fun ExoPlayerScreen(
        modifier: Modifier
    ) {
        val context = LocalContext.current
        val activity = context as Activity
        val window = activity.window

        var isFullScreen by remember { mutableStateOf(false) }
        var player by remember { mutableStateOf<Player?>(null) }

        // Create PlayerView with a properly initialized player
        val playerView = remember {
            PlayerView(context).apply {
                controllerAutoShow = true
                keepScreenOn = true
                setShowBuffering(SHOW_BUFFERING_WHEN_PLAYING)
                setFullscreenButtonClickListener { fullScreen ->
                    isFullScreen = fullScreen
                    if (fullScreen && activity.requestedOrientation == SCREEN_ORIENTATION_USER) {
                        enterFullScreen(window)
                        activity.requestedOrientation = SCREEN_ORIENTATION_USER_LANDSCAPE
                    } else if (!fullScreen) {
                        exitFullScreen(window)
                        activity.requestedOrientation = SCREEN_ORIENTATION_USER
                    }
                }
            }
        }

        // Update player reference when it changes
        DisposableEffect(player) {
            playerView.player = player
            onDispose {
                playerView.player = null
            }
        }

        ComposableLifeCycle { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    player = initPlayer(context)
                    playerView.onResume()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (player == null) {
                        player = initPlayer(context)
                    }
                    playerView.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    playerView.onPause()
                    player?.pause()
                }
                Lifecycle.Event.ON_STOP -> {
                    playerView.onPause()
                    player?.release()
                    player = null
                    // Make sure to exit full screen when stopping
                    if (isFullScreen) {
                        exitFullScreen(window)
                    }
                }
                else -> Unit
            }
        }

        AndroidView(
            factory = { playerView },
            modifier = modifier
        )
    }

    private fun enterFullScreen(window: Window) {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

    private fun exitFullScreen(window: Window) {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setDecorFitsSystemWindows(true)
            window.insetsController?.apply {
                show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
            }
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    @UnstableApi
    private fun initPlayer(context: Context): Player? {
        return ExoPlayer.Builder(context).build().apply {
            val defaultHttpDataSource = DefaultHttpDataSource.Factory()
            val uri = video_Url.toUri()
            val mediaSource = buildMediaSource(uri, defaultHttpDataSource, null)
            setMediaSource(mediaSource)
            playWhenReady = true
            prepare()
        }
    }

    @UnstableApi
    private fun buildMediaSource(
        uri: Uri,
        factory: DefaultHttpDataSource.Factory,
        exception: String?
    ): MediaSource {
        val type = Util.inferContentType(uri)
        return when (type) {
            C.CONTENT_TYPE_DASH -> DashMediaSource.Factory(factory).createMediaSource(
                MediaItem.fromUri(uri)
            )

            C.CONTENT_TYPE_SS -> SsMediaSource.Factory(factory).createMediaSource(
                MediaItem.fromUri(uri)
            )

            C.CONTENT_TYPE_HLS -> HlsMediaSource.Factory(factory).createMediaSource(
                MediaItem.fromUri(uri)
            )

            C.CONTENT_TYPE_OTHER -> ProgressiveMediaSource.Factory(factory).createMediaSource(
                MediaItem.fromUri(uri)
            )

            else -> {
                throw IllegalArgumentException("Unsupported media type: $type")
            }
        }
    }

    @Composable
    fun ComposableLifeCycle(
        lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
        onEvent: (LifecycleOwner, Lifecycle.Event) -> Unit
    ) {
        DisposableEffect(key1 = lifecycleOwner) {
            // 1. Create a LifecycleEventObserver to handle lifecycle events
            val observer = LifecycleEventObserver { source, event ->
                // 2. Call the provided onEvent callback with the source and event.
                onEvent(source, event)
            }

            // 3. Add the observer to the lifecycle of the provided lifecycleOwner
            lifecycleOwner.lifecycle.addObserver(observer)

            // 4. Remove the observer when the composable is disposed.
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    @Composable
    private fun createPlayerView(player: Player?): PlayerView {
        val context = LocalContext.current
        val playerView = remember {
            PlayerView(context).apply {
                this.player = player
            }
        }

        DisposableEffect(key1 = player) {
            playerView.player = player

            onDispose {
                playerView.player = null
            }
        }

        return playerView
    }
}