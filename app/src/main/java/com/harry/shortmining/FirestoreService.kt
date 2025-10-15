import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject

class FirestoreService() {
    private val db = FirebaseFirestore.getInstance()

    fun addDocument(context:Context,collectionName: String, bbCandidateId: String, data: JSONObject) {
        db.collection(collectionName).document(bbCandidateId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    db.collection(collectionName).document(bbCandidateId).update(data.toMap())
                        .addOnSuccessListener {
                            Log.d("Firestore", "DocumentSnapshot successfully written!")
                            Toast.makeText(context, "Data for updated successfully.", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error writing document.", Toast.LENGTH_LONG).show()
                            Log.w("Firestore", "Error writing document", e)
                        }
                } else {
                    Log.d("Firestore", "No such document")
                    db.collection(collectionName).document(bbCandidateId).set(data.toMap())
                        .addOnSuccessListener {
                            Log.d("Firestore", "DocumentSnapshot successfully written!")
                            Toast.makeText(context, "Data for updated successfully.", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error writing document.", Toast.LENGTH_LONG).show()
                            Log.w("Firestore", "Error writing document", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error fetching document", e)
                Toast.makeText(context, "Error fetching document", Toast.LENGTH_LONG).show()
            }


    }

    fun get_doc_status(context: Context, collectionName: String, bbCandidateId: String, onResult: (String?) -> Unit ){
        db.collection(collectionName).document(bbCandidateId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val doc_data = document.data
                    if (doc_data != null) {
                        val status = doc_data["status"] as? String
                        onResult(status)
                    } else {
                        Toast.makeText(context, "No data found in document.", Toast.LENGTH_LONG).show()
                        onResult(null)
                    }
                } else {
                    Log.d("Firestore", "No such document")
                    Toast.makeText(context, "No such document.", Toast.LENGTH_LONG).show()
                    onResult(null)
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error fetching document", e)
                Toast.makeText(context, "Error fetching document", Toast.LENGTH_LONG).show()
                onResult(null)
            }
    }

    fun fetchAllDocuments(collectionName: String, onResult: (List<Map<String, Any>>?) -> Unit) {
        db.collection(collectionName).get()
            .addOnSuccessListener { result ->
                val documents = result.map { it.data }
                onResult(documents)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error fetching documents", e)
                onResult(null)
            }
    }

    fun updateStatusBYID(collectionName: String, bbCandidateId: String, newStatus: String) {
        val updates = mapOf("status" to newStatus)
        db.collection(collectionName).document(bbCandidateId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("Firestore", "Status successfully updated!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error updating status", e)
            }
    }

    fun retrieveDocumentByID(collectionName: String, bbCandidateId: String, onResult: (Map<String, Any>?) -> Unit) {
        db.collection(collectionName).document(bbCandidateId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    onResult(document.data)
                } else {
                    Log.d("Firestore", "No such document")
                    onResult(null)
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error fetching document", e)
                onResult(null)
            }
    }

    private fun JSONObject.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = this[key]
            map[key] = when (value) {
                is JSONObject -> value.toMap()
                is org.json.JSONArray -> value.toList()
                else -> value
            }
        }
        return map
    }

    // Extension function to convert JSONArray to List
    private fun org.json.JSONArray.toList(): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until length()) {
            val value = this[i]
            list.add(
                when (value) {
                    is JSONObject -> value.toMap()
                    is org.json.JSONArray -> value.toList()
                    else -> value
                }
            )
        }
        return list
    }

}