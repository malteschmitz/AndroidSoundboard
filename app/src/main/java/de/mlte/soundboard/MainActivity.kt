package de.mlte.soundboard

import android.animation.ObjectAnimator
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ProgressBar
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val objectAnimator = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), 100).setDuration(2000)
        val btn = findViewById<TextView>(R.id.text_view_button)

        objectAnimator.addUpdateListener({ valueAnimator ->
            val progress = valueAnimator.animatedValue as Int
            progressBar.progress = progress
        })

        btn.setOnClickListener {
            objectAnimator.start()
        }
    }
}
