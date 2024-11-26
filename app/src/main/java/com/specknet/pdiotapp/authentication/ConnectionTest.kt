import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import android.content.Context
import android.os.Bundle
import android.provider.Settings.Global.putString

object FirebaseTestUtil {

    private const val TAG = "FirebaseTestUtil"

    // Initialize Firebase Analytics and log a test event
    fun testAnalytics(context: Context) {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, "test_connection")
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
        Log.d(TAG, "Logged Analytics test event.")
    }

    // Test Firebase Firestore by writing data to it
    fun testFirestore() {
        val db = FirebaseFirestore.getInstance()
        val testData = hashMapOf("name" to "Test User", "age" to 25)

        db.collection("testCollection").document("testDoc")
            .set(testData)
            .addOnSuccessListener {
                Log.d(TAG, "Firestore write success: Document successfully written!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Firestore write failure", e)
            }
    }

    // Test Firebase Authentication by signing in anonymously
    fun testAuth() {
        val auth = Firebase.auth
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d(TAG, "Auth success: Signed in anonymously as ${user?.uid}")
                } else {
                    Log.w(TAG, "Auth failure", task.exception)
                }
            }
    }
}