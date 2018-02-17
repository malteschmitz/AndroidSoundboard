package de.mlte.soundboard

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder

class PlayerService : Service() {
    private val binder = PlayerServiceBinder()

    private var player: MediaPlayer? = null

    val currentPosition: Int
    get() = player?.currentPosition ?: 0

    val duration: Int
    get() = player?.duration ?: 0

    val playing: Boolean
    get() = player?.isPlaying ?: false

    inner class PlayerServiceBinder : Binder() {
        internal
        val service: PlayerService
            get() = this@PlayerService
    }

    override fun onBind(intent: Intent): IBinder = binder

    fun stop() {
        player?.let { mp ->
            mp.stop()
            mp.reset()
            mp.release()
            player = null
        }
    }

    fun start(soundId: Long) {
        stop()
        val file = getFileStreamPath("audio" + soundId)
        if (file.exists()) {
            val mp = MediaPlayer.create(this, Uri.fromFile(file))
            mp.setOnCompletionListener {
                mp.reset()
                mp.release()
                player = null
            }
            mp.start()
            player = mp
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
