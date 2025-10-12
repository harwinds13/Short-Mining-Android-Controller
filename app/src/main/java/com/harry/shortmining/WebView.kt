package com.harry.shortmining

import FirestoreService
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import org.json.JSONObject

class WebView : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var executeServiceButton:Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)
        val url = intent.getStringExtra("url") ?: "https://your-url.com"
        val accessToken = intent.getStringExtra("accessToken") ?: ""
        val awswaf_session_storage = intent.getStringExtra("awswaf_session_storage") ?: ""
        val bbCandidateId = intent.getStringExtra("bbCandidateId") ?: ""
        val idToken = intent.getStringExtra("idToken") ?: ""
        val awswaf_token_refresh_timestamp = intent.getStringExtra("awswaf_token_refresh_timestamp") ?: ""
        val sessionToken = intent.getStringExtra("sessionToken") ?: ""
        val refreshToken = intent.getStringExtra("refreshToken") ?: ""
        val sfCandidateId = intent.getStringExtra("sfCandidateId") ?: ""



        sharedPreferences = getSharedPreferences("LocalStorage", Context.MODE_PRIVATE)
        executeServiceButton = findViewById(R.id.executeServiceButtons)
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        WebView.setWebContentsDebuggingEnabled(false)
        webView.addJavascriptInterface(WebAppInterface(), "AndroidInterface")
        webView.loadUrl(url)
        val reloadButton = findViewById<Button>(R.id.reloadButton)
        val webView = findViewById<WebView>(R.id.webView)
        reloadButton.setOnClickListener {
            webView.reload()

        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if(accessToken.isNotEmpty()){
                    val js = """
                    (function() {
                        try {
                         localStorage.setItem("accessToken", "$accessToken");
                         localStorage.setItem("awswaf_session_storage", "$awswaf_session_storage");
                         localStorage.setItem("bbCandidateId", "$bbCandidateId");
                         localStorage.setItem("idToken", "$idToken");
                         localStorage.setItem("awswaf_token_refresh_timestamp", "$awswaf_token_refresh_timestamp");
                         localStorage.setItem("sessionToken", "$sessionToken");
                         localStorage.setItem("refreshToken", "$refreshToken");
                         localStorage.setItem("sfCandidateId", "$sfCandidateId");
                        } catch (e) {
                            console.error("localStorage injection error", e);
                        }
                    })();
                """.trimIndent()
                    view?.evaluateJavascript(js, null)
                }

            }
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }
        }

        executeServiceButton.setOnClickListener {
            webView.evaluateJavascript(
                """
            (function() {
                try {
                    var requiredData = {};
                    if (localStorage.getItem('bbCandidateId')) {
                        requiredData.bbCandidateId = localStorage.getItem('bbCandidateId');
                        requiredData.accessToken = localStorage.getItem('accessToken');
                        geoInfo = localStorage.getItem('geoInfo');
                        persist = localStorage.getItem('persist:root');
                        request = JSON.parse(JSON.parse(persist)['request'])
                        distance = request['distance']['value']
                        workHours = request['workHour']['value']
                        geoJson = JSON.parse(geoInfo);
                        requiredData.lat = geoJson['lat'];
                        requiredData.lng = geoJson['lng'];
                        requiredData.location = geoJson['label'];
                        requiredData.subRegion = geoJson['subRegion'];
                        requiredData.distance = distance;
                        requiredData.jobType = workHours;
                        requiredData.fullLocal = localStorage

                        AndroidInterface.sendLocalStorage(JSON.stringify(requiredData));
                    } else {
                        console.warn("bbCandidateId not found in localStorage.");
                    }
                } catch (error) {
                    console.error("Error reading localStorage:", error);
                }
            })();
            """.trimIndent(), null
            )
        }

    }


    inner class WebAppInterface {
        @JavascriptInterface
        fun sendLocalStorage(data: String) {
            if (data.isNotEmpty()) {
                storeDataInSharedPreferences(data)
            } else {
                Log.w("SHORT_MINING", "No data found in localStorage.")
            }
        }
    }


    private fun storeDataInSharedPreferences(data: String) {
        val firestore = FirestoreService()
        val currentTime = System.currentTimeMillis()
        val jsonObject = JSONObject(data)
        val bbCandidateId = jsonObject.getString("bbCandidateId")
        val expireTime = currentTime + 2 * 60 * 60 * 1000 // 2 hours from now

        val storedData = sharedPreferences.getString(bbCandidateId, null)
        if (storedData != null) {
            val storedJson = JSONObject(storedData)
            val storedExpireTime = storedJson.getLong("expireTime")

            // Data exists and is not expired
            if (currentTime < storedExpireTime) {
                Log.d("SHORT_MINING", "Data for $bbCandidateId already exists and is not expired.")
                storedJson.put("lng", jsonObject.getDouble("lng"))
                storedJson.put("lat", jsonObject.getDouble("lat"))
                storedJson.put("jobType", jsonObject.getString("jobType"))
                storedJson.put("distance", jsonObject.getString("distance"))
                storedJson.put("location", jsonObject.getString("location"))
                storedJson.put("subRegion", jsonObject.getString("subRegion"))

                firestore.get_doc_status(this, "client_sheet", bbCandidateId) { status ->
                    if (status in listOf("finished","system_interrupt", "generic_error")) {
                        storedJson.put("status", "submitted")
                    }
                    else{
                        storedJson.put("status", status)
                    }
                }
                // Save updated data
                sharedPreferences.edit().apply {
                    putString(bbCandidateId, storedJson.toString())
                    apply()
                }


                firestore.addDocument(this,"client_sheet", bbCandidateId, storedJson)

                return@storeDataInSharedPreferences
            }
        }
        val newData = jsonObject.apply {
            put("expireTime", expireTime)
            put("status", "submitted")
        }


        val apiService = ApiService(jsonObject.getString("accessToken"))
         val candidateDetails  = apiService.queryCandidate(bbCandidateId)
        candidateDetails?.let { (firstName, phoneNumber, emailId) ->
            newData.apply {
                put("clientName", firstName)
                put("clientPhoneNumber", phoneNumber)
                put("clientEmail", emailId)
            }
        } ?: Log.e("CandidateDetails", "Failed to fetch candidate details.")




        Log.i("SHORT_MINING", "Storing data for $bbCandidateId: $newData")
        sharedPreferences.edit().apply {
                putString(bbCandidateId, newData.toString())
                    apply()
        }
            Log.d("SharedPreferences", "Data stored successfully.")

        firestore.addDocument(this,"client_sheet", bbCandidateId, newData)

    }

}