package com.harry.shortmining

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private lateinit var buttonEnableService: Button
    private lateinit var buttonClientViewActivity: Button

    private val logBuilder = StringBuilder()
    private lateinit var consoleTextView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var clearLogs: AppCompatImageButton
    private var fetchJobDetailsJob: Job? = null
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        consoleTextView = findViewById(R.id.consoleTextView)
        clearLogs = findViewById(R.id.buttonClearLogs)
        scrollView = findViewById(R.id.consoleScrollView)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val apiService = ApiService("")
        fetchJobDetailsJob = CoroutineScope(Dispatchers.IO).launch {

//            while (isActive) { // Check if the coroutine is active
//                try {
//                    val res = apiService.invokeGraphQlTOGetShifts()
//                    if (res == "No_Data_Found"){
//                        delay(120000)
//                    }
//                    fetchJobDetails(res)
//                } catch (e: Exception) {
//                    Log.e("SM", "Error in loop: ${e.message}")
//                    break
//                }
//                delay(1000) // Replace Thread.sleep with delay
//            }
        }
        clearLogs.setOnClickListener {
            logBuilder.clear()
            consoleTextView.text = ""
            try {
                Runtime.getRuntime().exec("logcat -c") // Clear logcat buffer
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Log.i("SM", "Logs cleared")
        }
        startReadingLogs(consoleTextView, scrollView)



        buttonEnableService = findViewById(R.id.button)
        buttonClientViewActivity = findViewById(R.id.buttonToList)
        buttonEnableService.setOnClickListener {
            val intent = Intent(this, WebView::class.java)
            intent.putExtra("url", "https://hiring.amazon.ca")
            startActivity(intent)
        }
        buttonClientViewActivity.setOnClickListener {
            startActivity(Intent(this, ClientSheetView::class.java))
        }
    }

    private fun fetchJobDetails(response: String) {
        try {
            val jsonResponse = JSONObject(response)
            val jobCards = jsonResponse
                .getJSONObject("data")
                .getJSONObject("searchJobCardsByLocation")
                .getJSONArray("jobCards")

            for (i in 0 until jobCards.length()) {
                val jobCard = jobCards.getJSONObject(i)
                val locationName = jobCard.getString("locationName")
                val jobTypeL10N = jobCard.getString("jobTypeL10N")
                val scheduleCount = jobCard.getInt("scheduleCount")
                val timestamp = System.currentTimeMillis()
                // Log or use the extracted values
                Log.i("SM", "Location: $locationName, Job Type: $jobTypeL10N, Schedule Count: $scheduleCount")

                val sharedPreferences = getSharedPreferences("JobDetails", MODE_PRIVATE)
                val editor = sharedPreferences.edit()

                val existingData = sharedPreferences.getString("jobDetails", "") ?: ""
                val newData = "Location: $locationName, Job Type: $jobTypeL10N, Schedule Count: $scheduleCount, Time: $timestamp"

                val updatedData = if (existingData.isNotEmpty()) {
                    "$existingData\n$newData"
                } else {
                    newData
                }

                editor.putString("jobDetails", updatedData)
                editor.apply()

            }
        } catch (e: Exception) {
            Log.e("SM", "Error parsing job details: ${e.message}")
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        fetchJobDetailsJob?.cancel()
    }
    private fun startReadingLogs(consoleTextView: TextView, scrollView: ScrollView) {

        Thread {
            try {
                val process = Runtime.getRuntime().exec("logcat -v time SM:I *:S")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let {
                        val coloredLine = when {
                            it.contains("D/SM") -> "<font color='#FFFF00'>$it</font><br>"
                            it.contains("I/SM") -> "<font color='#00FF00'>$it</font><br>"
                            it.contains("E/SM") -> "<font color='#FF0000'>$it</font><br>"
                            else -> "<font color='#FFFFFF'>$it</font><br>"
                        }
                        consoleTextView.post {
                            logBuilder.append(coloredLine)
                            consoleTextView.text = Html.fromHtml(logBuilder.toString(), Html.FROM_HTML_MODE_LEGACY)
                            scrollView.post {
                                scrollView.fullScroll(View.FOCUS_DOWN)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}