package com.ky.bananacycles

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ky.bananacycles.ui.theme.BananaCyclesTheme

private const val FIREBASE_TEST_TAG = "FIREBASE_TEST"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BananaCyclesTheme {
                FirebaseTestingScreen(
                    onRunTest = { onResult ->
                        runFirebaseTest(onResult)
                    }
                )
            }
        }
    }

    private fun runFirebaseTest(onResult: (FirebaseTestResult) -> Unit) {
        onResult(
            FirebaseTestResult(
                title = "Testing Firebase...",
                message = "Menulis data test ke Firestore."
            )
        )

        try {
            val app = FirebaseApp.getInstance()
            val projectId = app.options.projectId ?: "project id tidak ditemukan"
            val db = FirebaseFirestore.getInstance()
            val testDocument = db.collection("firebase_test")
                .document("main_activity_test")

            val testData = hashMapOf<String, Any>(
                "message" to "Hello Firebase from BananaCycles",
                "source" to "MainActivity testing screen",
                "clientTimestamp" to System.currentTimeMillis(),
                "serverTimestamp" to FieldValue.serverTimestamp()
            )

            testDocument
                .set(testData)
                .addOnSuccessListener {
                    Log.d(FIREBASE_TEST_TAG, "Write success. Project: $projectId")

                    testDocument
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val savedMessage = snapshot.getString("message")
                            Log.d(FIREBASE_TEST_TAG, "Read success: $savedMessage")
                            onResult(
                                FirebaseTestResult(
                                    title = "Firebase berhasil terkoneksi",
                                    message = "Write dan read Firestore sukses.",
                                    detail = "Project: $projectId\nCollection: firebase_test\nDocument: main_activity_test\nMessage: $savedMessage"
                                )
                            )
                        }
                        .addOnFailureListener { error ->
                            Log.e(FIREBASE_TEST_TAG, "Read failed", error)
                            onResult(
                                FirebaseTestResult(
                                    title = "Write sukses, read gagal",
                                    message = error.message ?: "Firestore read gagal tanpa pesan error.",
                                    detail = "Project: $projectId"
                                )
                            )
                        }
                }
                .addOnFailureListener { error ->
                    Log.e(FIREBASE_TEST_TAG, "Write failed", error)
                    onResult(
                        FirebaseTestResult(
                            title = "Firebase gagal terkoneksi",
                            message = error.message ?: "Firestore write gagal tanpa pesan error.",
                            detail = "Cek google-services.json, koneksi internet, dan Firestore rules."
                        )
                    )
                }
        } catch (error: Exception) {
            Log.e(FIREBASE_TEST_TAG, "Firebase test crashed", error)
            onResult(
                FirebaseTestResult(
                    title = "Firebase belum siap",
                    message = error.message ?: "Firebase gagal diinisialisasi.",
                    detail = "Pastikan google-services.json sesuai package com.ky.bananacycles."
                )
            )
        }
    }
}

@Composable
private fun FirebaseTestingScreen(
    onRunTest: ((FirebaseTestResult) -> Unit) -> Unit
) {
    var result by remember {
        mutableStateOf(
            FirebaseTestResult(
                title = "Firebase Test",
                message = "Menunggu test dijalankan."
            )
        )
    }

    LaunchedEffect(Unit) {
        onRunTest { result = it }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "BananaCycles Firebase Test",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    result.detail?.let { detail ->
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        onRunTest { result = it }
                    }
                ) {
                    Text("Test Lagi")
                }
            }
        }
    }
}

private data class FirebaseTestResult(
    val title: String,
    val message: String,
    val detail: String? = null
)
