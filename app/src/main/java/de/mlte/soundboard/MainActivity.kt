package de.mlte.soundboard

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
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
import android.content.ComponentName
import android.os.IBinder
import android.content.ServiceConnection
import android.animation.AnimatorListenerAdapter
import android.util.Log

class MainActivity : AppCompatActivity() {
    private val buttons = ArrayList<SoundButton>()
    private var playingButton: SoundButton? = null
    private var playingAnimator: ValueAnimator? = null

    private var playerService: PlayerService? = null

    private val playerServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName,
                                        binder: IBinder) {
            val binder = binder as PlayerService.PlayerServiceBinder
            val service = binder.service
            playerService = service
            playingButton?.let{ button -> startAnimator(service, button) }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            playerService = null
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myToolbar = findViewById<View>(R.id.my_toolbar) as Toolbar
        setSupportActionBar(myToolbar)

        loadPreferences()
        organizeButtons()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        playerService?.let { service ->
            if (service.playing) {
                val parent = findViewById<GridLayout>(R.id.grid_layout)
                val index = parent.indexOfChild(playingButton)
                outState?.putInt("playing", index)
            }
        }

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        buttons.forEach { button ->
            button.progressBar.progress = 0
        }

        savedInstanceState?.let { state ->
            val index = state.getInt("playing", -1)
            if (index > -1) {
                playingButton = buttons[index]
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val intent = Intent(this, PlayerService::class.java)
        startService(intent)
        bindService(intent, playerServiceConnection, 0)
    }

    override fun onStop() {
        super.onStop()

        if (playerService?.playing != true) {
            val intent = Intent(this, PlayerService::class.java)
            stopService(intent)
        }

        unbindService(playerServiceConnection)
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
        editor.apply()
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

    private fun playButton(soundButton: SoundButton) {
        playerService?.let{ service ->
            playingButton = soundButton
            service.start(soundButton.soundId)
            startAnimator(service, soundButton)
        }
    }

    private fun startAnimator(service: PlayerService, button: SoundButton) {
        playingAnimator?.cancel()
        if (service.playing) {
            val bar = button.progressBar
            val animator = ValueAnimator.ofInt(bar.max * service.currentPosition / service.duration, bar.max)
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener {
                bar.progress = animator.animatedValue as Int
            }
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    bar.progress = 0
                }
            })

            animator.duration = service.duration.toLong() - service.currentPosition
            animator.start()
            playingAnimator = animator
        }
    }

    private fun addButton(soundButton: SoundButton) {
        buttons.add(soundButton)

        val parent = findViewById<GridLayout>(R.id.grid_layout)
        parent.addView(soundButton)

        soundButton.textView.setOnClickListener {
            if (playerService?.playing == true) {
                playerService?.stop()
                playingAnimator?.cancel()
            } else {
                playButton(soundButton)
            }
        }

        soundButton.textView.setOnLongClickListener {
            editButton(soundButton)
            true
        }
    }

    private fun editButton(soundButton: SoundButton) {
        playerService?.stop()
        playingAnimator?.cancel()
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
                    // TODO: Do we need to remove some preferences?
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
            editor.apply()

            uri?.let { u ->
                deleteFile("audio" + soundId)
                val newSoundId = System.currentTimeMillis()
                buttons[index].soundId = newSoundId
                editor.putLong("soundId" + index, newSoundId)
                editor.commit()
                grantUriPermission(packageName, u, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val output = BufferedOutputStream(openFileOutput("audio" + newSoundId, Context.MODE_PRIVATE))
                val input = BufferedInputStream(contentResolver.openInputStream(u))
                try {
                    val buf = ByteArray(1024)
                    input.read(buf)
                    do {
                        output.write(buf)
                    } while (input.read(buf) != -1)
                } finally {
                    input.close()
                    output.close()
                }
            }
        }
    }

}