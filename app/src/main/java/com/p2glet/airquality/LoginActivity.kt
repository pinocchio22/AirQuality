package com.p2glet.airquality

import android.R
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.p2glet.airquality.databinding.ActivityLoginBinding
import java.util.*


class LoginActivity : AppCompatActivity() {

    lateinit var binding: ActivityLoginBinding

    var googleSignInClient : GoogleSignInClient ?= null
    var GOOGLE_LOGIN_CODE = 9001
    var auth : FirebaseAuth ?= null
    var callbackManager : CallbackManager ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        // range of Alpha : 0~255
        binding.iconLogo.drawable.alpha = 180

        binding.emailBtn.setOnClickListener {
            signinAndSignup()
        }

        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("33521507868-da69s5m001eis0pmmrifa84v01umoo15.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this,gso)
        callbackManager = CallbackManager.Factory.create()

        binding.loginGoogle.setOnClickListener {
            googleLogin()
        }
        binding.loginFacebook.setOnClickListener {
            facebookLogin()
        }
//        googleLogout()
    }

    override fun onStart() {
        super.onStart()
        moveMain(auth?.currentUser)
    }

    fun moveMain(user : FirebaseUser?) {
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    fun googleLogin() {
        var signInClient = googleSignInClient?.signInIntent
        startActivityForResult(signInClient,GOOGLE_LOGIN_CODE)
    }

    fun facebookLogin() {
        LoginManager.getInstance()
            .logInWithReadPermissions(this, Arrays.asList("public_profile", "email"))

        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult?) {
                    handleFacebookAccessToken(result?.accessToken)
                }

                override fun onCancel() {

                }

                override fun onError(error: FacebookException?) {

                }

            })
    }

    fun handleFacebookAccessToken(token : AccessToken?) {
        var credential = FacebookAuthProvider.getCredential(token?.token!!)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    moveMain(task.result?.user)
                } else {
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    moveMain(task.result?.user)
                } else {
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode,resultCode,data)
        if (requestCode == GOOGLE_LOGIN_CODE) {
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data!!)
            if (result!!.isSuccess) {
                var account = result.signInAccount
                firebaseAuthWithGoogle(account)
            }
        }
    }

    fun signinAndSignup() {
        when {
            binding.loginEmail.text.toString().isEmpty() -> {
                Toast.makeText(this, "이메일을 입력하세요", Toast.LENGTH_LONG).show()
            }
            binding.loginPsw.text.toString().isEmpty() -> {
                Toast.makeText(this, "비밀번호를 입력하세요", Toast.LENGTH_LONG).show()
            }
            else -> {
                auth?.createUserWithEmailAndPassword(binding.loginEmail.text.toString(), binding.loginPsw.text.toString())
                    ?.addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Log.d("TAG", "1")
                            //Creating a user account
                            moveMain(task.result?.user) }
                        else if (task.exception?.message.isNullOrEmpty()){
                            Log.d("TAG", "2")
                            //show the error messagge
                            Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show() }
                        else {
                            Log.d("TAG", "3")
                            //Login if you have account
                            signinEmail()
                        }
                    }
            }
        }
    }

    fun signinEmail(){
        auth?.signInWithEmailAndPassword(
            binding.loginEmail.text.toString(),
            binding.loginPsw.text.toString()
        )?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                //Login
                Log.d("TAG", "성공", task.exception)
                moveMain(task.result?.user)
            }else {
                //Show the error message
                Log.d("TAG", "실패", task.exception)
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
            }
        }
    }

//    fun googleLogout() {
//        binding.logoutBtn.setOnClickListener {
//            firebase.signOut()
//            // signOut() 호출 후 currentUser을 호출하면 null이 반환됩니다.
//            val user = firebase.currentUser
//
//            val opt = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
//            val client = GoogleSignIn.getClient(this, opt)
//            client.signOut()
//            client.revokeAccess()
//        }
//    }
}