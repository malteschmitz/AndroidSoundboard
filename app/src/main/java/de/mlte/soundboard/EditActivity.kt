package de.mlte.soundboard

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class EditActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        val captionEditText = findViewById<EditText>(R.id.captionEditText)
        captionEditText.setText(intent.getStringExtra("caption"))

        val okButton = findViewById<Button>(R.id.okButton)
        okButton.setOnClickListener {
            val intent = Intent()
            intent.putExtra("caption", captionEditText.text.toString())
            intent.putExtra("uri", currentUri)
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
        }
    }
}
