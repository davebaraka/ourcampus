package dev.davebaraka.ourcampus

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.renderscript.Sampler
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.firebase.ui.auth.AuthUI
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.IOException

private val user = FirebaseAuth.getInstance().currentUser!!.uid
private val database = FirebaseDatabase.getInstance()
private val storage = FirebaseStorage.getInstance().reference
private val utilities = Utilities.instance
private val GALLERY = 1
private val CAMERA = 2
private lateinit var image: ByteArray

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()

        val btnBack = findViewById<ImageButton>(R.id.imgBtnBack)
        btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val displayNamePreference = findPreference<EditTextPreference>("displayName")!!
            displayNamePreference.setOnPreferenceChangeListener { _, newValue ->
                if (newValue.toString().length >= 3 && newValue.toString().length <=15) {
                val ref = database.getReference("Users/"+user+"/display")
                ref.setValue(newValue).addOnSuccessListener {
                    utilities.toast("Display name changed. You may need to restart the application to see changes.", "SUCCESS", context!!, null)
                }
                } else {
                    utilities.toast("Display name must be between 3 and 15 characters.", "ERROR", context!!, null)
                }
                true
            }

            val incognitoPreference = findPreference<SwitchPreferenceCompat>("incognito")
            val p0 = (activity as SettingsActivity).intent.getBooleanExtra("incognito", false)
            if (p0) {
                incognitoPreference!!.isChecked = true
            } else {
                incognitoPreference!!.isChecked = false
            }
            incognitoPreference.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    incognitoPreference.isChecked = true
                } else {
                    incognitoPreference.isChecked = false
                }
                database.getReference("Users/"+user+"/incognito").setValue(newValue as Boolean)
                true
            }


            val classYearPreference = findPreference<Preference>("classYear")
            classYearPreference!!.summary = (activity as SettingsActivity).intent.getStringExtra("year")
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {

            if (preference!!.key == "logout") {
                AuthUI.getInstance()
                    .signOut(context!!)
                    .addOnCompleteListener {
                        activity!!.finishAffinity()
                        val intent = Intent(context, LoginActivity::class.java)
                        startActivity(intent)
                    }

            } else if (preference.key == "displayName") {
                val displayNamePreference = findPreference<EditTextPreference>("displayName")
                displayNamePreference!!.text =(activity as SettingsActivity).intent.getStringExtra("display")

            } else if (preference.key == "picture") {
                showPictureDialog()

            } else if (preference.key == "terms") {
                val webView = WebView(context)
                webView.loadUrl("http://ourcampus.us.com/Terms.html")
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Terms of Service")
                builder.setView(webView)
                builder.setCancelable(false)
                builder.setPositiveButton("Cose") {_, _ -> }
                builder.create()
                builder.show()

            } else if (preference.key == "privacyPolicy") {
                val webView = WebView(context)
                webView.loadUrl("http://ourcampus.us.com/PrivacyPolicy.html")
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Privacy Policy")
                builder.setView(webView)
                builder.setCancelable(false)
                builder.setPositiveButton("Cose") {_, _ -> }
                builder.create()
                builder.show()

            } else if (preference.key == "delete") {
                utilities.toast("Contact OurCampus Support", "INFO", context!!, null)
        }
            return super.onPreferenceTreeClick(preference)
        }

        private fun showPictureDialog() {
            val pictureDialog = AlertDialog.Builder(context)
            pictureDialog.setTitle("Select Action")
            val pictureDialogItems = arrayOf("Open Gallery", "Capture Photo")
            pictureDialog.setItems(pictureDialogItems
            ) { _, which ->
                when (which) {
                    0 -> choosePhotoFromGallery()
                    1 -> takePhotoFromCamera()
                }
            }
            pictureDialog.show()
        }

        private fun choosePhotoFromGallery() {
            val galleryIntent = Intent(Intent.ACTION_PICK,
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
                            context!!,
                            null)
                    }
                    return
                }
            }
        }

        private fun takePhotoFromCamera() {
            if (ContextCompat.checkSelfPermission(context!!,
                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent, CAMERA)
            } else if (ContextCompat.checkSelfPermission(context!!,
                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity!!,
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
                        val imageStream = context!!.contentResolver.openInputStream(contentURI!!)
                        val bitmap = BitmapFactory.decodeStream(imageStream)
                        val baos = ByteArrayOutputStream()
                        Bitmap.createScaledBitmap(bitmap, 640, 640, true)
                        bitmap.compress(Bitmap.CompressFormat.PNG, 30, baos)

                        val temp = baos.toByteArray()

                        if (temp.size > 10000000) {
                            utilities.toast(
                                "Image Exceeds File Size Limit",
                                "ERROR",
                                context!!,
                                null)
                        } else {
                            image = temp
                            storage.child("users/"+user+".png").putBytes(image).addOnSuccessListener {
                                utilities.toast(
                                    "Profile picture changed. You may need to restart the application to see changes.",
                                    "SUCCESS",
                                    context!!,
                                    null)                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        utilities.toast(
                            "Failed",
                            "Error",
                            context!!,
                            null)                    }

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
                            "Image Exceeds File Size Limit",
                            "ERROR",
                            context!!,
                            null)
                    } else {
                        image = temp
                        storage.child("users/"+user+".png").putBytes(image).addOnSuccessListener {
                            utilities.toast(
                                "Profile picture changed. You may need to restart the application to see changes.",
                                "SUCCESS",
                                context!!,
                                null)
                        }
                    }
                }
            }
        }
    }
}