package com.shrutislegion.sportify

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.shrutislegion.sportify.lender_activities.LenderHomeActivity
import com.shrutislegion.sportify.player_activities.PlayerHomeActivity
import kotlinx.android.synthetic.main.activity_plsign_in.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
class PLSignInActivity : AppCompatActivity() {

    private val RC_SIGN_IN: Int = 123
    private val TAG = "SignInActivity Tag"
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plsign_in)

        //google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = Firebase.auth

        PLSignInButton.setOnClickListener {
            signIn()
        }
    }

    override fun onStart(){
        super.onStart()
        val currentlander = auth.currentUser
        updateUI(currentlander)
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            // Google Sign In was successful, authenticate with Firebase
            val account = completedTask.getResult(ApiException::class.java)!!
            Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            // Google Sign In failed, update UI appropriately
            Log.w(TAG, "Google sign in failed", e)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        PLSignInButton.visibility = View.GONE
        PLprogressBarSignIn.visibility = View.VISIBLE

        GlobalScope.launch(Dispatchers.IO) {
            val auth = auth.signInWithCredential(credential).await()
            val firebaseUser = auth.user
            withContext(Dispatchers.Main){
                updateUI(firebaseUser)
            }
        }
    }

    private fun updateUI(firebaseUser: FirebaseUser?) {

        if (firebaseUser!=null){

            val checkId = firebaseUser.uid
            var check: Boolean = false
            var check1: Boolean = false
            var check2: Boolean = false

            Firebase.firestore.collection("Landers")
                .get().addOnSuccessListener { result->
                    for(document in result){
                        check1 = true
                        if(document.id == checkId){
                            check = true
                            Toast.makeText(this,"Logged in as Lender", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LenderHomeActivity::class.java))
                            finish()
                            break
                        }
                    }
                }

            Handler(Looper.getMainLooper()).postDelayed({
                if(!check) {
                    Firebase.firestore.collection("users")
                        .get().addOnSuccessListener { result ->
                            for (document in result) {
                                check2 = true
                                if (document.id == checkId) {
                                    check = true
                                    Toast.makeText(this,"Logged in as Player", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, PlayerHomeActivity::class.java))
                                    finish()
                                    break
                                }
                            }
//                            Toast.makeText(this,"$check + $check1 + $check2", Toast.LENGTH_LONG).show()
                            if(check1 && check2 && !check){

                                MaterialAlertDialogBuilder(this).also {
                                    // set title for dailog box
                                    it.setTitle("Account doesn't exists")
                                    // set message for dialog box
                                    it.setMessage("Want to Register as Complex owner or Player?")
                                    // set icon for dialog box
                                    it.setIcon(R.drawable.ic_baseline_account_box_24)

                                    // perform positive action which deletes details from the lender activity and player activity
                                    it.setPositiveButton("CONTINUE") { dialogInterface, which ->

                                        googleSignInClient.signOut().addOnCompleteListener{
                                            Firebase.auth.signOut()
                                        }
                                        startActivity(Intent(this, RegistrationActivity::class.java))
                                        finish()

                                    }

                                    // performs neutral action
                                    it.setNeutralButton("CANCEL"){
                                            dialogInterface, which->

                                        googleSignInClient.signOut().addOnCompleteListener{
                                            Firebase.auth.signOut()
                                        }
                                        PLSignInButton.visibility = View.VISIBLE
                                        PLprogressBarSignIn.visibility = View.GONE

                                    }

                                    // performs negative/NO action
                                    it.setNegativeButton("NO"){
                                            dialogInterface, which->

                                        googleSignInClient.signOut().addOnCompleteListener{
                                            Firebase.auth.signOut()
                                        }
                                        Toast.makeText(this, "Please use a different account to login", Toast.LENGTH_SHORT).show()
                                        PLSignInButton.visibility = View.VISIBLE
                                        PLprogressBarSignIn.visibility = View.GONE

                                    }

                                    // create the AlertDialogBox
                                    val alertDialog: androidx.appcompat.app.AlertDialog = it.create()
                                    alertDialog.setCancelable(false)
                                    alertDialog.show()

                                }
                            }
                        }
                }
            },1000)


        } else {
            PLSignInButton.visibility = View.VISIBLE
            PLprogressBarSignIn.visibility = View.GONE
//            Toast.makeText(this, "Try Again!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, RegistrationActivity::class.java))
    }
}