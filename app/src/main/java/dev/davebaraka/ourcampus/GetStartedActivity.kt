package dev.davebaraka.ourcampus

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_get_started.*
import org.w3c.dom.Text
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class GetStartedActivity : AppCompatActivity() {

    private val GALLERY = 1
    private val CAMERA = 2
    var image = byteArrayOf()
    val utilities = Utilities.instance
    var agreement = false

    lateinit var classYear: String
    lateinit var incognito: String
    lateinit var editTextDisplayName: EditText
    lateinit var textViewClassYear: TextView
    lateinit var textViewPhoto: TextView
    lateinit var textViewIncognito: TextView
    lateinit var fltActButtonSubmit: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_started)

        editTextDisplayName = findViewById(R.id.editTextDisplayName)
        textViewClassYear = findViewById(R.id.textViewClassYear)
        textViewPhoto = findViewById(R.id.textViewPhoto)
        textViewIncognito = findViewById(R.id.textViewIncognito)
        fltActButtonSubmit = findViewById(R.id.fltActBtnSubmit)

        val td = termsDialog()
        td.show()

        textViewPhoto.setOnClickListener {
            val id = imageDialog()
            id.show()
        }

        textViewClassYear.setOnClickListener {
            val yd = yearDialog()
            yd.show()
        }

        textViewIncognito.setOnClickListener {
            val id = incognitoDialog()
            id.show()
        }

        fltActButtonSubmit.setOnClickListener {
            fltActButtonSubmit.hide()
            if (editTextDisplayName.text.length < 3 || editTextDisplayName.text.length > 15) {
                editTextDisplayName.error = "Display name must be between 3 and 15 characters."
                utilities.toast("Form Incomplete", "ERROR", this, null)
                fltActButtonSubmit.show()
            } else if (textViewClassYear.text.isEmpty()) {
                textViewClassYear.error = "Please select a class year"
                utilities.toast("Form Incomplete", "ERROR", this, null)
                fltActButtonSubmit.show()
            } else if (image.isEmpty()) {
                textViewPhoto.error = "Please add a profile picture"
                utilities.toast("Form Incomplete", "ERROR", this, null)
                fltActButtonSubmit.show()
            } else if (!agreement) {
                utilities.toast(
                    "Must Agree to Terms and Privacy Policy",
                    "ERROR",
                    this,
                    null)
                fltActButtonSubmit.show()
            } else {
                val user = FirebaseAuth.getInstance().currentUser
                val ref = FirebaseDatabase.getInstance().getReference("Users/" + user!!.uid)
                val storage = FirebaseStorage.getInstance().reference
                val storageRef =
                    storage.child("users/" + user.uid + ".png")
                val data = mapOf<String, Any?>(
                    "display" to editTextDisplayName.text.toString().trim(),
                    "incognito" to if (textViewIncognito.text == "Off") false else true,
                    "model" to Build.MODEL,
                    "version" to Build.VERSION.SDK_INT,
                    "time" to SimpleDateFormat("M/dd/yyyy h:mm a", Locale.US).format(Date()),
                    "manufacturer" to Build.MANUFACTURER,
                    "user" to user.email,
                    "year" to textViewClassYear.text
                )
                storageRef.putBytes(image).addOnSuccessListener {
                    ref.updateChildren(data).addOnSuccessListener {
                        finishAndRemoveTask()
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        startActivity(intent)
                    }.addOnCanceledListener {
                        fltActButtonSubmit.show()
                        utilities.toast("Unsuccessful", "ERROR", this, null)
                    }
                }.addOnCanceledListener {
                    fltActButtonSubmit.show()
                    utilities.toast("Unsuccessful", "ERROR", this, null)
                }
            }
        }

    }

    override fun onBackPressed() {
        return
    }

    private fun termsDialog():AlertDialog.Builder {
        val privacy = privacyDialog()
        val webView = WebView(this)
        webView.loadUrl("http://ourcampus.us.com/Terms.html")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Terms of Service")
        builder.setView(webView)
        builder.setCancelable(false)
        builder.setPositiveButton("Agree") { dialogInterface, i ->
            privacy.show()
        }
        builder.create()
        return builder
    }

    private fun privacyDialog():AlertDialog.Builder {
        val webView = WebView(this)
        webView.loadUrl("http://ourcampus.us.com/PrivacyPolicy.html")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Privacy Policy")
        builder.setView(webView)
        builder.setCancelable(false)
        builder.setPositiveButton("Agree") { dialogInterface, i -> agreement = true}
        builder.create()
        return builder
    }

    private fun yearDialog():AlertDialog.Builder {
        val array = arrayOf("2020", "2021", "2022", "2023")
        classYear = textViewClassYear.text.toString()
        var checkedItem = 0
        if (classYear != "") {
            checkedItem =  array.indexOf(classYear)
        } else {
            classYear = "2020"
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Class Year")
        builder.setPositiveButton("Select") { dialogInterface, i -> textViewClassYear.text = classYear; textViewClassYear.setError(null) }
        builder.setSingleChoiceItems(array, checkedItem) {_, which -> classYear = array[which] }
        builder.create()
        return builder
    }

    private fun incognitoDialog():AlertDialog.Builder {
        val array = arrayOf("On", "Off")
        incognito = textViewIncognito.text.toString()
        var checkedItem = 0
        if (incognito != "") {
            checkedItem =  array.indexOf(incognito)
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Incognito")
        builder.setPositiveButton("Select") { dialogInterface, i -> textViewIncognito.text = incognito; textViewIncognito.setError(null) }
        builder.setSingleChoiceItems(array, checkedItem) {_, which -> incognito = array[which] }
        builder.create()
        return builder
    }

    private fun imageDialog():AlertDialog.Builder {
        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems = arrayOf("Open Gallery", "Capture Photo")
        pictureDialog.setItems(pictureDialogItems
        ) { _, which ->
            when (which) {
                0 -> choosePhotoFromGallery()
                1 -> takePhotoFromCamera()
            }
        }
        return pictureDialog
    }

    private fun choosePhotoFromGallery() {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, GALLERY)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            0  -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay!
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(intent, CAMERA)
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    utilities.toast(
                        "Permission Denied",
                        "ERROR",
                        this,
                        null)
                }
                return
            }
        }
    }

    private fun takePhotoFromCamera() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, CAMERA)
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 0)
            } else {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 0)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY) {
            if (data != null) {
                val contentURI = data.data
                try {
                    val imageStream = this.contentResolver.openInputStream(contentURI!!)
                    val bitmap = BitmapFactory.decodeStream(imageStream)
                    val baos = ByteArrayOutputStream()
                    Bitmap.createScaledBitmap(bitmap, 640, 640, true)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 30, baos)

                    val temp = baos.toByteArray()

                    if (temp.size > 10000000) {
                        utilities.toast(
                            "Image Exceeds File Limit",
                            "ERROR",
                            this,
                            null)
                    } else {
                        image = temp
                        textViewPhoto.setError(null)
                        textViewPhoto.text = "Image Added"
                        utilities.toast("Image Added", "SUCCESS", this, null)
                        textViewPhoto.setError(null)
                    }



                } catch (e: IOException) {
                    e.printStackTrace()
                    utilities.toast(
                        "Error retrieving photo",
                        "ERROR",
                        this,
                        null)
                }

            }

        } else if (requestCode == CAMERA) {
            if (resultCode == Activity.RESULT_OK && data !=null) {
                val bitmap = data.extras!!.get("data") as Bitmap
                val baos = ByteArrayOutputStream()
                Bitmap.createScaledBitmap(bitmap, 640, 640, true)
                bitmap.compress(Bitmap.CompressFormat.PNG, 30, baos)
                val temp = baos.toByteArray()
                if (temp.size > 10000000) {
                    utilities.toast(
                        "Image Exceeds File Limit",
                        "ERROR",
                        this,
                        null)
                } else {
                    image = temp
                    textViewPhoto.text = "Image Added"
                    utilities.toast("Image Added", "SUCCESS", this, null)
                    textViewPhoto.setError(null)
                }
            }
        }
    }
}
