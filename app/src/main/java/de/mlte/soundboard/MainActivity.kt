package de.mlte.soundboard

import android.animation.ObjectAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import java.util.*
import kotlin.concurrent.timerTask
import android.content.Intent
import android.R.attr.data
import android.app.Activity
import android.net.Uri


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var player: MediaPlayer? = null
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val objectAnimator = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), 1000)
        objectAnimator.interpolator = LinearInterpolator()
        progressBar.max = 1000
        objectAnimator.addUpdateListener({ valueAnimator ->
            val progress = valueAnimator.animatedValue as Int
            progressBar.progress = progress
        })
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
                objectAnimator.cancel()
                playing = false
                progressBar.progress = 0
            } else {
                currentUri?.let { uri ->
                    val mp = MediaPlayer.create(this, uri)
                    mp.setOnCompletionListener {
                        progressBar.progress = 0
                        mp.reset()
                        mp.release()
                        playing = false
                    }
                    mp.start()
                    player = mp
                    playing = true

                    progressBar.progress = 0
                    objectAnimator.setDuration(mp.duration.toLong()).start()
                }
            }
        }

        btn.setOnLongClickListener {
            val intent = Intent()
                    .setType("audio/*")
                    .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, "Select a file"), 123)
            true
        }
    }

    var currentUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 123 && resultCode == Activity.RESULT_OK && data != null) {
            currentUri = data.data
        }
    }
}