package de.mlte.soundboard

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout

class SoundButton : FrameLayout {
    constructor(context: Context, col: Int, row: Int) : super(context) {
        View.inflate(context, R.layout.layout_button, this)

        val params = GridLayout.LayoutParams()
        params.columnSpec = GridLayout.spec(col, 1, 1.0f)
        params.rowSpec = GridLayout.spec(row, 1, 1.0f)
        params.width = 0
        params.height = 0
        layoutParams = params
    }
}