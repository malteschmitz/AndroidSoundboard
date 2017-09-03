package de.mlte.soundboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.GridLayout
import java.io.BufferedInputStream
import java.io.BufferedOutputStream


class MainActivity : AppCompatActivity() {
    private val buttons = ArrayList<SoundButton>()
    private var player: MediaPlayer? = null
    private var playing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myToolbar = findViewById<View>(R.id.my_toolbar) as Toolbar
        setSupportActionBar(myToolbar)


        spawnButtons()

        loadPreferences()

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
                    organizeButtons()
                    return true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun spawnButtons() {
        for (i in 0..4) {
            val soundButton = SoundButton(this)
            addButton(soundButton)
        }
        organizeButtons()
    }

    private fun organizeButtons() {
        val parent = findViewById<GridLayout>(R.id.grid_layout)
        val columns = Math.min(Math.ceil(buttons.size / 4.0).toInt(), 4)
        parent.columnCount = columns
        buttons.forEachIndexed { index, soundButton ->
            val col = index % columns
            val row = index / columns
            soundButton.move(col, row)
        }
    }

    private fun addButton(soundButton: SoundButton) {
        val index = buttons.size
        buttons.add(soundButton)

        val parent = findViewById<GridLayout>(R.id.grid_layout)
        parent.addView(soundButton)

        soundButton.btn.setOnClickListener {
            if (playing) {
                player?.let { mp ->
                    if (mp.isPlaying) {
                        mp.stop()
                    }
                    mp.reset()
                    mp.release()
                }
                for (button in buttons) {
                    button.objectAnimator.cancel()
                    button.progressBar.progress = 0
                }
                playing = false
            } else {
                val file = getFileStreamPath("audio" + index)
                if (file.exists()) {
                    val mp = MediaPlayer.create(this, Uri.fromFile(file))
                    mp.setOnCompletionListener {
                        soundButton.progressBar.progress = 0
                        mp.reset()
                        mp.release()
                        playing = false
                    }
                    mp.start()
                    player = mp
                    playing = true

                    soundButton.progressBar.progress = 0
                    soundButton.objectAnimator.setDuration(mp.duration.toLong()).start()
                }
            }
        }

        soundButton.btn.setOnLongClickListener {
            val intent = Intent(baseContext, EditActivity::class.java)
            intent.putExtra("index", index)
            intent.putExtra("caption", soundButton.btn.text)
            startActivityForResult(intent, 1234)

            true
        }
    }

    private fun loadPreferences() {
        val preferences = getPreferences(Context.MODE_PRIVATE)
        buttons.forEachIndexed { index, soundButton ->
            val caption = preferences.getString("caption" + index, "")
            soundButton.btn.text = caption
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1234 && resultCode == Activity.RESULT_OK && data != null) {
            val index = data.getIntExtra("index", -1)
            if (index > -1 && index < buttons.size) {
                val btn = buttons[index].btn
                val caption = data.getStringExtra("caption")
                if (caption != null) {
                    btn.setText(caption)
                }
                val uri = data.getParcelableExtra<Uri>("uri")
                savePreferences(caption, uri, index)
            }
        }
    }

    private fun savePreferences(caption: String, uri: Uri?, index: Int) {
        if (index > -1 && index < buttons.size) {
            val btn = buttons[index].btn
            val editor = getPreferences(Context.MODE_PRIVATE).edit()
            editor.putString("caption" + index, btn.text.toString())
            editor.commit()

            uri?.let { uri ->
                grantUriPermission(getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val output = BufferedOutputStream(openFileOutput("audio" + index, Context.MODE_PRIVATE))
                val input = BufferedInputStream(getContentResolver().openInputStream(uri))
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