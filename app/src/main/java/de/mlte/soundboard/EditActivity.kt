package de.mlte.soundboard

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.provider.OpenableColumns
import android.widget.TextView

class EditActivity : AppCompatActivity() {

    private fun displayFile() {
        val fileTextView = findViewById<TextView>(R.id.fileTextView)
        currentUri?.let { uri -> fileTextView.setText(getFileName(uri)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        val captionEditText = findViewById<EditText>(R.id.captionEditText)
        captionEditText.setText(intent.getStringExtra("caption"))

        currentUri = intent.getParcelableExtra<Uri>("uri")
        displayFile();

        val okButton = findViewById<Button>(R.id.okButton)
        okButton.setOnClickListener {
            val intent = Intent()
            intent.putExtra("caption", captionEditText.text.toString())
            if (currentUri != null) {
                intent.putExtra("uri", currentUri)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        val selectButton = findViewById<Button>(R.id.selectButton)
        selectButton.setOnClickListener {
            val intent = Intent()
                    .setType("audio/*")
                    .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(intent, 123)
        }
    }

    var currentUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 123 && resultCode == Activity.RESULT_OK && data != null) {
            currentUri = data.data
            displayFile()
        }
    }

    fun getFileName(uri: Uri): String {
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
