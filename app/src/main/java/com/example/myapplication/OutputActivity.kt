package com.example.myapplication

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class OutputActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_output)

        val bundle = intent.extras
        if(bundle!=null) {
            Log.e("Output",bundle.getString("imgFile").toString())
            val imgFile = File(bundle.getString("imgFile").toString())
            if(imgFile.exists()) {
                val imgView = findViewById<ImageView>(R.id.imageView2)
                val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                imgView.setImageBitmap(imgBitmap)
            } else {
                Log.e("OutputActivity", "File does not found!")
            }
        }
    }

    fun onOutputBtnClick(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}