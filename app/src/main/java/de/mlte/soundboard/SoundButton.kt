package de.mlte.soundboard

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ProgressBar
import android.widget.TextView

class SoundButton : FrameLayout {
    val progressBar: ProgressBar
    val textView: TextView
    var soundId: Long = 0
    var fileName: String = ""

    constructor(context: Context) : super(context) {
        View.inflate(context, R.layout.layout_button, this)

        progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        progressBar.max = 1000
        textView = findViewById<TextView>(R.id.text_view_button)
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