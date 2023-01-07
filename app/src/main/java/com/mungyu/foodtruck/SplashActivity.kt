package com.mungyu.foodtruck

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class SplashActivity : AppCompatActivity() {
    private var SPLASH_TIME = 500L
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        initFirebase()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_SIGN_IN -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account!!)
                } catch (e: ApiException) {
                    FirebaseAnalytics.getInstance(this@SplashActivity)
                        .logEvent("RC_SIGN_IN", Bundle().apply {
                            putString("sign in", "failed")
                        })
                    Log.w(TAG, "Google sign in failed", e)
                    Toast.makeText(
                        this@SplashActivity,
                        "구글 계정 등록이 실패하였습니다.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth?.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Toast.makeText(this@SplashActivity, "환영합니다.", Toast.LENGTH_SHORT).show()
                nextStep()
            } else {
                Toast.makeText(this@SplashActivity, "Login 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initFirebase() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(application, gso)
        auth = FirebaseAuth.getInstance().apply {
            Log.d(TAG, "initFirebase currentUser:$currentUser")
            if (currentUser == null) {
                startActivityForResult(client.signInIntent, RC_SIGN_IN)
            } else {
                nextStep()
            }
        }
    }

    private fun nextStep() {
        val logoTimer: Thread = object : Thread() {
            override fun run() {
                try {
                    sleep(SPLASH_TIME)
                } catch (e: InterruptedException) {
                    Log.d("Exception", "Exception$e")
                } finally {
                    startActivity(Intent(this@SplashActivity, MapsActivity::class.java))
                }
                finish()
            }
        }
        logoTimer.start()
    }

    companion object {
        const val TAG = "SplashActivity"
        const val RC_SIGN_IN = 9001
    }
}