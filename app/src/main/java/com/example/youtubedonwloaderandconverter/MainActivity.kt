package com.example.youtubedonwloaderandconverter

import android.app.DownloadManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.JsonReader
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.StringReader
import java.net.URL
import java.util.*

class MainActivity : AppCompatActivity(){
    val API_BASE_URL = "https://youtube-downloader-converter.herokuapp.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar!!.title = ""

        val urlField = findViewById<EditText>(R.id.text_input)

        val radioType = findViewById<RadioGroup>(R.id.radio_type)

        val downloadBtn = findViewById<Button>(R.id.btn_download)
        downloadBtn.setOnClickListener{
            val url = urlField.text.toString()
            val radioChecked = findViewById<RadioButton>(radioType.checkedRadioButtonId)
            val type = radioChecked.text.toString()
            CoroutineScope(Dispatchers.IO).launch {
                val videos = if (url.contains("/playlist?"))  downloadPlaylist(url) else listOf(url)
                for(video in videos){
                    downloadInBackground(video, type)
                }
            }
        }
    }

    fun downloadPlaylist(ytUrl: String) : List<String>{
        val url = "$API_BASE_URL/playlist?url=$ytUrl"
        val responseText = URL(url).readText()
        val sreader = StringReader(responseText)
        val jreader = JsonReader(sreader)
        val urls = LinkedList<String>()
        jreader.beginArray()
        while(jreader.hasNext()){
            urls.add(jreader.nextString())
        }

        return urls
    }

    fun downloadFilename(ytUrl: String) : String{
        val url = "$API_BASE_URL/name?url=$ytUrl"
        return URL(url).readText()
    }

    fun downloadInBackground(url: String, type: String){
        CoroutineScope(Dispatchers.IO).launch {
            val filename = downloadFilename(url)
            withContext(Dispatchers.Main){
                download(url, filename, type)
            }
        }
    }

    fun download(ytUrl: String, filename: String, type: String){
        val url = "$API_BASE_URL/download?type=$type&url=$ytUrl"
        val request = DownloadManager.Request(Uri.parse(url))
        request.setAllowedNetworkTypes(
            DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
        )
        request.setTitle("$filename.$type")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
            "$filename.$type"
        )
        val downloadManager = applicationContext.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }
}