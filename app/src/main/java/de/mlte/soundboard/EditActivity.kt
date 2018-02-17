package de.mlte.soundboard

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.provider.OpenableColumns
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_edit.*

class EditActivity : AppCompatActivity() {

    private fun displayFile() {
        val fileTextView = findViewById<TextView>(R.id.fileTextView)
        currentUri?.let { uri ->
            fileTextView.text = getFileName(uri)
            if (captionEditText.text.isBlank()) {
                captionEditText.setText(fileTextView.text)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        val editToolbar = findViewById<View>(R.id.edit_toolbar) as Toolbar
        setSupportActionBar(editToolbar)
        editToolbar.navigationIcon = ResourcesCompat.getDrawable(resources, R.drawable.ic_arrow_back_black_24dp, null)
        editToolbar.setNavigationOnClickListener(View.OnClickListener { onBackPressed() })

        val captionEditText = findViewById<EditText>(R.id.captionEditText)
        captionEditText.setText(intent.getStringExtra("caption"))

        val fileName = intent.getStringExtra("fileName")
        if (fileName.isEmpty()) fileTextView.text = "No file selected" else fileTextView.text = fileName

        val selectButton = findViewById<Button>(R.id.selectButton)
        selectButton.setOnClickListener {
            val intent = Intent()
                    .setType("audio/*")
                    .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(intent, 123)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
                R.id.submit -> {
                    val data = Intent()
                    data.putExtra("index", intent.getIntExtra("index", -1))
                    data.putExtra("caption", captionEditText.text.toString())
                    data.putExtra("fileName", fileTextView.text.toString())
                    if (currentUri != null) {
                        data.putExtra("uri", currentUri)
                    }
                    setResult(Activity.RESULT_OK, data)
                    finish()
                }
                R.id.delete -> {
                    val data = Intent()
                    data.putExtra("index", intent.getIntExtra("index", -1))
                    data.putExtra("delete", true)
                    setResult(Activity.RESULT_OK, data)
                    finish()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private var currentUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 123 && resultCode == Activity.RESULT_OK && data != null) {
            currentUri = data.data
            displayFile()
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor!!.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }
}
