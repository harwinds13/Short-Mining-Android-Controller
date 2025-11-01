package com.harry.shortmining

import FirestoreService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class WebView : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var executeServiceButton:Button
    private lateinit var auth: FirebaseAuth
    private var userId: String? = null
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        auth = FirebaseAuth.getInstance()

        userId = intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_SHORT).show()
            goToLogin()
            return
        }
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
            showParametersDialog()
        }

    }
    private fun showParametersDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_parameters, null)

        val spinnerParam1 = dialogView.findViewById<Spinner>(R.id.spinnerParam1)
        val spinnerParam2 = dialogView.findViewById<Spinner>(R.id.spinnerParam2)
        val sharedPreferences = getSharedPreferences("VendorPrefs", MODE_PRIVATE)
        val company = sharedPreferences.getString("company", "Default Company")
        var companies: List<String>
        if(company == "Admin"){
            companies = listOf("HELP_HUB", "SKY_ATOZ", "HARGUN", "SHORT_MINING", "MANI")
        }else{
            companies = listOf(company!!)

        }
        val param1Options = companies
        val adapter1 = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, param1Options)
        spinnerParam1.adapter = adapter1

        // Define options for second dropdown
        val param2Options = arrayOf("1", "2", "3")
        val adapter2 = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, param2Options)
        spinnerParam2.adapter = adapter2

        AlertDialog.Builder(this)
            .setTitle("Select Parameters")
            .setView(dialogView)
            .setPositiveButton("Submit") { dialog, _ ->
                val param1 = spinnerParam1.selectedItem.toString()
                val param2 = spinnerParam2.selectedItem.toString()

                executeService(param1, param2)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
    private fun executeService(additionalParam1: String, additionalParam2: String) {
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
                        
                        // Add additional parameters
                        requiredData.vendor = "$additionalParam1";
                        requiredData.priority = "$additionalParam2";

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
    inner class WebAppInterface {
        @JavascriptInterface
        fun sendLocalStorage(data: String) {
            if (data.isNotEmpty()) {
                storeDataInDataBase(data)
            } else {
                Log.w("SHORT_MINING", "No data found in localStorage.")
            }
        }
    }


    private fun storeDataInDataBase(data: String) {
        val firestore = FirestoreService()
        val currentTime = System.currentTimeMillis()
        val jsonObject = JSONObject(data)
        val bbCandidateId = jsonObject.getString("bbCandidateId")
        val expireTime = currentTime + 118 * 60 * 1000
        var isFirstAttempt = false

        firestore.retrieveDocumentByID( "client_sheet", bbCandidateId){
            doc ->
            if (doc != null ) {
                val status = doc["status"] as? String
                if (status in listOf("finished","system_interrupt", "generic_error","documentation","token_expired")
                        && doc["expireTime"] != null && (doc["expireTime"] as Long) > currentTime) {
                    jsonObject.put("status", "submitted")
                    jsonObject.put("status_new", "submitted")
                    jsonObject.put("updatedAt", currentTime)
                    firestore.addDocument(this,"client_sheet", bbCandidateId, jsonObject)
                } else if (status in listOf("processing","token_expired","finished","documentation","submitted")
                        && doc["expireTime"] != null && (doc["expireTime"] as Long) < currentTime) {

                    jsonObject.put("status", "submitted")
                    jsonObject.put("status_new", "submitted")
                    jsonObject.put("expireTime", expireTime)
                    jsonObject.put("updatedAt", currentTime)
                    firestore.addDocument(this,"client_sheet", bbCandidateId, jsonObject)
                }else if(status in listOf("processing")
                    && doc["expireTime"] != null && (doc["expireTime"] as Long) > currentTime){
                    Toast.makeText(this, "Application is in process, changes cannot be committed.", Toast.LENGTH_LONG).show()
                }
            } else {
                lifecycleScope.launch {
                    val apiService = ApiService(jsonObject.getString("accessToken"))
                    val candidateDetails = withContext(Dispatchers.IO) {
                        apiService.queryCandidate(bbCandidateId)
                    }

                    candidateDetails?.let { (firstName, phoneNumber, emailId) ->
                        jsonObject.put("clientName", firstName)
                        jsonObject.put("clientPhoneNumber", phoneNumber)
                        jsonObject.put("clientEmail", emailId)
                    } ?: Log.e("CandidateDetails", "Failed to fetch candidate details.")

                    firestore.addDocument(this@WebView, "client_sheet", bbCandidateId, jsonObject)
                }
                jsonObject.put("status", "submitted")
                jsonObject.put("status_new", "submitted")
                jsonObject.put("expireTime", expireTime)
                jsonObject.put("updatedAt", currentTime)
                firestore.addDocument(this,"client_sheet", bbCandidateId, jsonObject)
            }
        }


    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}