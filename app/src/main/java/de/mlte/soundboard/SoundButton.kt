package de.mlte.soundboard

import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ProgressBar
import android.widget.TextView

class SoundButton : FrameLayout {
    val progressBar: ProgressBar
    val btn: TextView
    val objectAnimator: ObjectAnimator

    constructor(context: Context) : super(context) {
        View.inflate(context, R.layout.layout_button, this)

        progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        btn = findViewById<TextView>(R.id.text_view_button)

        objectAnimator = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), 1000)
        objectAnimator.interpolator = LinearInterpolator()
        progressBar.max = 1000
        objectAnimator.addUpdateListener({ valueAnimator ->
            val progress = valueAnimator.animatedValue as Int
            progressBar.progress = progress
        })
    }

    fun move(col: Int, row: Int) {
        val params = GridLayout.LayoutParams()
        params.columnSpec = GridLayout.spec(col, 1, 1.0f)
        params.rowSpec = GridLayout.spec(row, 1, 1.0f)
        params.width = 0
        params.height = 0
        layoutParams = params
    }
}