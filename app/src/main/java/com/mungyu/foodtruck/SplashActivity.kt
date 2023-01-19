package com.mungyu.foodtruck

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.trusted.sharing.ShareTarget.FileFormField.KEY_NAME
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mungyu.foodtruck.databinding.ActivitySplashBinding
import com.mungyu.foodtruck.model.User
import java.util.regex.Pattern

class SplashActivity : AppCompatActivity() {
    private var SPLASH_TIME = 500L
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_200)
        initLayout()
        initFirebase()
    }

    private fun initLayout() {
        with(binding.email) {
            editText?.addTextChangedListener {
                it?.let {
                    error = if(android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches()) {
                        null
                    } else {
                        getString(R.string.write_your_email)
                    }
                }
            }
        }
        with(binding.password) {
            editText?.addTextChangedListener {
                it?.let {
                    error = if(Pattern.matches("([\\w\\d]*[~!@#$%^&*()_+=:;,./<>?{}]+[\\w\\d]*)", it)) {
                        null
                    } else {
                        getString(R.string.include_one_or_more_special_characters)
                    }
                }
            }
        }

        // TODO
        /*
         * binding으로 activity_splash레이아웃 묶기
         * currentUser가 null이면 email, password, 로그인 혹은 가입하기,  google SIgn in 버튼 보이기
         * currentUser가 null이 아니면 nextStep
         * email, password입력 후 로그인 혹은 가입하기 버튼 누르면 이메일 인증 진행하기
         *   회원가입한 사용자가 있으면 그 회원으로 로그인
         *   회원가입하지 않았으면 회원가입 진행
         * google sign in 버튼 누르면 구글 로그인 인증 진행하기
         */
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
                registerUserInfo()
//                nextStep()
            } else {
                Toast.makeText(this@SplashActivity, "Login 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUserInfo() {
        val pref =
            applicationContext.getSharedPreferences(FOOD_TRUCK, Context.MODE_PRIVATE)
        val name = pref.getString(KEY_NAME, "unknown")
        val userRef = FirebaseDatabase.getInstance().reference.child("users")
        userRef.orderByChild("email").equalTo(auth.currentUser?.email)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.i(TAG, "onDataChange count:${snapshot.children.count()}, name:$name")
                    if (snapshot.children.count() == 0 && name == "unknown") {
                        val userRef = FirebaseDatabase.getInstance().reference.child("users").push()
                        val editor = pref.edit()
                        editor.putString("key", userRef.key)
                        editor.commit()
                        userRef.setValue(
                            User(
                                name = auth.currentUser?.displayName,
                                imageUrl = auth.currentUser?.photoUrl.toString(),
                                email = auth.currentUser?.email
                            )
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
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
//                nextStep()
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
        const val TAG = "FoodTruckSplash"
        const val RC_SIGN_IN = 9001
    }
}