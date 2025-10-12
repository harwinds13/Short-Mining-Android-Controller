package com.harry.shortmining

import FirestoreService
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.harry.shortmining.adapters.ClientAdapter
import com.harry.shortmining.models.Client
import org.json.JSONObject

class ClientSheetView : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var clientAdapter: ClientAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private val clientList = mutableListOf<Client>()
    private lateinit var db: FirebaseFirestore
    companion object {
        private const val TAG = "MainActivity"
        private const val COLLECTION_NAME = "client_sheet" // Change this to your collection name
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_sheet_view)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Client List"
        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        setupRecyclerView()
        fetchClientsWithRealtimeUpdates()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_client_sheet_view, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { // Handle back button press
                finish()
                true
            }
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showFilterDialog() {
        val filterOptions = arrayOf("All", "Submitted", "Waiting", "Processing", "Token Expired", "System Interrupt", "Finished")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Filter Clients")
        builder.setItems(filterOptions) { _, which ->
            val selectedFilter = filterOptions[which]
            applyFilter(selectedFilter)
        }
        builder.show()
    }

    private fun applyFilter(filter: String) {
        val filteredList = when (filter) {
            "Submitted" -> clientList.filter { it.status == "submitted" }
            "Waiting" -> clientList.filter { it.status == "waiting" }
            "Processing" -> clientList.filter { it.status == "processing" }
            "Token Expired" -> clientList.filter { it.status == "token_expired" }
            "System Interrupt" -> clientList.filter { it.status == "system_interrupt" }
            "Finished" -> clientList.filter { it.status == "finished" }
            else -> clientList
        }
        clientAdapter.updateList(filteredList)
    }

    private fun setupRecyclerView() {
        clientAdapter = ClientAdapter(clientList){
            client -> handleReLaunch(client)
        }

        recyclerView.apply {
            layoutManager = GridLayoutManager(this@ClientSheetView, 2)
            adapter = clientAdapter
            setHasFixedSize(true)
        }
    }

    private fun handleReLaunch(client: Client) {
        Toast.makeText(this, "Re-Launching for ${client.clientName}", Toast.LENGTH_SHORT).show()
        val firestoreService = FirestoreService()
        if (client.status == "system_interrupt")
            firestoreService.updateStatusBYID( COLLECTION_NAME, client.bbCandidateId, "submitted")
        if (client.status == "finished"){
            val intent = Intent(this, WebView::class.java)
            if(client.job.isNotEmpty()){
                val jobId = client.job
                val schId = client.sch
                val applicationId = client.applicationId
                intent.putExtra("url", "https://hiring.amazon.ca/application/ca/?CS=true&jobId=$jobId&locale=en-CA&scheduleId=$schId&ssoEnabled=1#/resume-application?CS=true&jobId=$jobId&locale=en-CA&scheduleId=$schId&ssoEnabled=1&applicationId=$applicationId")
            }else{
                intent.putExtra("url", "https://hiring.amazon.ca/app#/myApplications?intcmpid=gotomyjobsleft")
            }

            intent.putExtra("accessToken", client.fullLocal.accessToken)
            intent.putExtra("awswaf_session_storage", client.fullLocal.awswaf_session_storage)
            intent.putExtra("bbCandidateId", client.fullLocal.bbCandidateId)
            intent.putExtra("idToken", client.fullLocal.idToken)
            intent.putExtra("awswaf_token_refresh_timestamp", client.fullLocal.awswaf_token_refresh_timestamp)
            intent.putExtra("awswaf_token_refresh_timestamp", client.fullLocal.refreshToken)
            intent.putExtra("sessionToken", client.fullLocal.sessionToken)
            intent.putExtra("sfCandidateId", client.fullLocal.sfCandidateId)


            firestoreService.updateStatusBYID("client_sheet", client.bbCandidateId,"documentation")
            startActivity(intent)

        }
        if (client.status == "token_expired"){
            val intent = Intent(this, WebView::class.java)
            intent.putExtra("url", "https://hiring.amazon.ca/app#/login?redirectUrl=https%3A%2F%2Fhiring.amazon.ca%2F")
            intent.putExtra("accessToken", client.fullLocal.accessToken)
            intent.putExtra("awswaf_session_storage", client.fullLocal.awswaf_session_storage)
            intent.putExtra("bbCandidateId", client.fullLocal.bbCandidateId)
            intent.putExtra("idToken", client.fullLocal.idToken)
            intent.putExtra("awswaf_token_refresh_timestamp", client.fullLocal.awswaf_token_refresh_timestamp)
            intent.putExtra("awswaf_token_refresh_timestamp", client.fullLocal.refreshToken)
            intent.putExtra("sessionToken", client.fullLocal.sessionToken)
            intent.putExtra("sfCandidateId", client.fullLocal.sfCandidateId)

            startActivity(intent)

        }
    }
    private fun fetchClientsWithRealtimeUpdates() {
        showLoading(true)

        db.collection(COLLECTION_NAME)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Listen failed.", error)
                    showLoading(false)
                    Toast.makeText(
                        this,
                        "Error: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                clientList.clear()
                for (doc in snapshots!!) {
                    val client = doc.toObject(Client::class.java)
                    client.bbCandidateId = doc.id
                    clientList.add(client)
                }
                clientList.sortByDescending { it.expireTime }
                clientAdapter.updateList(clientList)
                showLoading(false)

                if (clientList.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                } else {
                    tvEmptyState.visibility = View.GONE
                }
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }




}