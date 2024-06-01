package com.example.myapplication

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class ContentActivity : ComponentActivity() {
    private val TAG = "ContentActivity"
    private var filepath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content)

        val permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "HERE! not equal to"+permission.toString())
            ActivityCompat.requestPermissions(
                this,
                mutableListOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE).toTypedArray(),
                1
            )
        }

        loadImage()
    }

    private fun loadImage() {
        val bundle = intent.extras
        if(bundle!=null) {
            filepath = bundle.getString("imgFile").toString()
            val imgFile = File(filepath)
            if (imgFile.exists()) {
                val imgView = findViewById<ImageView>(R.id.imageView)
                val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                imgView.setImageBitmap(imgBitmap)
            } else {
                Log.e("ContentActivity", "File does not found!")
            }
        }
    }

    fun onBtn3Click(view: View) {
        finish()
    }

    fun onRecognizing(view: View) {
        Log.e("Content","Hi!Here to ready to recognize.")
        val digitsClassifyHelper = DigitsClassifyHelper(filepath)
        digitsClassifyHelper.init()
        val res = digitsClassifyHelper.toSave(assets)

        Toast.makeText(this, res.second, Toast.LENGTH_SHORT).show()

        if (res.first) {
            val bundle = Bundle()
            bundle.putString("imgFile", res.second)
            val intent = Intent(this, OutputActivity::class.java)
            intent.putExtras(bundle)
            startActivity(intent)
        }
    }
}
