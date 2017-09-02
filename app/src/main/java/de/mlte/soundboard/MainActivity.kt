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

        val mp = MediaPlayer.create(this, R.raw.splash)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)

        var timer = Timer()

        mp.setOnCompletionListener {
            timer.cancel()
            progressBar.progress = 0
        }

        val btn = findViewById<TextView>(R.id.text_view_button)
        btn.setOnClickListener {
            if (mp.isPlaying) {
                mp.stop()
                mp.prepare()
                timer.cancel()
                progressBar.progress = 0
            } else {
                progressBar.max = mp.duration

                timer = Timer()
                val timerTask = timerTask {
                    runOnUiThread {
                        progressBar.progress = mp.currentPosition
                        println(mp.currentPosition)
                    }
                }
                timer.schedule(timerTask, 40, 40)

                mp.start()
            }
        }
    }
}