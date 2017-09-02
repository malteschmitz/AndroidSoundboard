package de.mlte.soundboard

import android.animation.ObjectAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ProgressBar
import android.widget.TextView
import java.util.*
import kotlin.concurrent.timerTask

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var player: MediaPlayer? = null
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        var timer = Timer()
        var playing = false

        val btn = findViewById<TextView>(R.id.text_view_button)
        btn.setOnClickListener {
            if (playing) {
                player?.let { mp ->
                    if (mp.isPlaying) {
                        mp.stop()
                    }
                    mp.reset()
                    mp.release()
                }
                playing = false
                timer.cancel()
                progressBar.progress = 0
            } else {
                val mp = MediaPlayer.create(this, R.raw.splash)
                mp.setOnCompletionListener {
                    timer.cancel()
                    progressBar.progress = 0
                    mp.reset()
                    mp.release()
                    playing = false
                }
                mp.start()
                player = mp
                playing = true

                progressBar.max = mp.duration
                progressBar.progress = 0

                timer = Timer()
                val timerTask = timerTask {
                    runOnUiThread {
                        if (playing && mp.isPlaying) {
                            if (mp.currentPosition > progressBar.progress) {
                                progressBar.progress = mp.currentPosition
                            }
                        }
                    }
                }
                timer.schedule(timerTask, 40, 40)
            }
        }
    }
}