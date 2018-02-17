package de.mlte.soundboard

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.GridLayout
import java.io.BufferedInputStream
import java.io.BufferedOutputStream

class MainActivity : AppCompatActivity() {
    private val buttons = ArrayList<SoundButton>()
    private var player: MediaPlayer? = null
    private var playing = false
    private var playingButton: SoundButton? = null
    private var playingAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myToolbar = findViewById<View>(R.id.my_toolbar) as Toolbar
        setSupportActionBar(myToolbar)

        loadPreferences()
        organizeButtons()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        if (playing) {
            player?.let { mp ->
                if (mp.isPlaying) {
                    outState?.putBoolean("playing", true)
                    outState?.putInt("playingPosition", mp.currentPosition)
                    val parent = findViewById<GridLayout>(R.id.grid_layout)
                    val index = parent.indexOfChild(playingButton)
                    outState?.putInt("playingIndex", index)
                }
            }
        }

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (playing) {
            player?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.reset()
                mp.release()
            }
            playingAnimator?.cancel()
        }

        super.onDestroy()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        buttons.forEach { button ->
            button.progressBar.progress = 0
        }

        savedInstanceState?.let { state ->
            if (state.getBoolean("playing")) {
                val button = buttons[state.getInt("playingIndex")]
                val position = state.getInt("playingPosition")
                startPlaying(button, position)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
                R.id.action_add_new -> {
                    val button = SoundButton(this)
                    addButton(button)
                    saveNumButtons()
                    organizeButtons()
                    editButton(button)
                    return true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveNumButtons() {
        val editor = getPreferences(Context.MODE_PRIVATE).edit()
        editor.putInt("numButtons", buttons.size)
        editor.commit()
    }

    private fun organizeButtons() {
        val orientation = resources.configuration.orientation
        val cols = if (orientation == ORIENTATION_LANDSCAPE)
            Math.ceil(buttons.size / 2.0).toInt()
        else
            Math.ceil(buttons.size / 4.0).toInt()
        val columns = Math.max(Math.min(cols, 4), 1)

        val parent = findViewById<GridLayout>(R.id.grid_layout)

        // every column index used by a button must always be lower than columnCount
        if (columns > parent.columnCount) {
            parent.columnCount = columns
        }

        buttons.forEachIndexed { index, soundButton ->
            val col = index % columns
            val row = index / columns
            soundButton.move(col, row)
        }

        parent.columnCount = columns
    }

    private fun addButton(soundButton: SoundButton) {
        buttons.add(soundButton)

        val parent = findViewById<GridLayout>(R.id.grid_layout)
        parent.addView(soundButton)

        soundButton.textView.setOnClickListener {
            if (playing) {
                player?.let { mp ->
                    if (mp.isPlaying) {
                        mp.stop()
                    }
                    mp.reset()
                    mp.release()
                }
                playingButton?.let { button ->
                    button.progressBar.progress = 0
                }
                playing = false
                playingAnimator?.cancel()
            } else {
                startPlaying(soundButton, 0)
            }
        }

        soundButton.textView.setOnLongClickListener {
            editButton(soundButton)
            true
        }
    }

    private fun startPlaying(soundButton: SoundButton, position: Int) {
        val file = getFileStreamPath("audio" + soundButton.soundId)
        if (file.exists()) {
            val mp = MediaPlayer.create(this, Uri.fromFile(file))
            mp.setOnCompletionListener {
                playingAnimator?.cancel()
                soundButton.progressBar.progress = 0
                mp.reset()
                mp.release()
                playing = false
            }
            mp.seekTo(position)
            mp.start()
            player = mp
            playing = true
            playingButton = soundButton

            val bar = soundButton.progressBar
            val animator = ValueAnimator.ofInt(bar.max * position / mp.duration, bar.max)
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener {
                bar.progress = animator.animatedValue as Int
            }
            animator.duration = mp.duration.toLong() - position
            animator.start()
            playingAnimator = animator
        }
    }

    private fun editButton(soundButton: SoundButton) {
        val intent = Intent(baseContext, EditActivity::class.java)
        val parent = findViewById<GridLayout>(R.id.grid_layout)
        val index = parent.indexOfChild(soundButton)
        intent.putExtra("index", index)
        intent.putExtra("caption", soundButton.textView.text)
        intent.putExtra("fileName", soundButton.fileName)
        startActivityForResult(intent, 1234)
    }

    private fun loadPreferences() {
        val parent = findViewById<GridLayout>(R.id.grid_layout)
        parent.columnCount = 1

        val preferences = getPreferences(Context.MODE_PRIVATE)
        val numButtons = preferences.getInt("numButtons", 0)
        for (index in 0 until numButtons) {
            val soundButton = SoundButton(this)
            addButton(soundButton)
            val caption = preferences.getString("caption" + index, "")
            soundButton.textView.text = caption
            val soundId = preferences.getLong("soundId" + index, 0)
            soundButton.soundId = soundId
            val fileName = preferences.getString("fileName" + index, "")
            soundButton.fileName = fileName
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1234 && resultCode == Activity.RESULT_OK && data != null) {
            val index = data.getIntExtra("index", -1)
            if (index > -1 && index < buttons.size) {
                val button = buttons[index]
                val textView = buttons[index].textView
                if (data.getBooleanExtra("delete", false)) {
                    deleteFile("audio" + buttons[index].soundId)
                    buttons.removeAt(index)
                    val parent = findViewById<GridLayout>(R.id.grid_layout)
                    parent.removeView(button)
                    saveNumButtons()
                    organizeButtons()
                } else {
                    val caption = data.getStringExtra("caption")
                    if (caption != null) {
                        textView.text = caption
                    }
                    val uri = data.getParcelableExtra<Uri>("uri")
                    val fileName = data.getStringExtra("fileName")
                    if (fileName != null) {
                        button.fileName = fileName
                    }
                    savePreferences(uri, index)
                }
            }
        }
    }

    private fun savePreferences(uri: Uri?, index: Int) {
        if (index > -1 && index < buttons.size) {
            val textView = buttons[index].textView
            val soundId = buttons[index].soundId
            val editor = getPreferences(Context.MODE_PRIVATE).edit()
            editor.putString("caption" + index, textView.text.toString())
            editor.putString("fileName" + index, buttons[index].fileName)
            editor.commit()

            uri?.let { uri ->
                deleteFile("audio" + soundId)
                val newSoundId = System.currentTimeMillis()
                buttons[index].soundId = newSoundId
                editor.putLong("soundId" + index, newSoundId)
                editor.commit()
                grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val output = BufferedOutputStream(openFileOutput("audio" + newSoundId, Context.MODE_PRIVATE))
                val input = BufferedInputStream(contentResolver.openInputStream(uri))
                try {
                    val buf = ByteArray(1024)
                    input.read(buf)
                    do {
                        output.write(buf)
                    } while (input.read(buf) !== -1)
                } finally {
                    input.close()
                    output.close()
                }
            }
        }
    }

}