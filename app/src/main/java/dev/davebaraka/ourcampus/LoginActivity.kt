package dev.davebaraka.ourcampus

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.*
import java.net.InetAddress

class LoginActivity : AppCompatActivity() {

    val RC_SIGN_IN = 2021
    val utilities = Utilities.instance
    var validated = false
    private lateinit var btnLogIn : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build())
        btnLogIn = findViewById<Button>(R.id.login)

        if (FirebaseAuth.getInstance().currentUser != null) {
            startActivityForResult(
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .setTheme(R.style.LoginTheme)
                    .build(),
                RC_SIGN_IN)
        } else {
            btnLogIn.visibility = View.VISIBLE
            btnLogIn.isEnabled = true
        }

        btnLogIn.setOnClickListener {
            btnLogIn.isEnabled = false
            btnLogIn.visibility = View.GONE
            startActivityForResult(
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .setTheme(R.style.LoginTheme)
                    .build(),
                RC_SIGN_IN)
        }

    }

    override fun onBackPressed() {
        return
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            //val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                GlobalScope.launch(Dispatchers.Main) {
                    val user = FirebaseAuth.getInstance().currentUser
                    val email = user!!.email!!
                    validateStudent(email.dropLast(13))
                    if (email.takeLast(13) == "@wesleyan.edu" && validated) {
                        val userUID = FirebaseAuth.getInstance().currentUser!!.uid
                        val database = FirebaseDatabase.getInstance().getReference("Users/" + userUID)
                        database.addListenerForSingleValueEvent(object: ValueEventListener{
                            override fun onCancelled(p0: DatabaseError) {
                                AuthUI.getInstance().signOut(applicationContext)
                                utilities.toast(
                                    "An unknown error occurred",
                                    "ERROR",
                                    applicationContext,
                                    null)
                                btnLogIn.visibility = View.VISIBLE
                                btnLogIn.isEnabled = true
                            }
                            override fun onDataChange(userSnap: DataSnapshot) {
                                if (userSnap.hasChild("display") && userSnap.hasChild("year") && userSnap.hasChild("incognito") && userSnap.hasChild("user")) {
                                    //Start Activity\
                                    finishAndRemoveTask()
                                    val intent = Intent(applicationContext, MainActivity::class.java)
                                    startActivity(intent)
                                } else {
                                    finishAndRemoveTask()
                                    val intent = Intent(applicationContext, GetStartedActivity::class.java)
                                    startActivity(intent)
                                }
                            }
                        })
                    } else {
                        AuthUI.getInstance().signOut(applicationContext)
                        utilities.toast(
                            "Wesleyan Domain Error",
                            "ERROR",
                            applicationContext,
                            null)
                        btnLogIn.visibility = View.VISIBLE
                        btnLogIn.isEnabled = true
                    }
                }
            } else {
                btnLogIn.visibility = View.VISIBLE
                btnLogIn.isEnabled = true
            }
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
            btnLogIn.visibility = View.VISIBLE
            btnLogIn.isEnabled = true
        }
    }

    suspend fun validateStudent(user : String) {
        return withContext(Dispatchers.IO) {
            try {
                val ipa = InetAddress.getByName(user + ".mail.wesleyan.edu").toString()
                if (ipa.takeLast(2) == "66") validated = true
            } catch (e: Throwable) {
                validated = false
            }
        }
    }
}
