package com.shrutislegion.sportify

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import android.widget.Toast
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.shrutislegion.sportify.databinding.ActivityMainBinding
import com.shrutislegion.sportify.modules.ComplexInfo
import kotlinx.android.synthetic.main.activity_add_complex.*


@Suppress("DEPRECATION")
class AddComplexActivity : AppCompatActivity() {

//    lateinit var binding: ActivityMainBinding

    lateinit var database: FirebaseDatabase
    lateinit var databaseref: DatabaseReference
    lateinit var storage: FirebaseStorage
    lateinit var auth: FirebaseAuth
    var uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_complex)

        databaseref = FirebaseDatabase.getInstance().getReference("Lenders")
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        database = FirebaseDatabase.getInstance()

        floatingActionButton.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .start()
        }

    }

    fun submitComplexDetails(view: android.view.View) {
        val name = complexName.getText().toString()
        val type = typeOfSport.getText().toString()
        val courts = totalCourts.getText().toString().toInt()
        val price = pricePerHour.getText().toString().toInt()
        val location = location.getText().toString()
        var uriString: String = ""

        val reference: StorageReference = storage.getReference().child("complex photo").child(
            FirebaseAuth.getInstance().getUid().toString())
        if (uri != null) {
            reference.putFile(uri!!).addOnSuccessListener(OnSuccessListener {
                Toast.makeText(this,"Image Uploaded Successfully!", Toast.LENGTH_LONG).show()

                reference.getDownloadUrl().addOnSuccessListener(OnSuccessListener<Uri>() {
                    database.getReference().child("Lenders").child(FirebaseAuth.getInstance().getUid().toString()).child("Complexes").child(name).child("imageUri").setValue(it.toString())
                    uriString = it.toString()
                })
            })
        }
        if(name!="" && type!="" && courts!=null && price!=null && location!="") {
            val User =
                ComplexInfo(name, type, price.toString(), courts.toString(), location, uriString)
            databaseref.child(FirebaseAuth.getInstance().getUid().toString()).child("Complexes")
                .child(name).setValue(User).addOnSuccessListener {
                Toast.makeText(this, "Successfully inserted data!!", Toast.LENGTH_LONG).show()
            }.addOnFailureListener {
                Toast.makeText(this, "Unsuccessful!!", Toast.LENGTH_LONG).show()
            }
        }
        else Toast.makeText(this,"Please enter all the required details!", Toast.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        uri = data?.getData()
        uploadedImage.setImageURI(uri)
        uploadedImage.visibility = VISIBLE
    }


}