package dev.davebaraka.ourcampus

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import androidx.fragment.app.Fragment
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_event.*
import kotlinx.android.synthetic.main.toast.*

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class EventFormFragment : Fragment() {

    private var eventid = ""
    private var create = true
    private val cal = Calendar.getInstance()
    private var category = "Other"
    private var visibility = "Public"
    private val GALLERY = 1
    private val CAMERA = 2
    private var image = byteArrayOf()
    private val utilities = Utilities.instance
    private val database = FirebaseDatabase.getInstance()

    private lateinit var event: OCEvent
    private lateinit var imgBtnBack: ImageButton
    private lateinit var textViewFragmentName: TextView
    private lateinit var textViewDelete: TextView
    private lateinit var editTextTitle: EditText
    private lateinit var textViewDate: TextView
    private lateinit var textViewTime: TextView
    private lateinit var editTextLocation: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var textViewCategory: TextView
    private lateinit var textViewPhoto: TextView
    private lateinit var textViewInvite: TextView
    private lateinit var editTextLink: EditText
    private lateinit var textViewVisiblity: TextView
    private lateinit var fltActBtnSubmit: FloatingActionButton
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_event_form, container, false)
        create = arguments?.get("CREATE") as Boolean
        eventid = arguments?.get("EVENTID") as String

        imgBtnBack = view.findViewById(R.id.imgBtnBack)
        textViewFragmentName = view.findViewById(R.id.textViewFragmentName)
        textViewDelete = view.findViewById(R.id.textViewDelete)
        editTextTitle = view.findViewById(R.id.editTextTitle)
        textViewDate = view.findViewById(R.id.textViewDate)
        textViewTime = view.findViewById(R.id.textViewTime)
        editTextLocation = view.findViewById(R.id.editTextLocation)
        editTextDescription = view.findViewById(R.id.editTextDescription)
        textViewCategory = view.findViewById(R.id.textViewCategory)
        textViewPhoto = view.findViewById(R.id.textViewPhoto)
        textViewInvite = view.findViewById(R.id.textViewInvite)
        editTextLink = view.findViewById(R.id.editTextLink)
        textViewVisiblity = view.findViewById(R.id.textViewVisibility)
        fltActBtnSubmit = view.findViewById(R.id.fltActBtnSubmit)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        swipeRefresh.isEnabled = false

        imgBtnBack.setOnClickListener { (activity as MainActivity).onBackPressed() }

        if (create) {
            textViewDelete.visibility = View.GONE
            textViewFragmentName.text = "Create Event"
            fltActBtnSubmit.setOnClickListener {
                fltActBtnSubmit.hide()
                createEvent()
                fltActBtnSubmit.show()
            }

        } else {
            textViewDelete.visibility = View.VISIBLE
            textViewDelete.setOnClickListener {
                val eventRef = database.getReference("Events/" + eventid)
                eventRef.removeValue().addOnCompleteListener {
                    (activity as MainActivity).onBackPressed()
                    database.getReference("DeletedEvents/" + event.eventid).setValue(event)
                    utilities.toast("Event Deleted", "SUCCESS", context!!, null)
                }
            }
            textViewFragmentName.text = "Edit Event"
            if ((activity as MainActivity).eventsSnapshot.hasChild(eventid + "/timeposted")) {
                event = (activity as MainActivity).eventsSnapshot.child(eventid).getValue(OCEvent::class.java)!!
                cal.timeInMillis = event.eventtime as Long
                editTextTitle.setText(event.title)
                editTextLocation.setText(event.location)
                editTextDescription.setText(event.description)
                textViewCategory.text = event.category
                editTextLink.setText(event.link)
                textViewDate.text = SimpleDateFormat("E, MMM d", Locale.US).format(cal.time.time)
                textViewTime.text = SimpleDateFormat("h:mm a", Locale.US).format(cal.time.time)
                if (event.public!!) {
                    textViewVisiblity.text = "Public"
                } else {
                    textViewVisiblity.text = "Private"
                }
            } else {
                utilities.toast("An Unknown Error Occured", "ERROR", context!!, null)
                (activity as MainActivity).onBackPressed()
            }
            fltActBtnSubmit.setOnClickListener {
                fltActBtnSubmit.hide()
                editEvent()
                fltActBtnSubmit.show()
            }
        }

        val dpd = datePickerDialog()
        val tpd = timePickerDialog()
        textViewInvite.setOnClickListener {
            utilities.toast(
                "Coming Soon",
                "INFO",
                context!!,
                null
            )
        }
        textViewDate.setOnClickListener { dpd.show() }
        textViewTime.setOnClickListener { tpd.show() }
        textViewCategory.setOnClickListener { categoryDialog() }
        textViewPhoto.setOnClickListener { showPictureDialog() }
        textViewVisiblity.setOnClickListener { visibilityDialog() }

        return view
    }

    private fun visibilityDialog() {
        val array = arrayOf("Private", "Public")
        val temp = array.indexOf(visibility)
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Adjust Visibility")
        builder.setPositiveButton("Select") { _, _ ->
            textViewVisiblity.text = visibility; textViewVisiblity.setError(null)
        }
        builder.setNegativeButton("Cancel") { _, _ -> }
        builder.setSingleChoiceItems(array, temp) { _, which -> visibility = array[which] }
        builder.create()
        builder.show()
    }

    private fun categoryDialog() {
        val array =
            arrayOf("Athletics", "Activism", "Education", "Shows", "Social", "Rides", "Other")
        val temp = array.indexOf(category)
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Choose a Category")
        builder.setPositiveButton("Select") { _, _ ->
            textViewCategory.text = category; textViewCategory.setError(null)
        }
        builder.setNegativeButton("Cancel") { _, _ -> }
        builder.setSingleChoiceItems(array, temp) { _, which -> category = array[which] }
        builder.create()
        builder.show()
    }

    private fun datePickerDialog(): DatePickerDialog {
        return DatePickerDialog(
            context!!,
            DatePickerDialog.OnDateSetListener { _, mYear, mMonth, mDay ->
                cal.set(Calendar.YEAR, mYear)
                cal.set(Calendar.MONTH, mMonth)
                cal.set(Calendar.DAY_OF_MONTH, mDay)
                textViewDate.text = SimpleDateFormat("E, MMM d", Locale.US).format(cal.time)
                textViewDate.setError(null)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun timePickerDialog(): TimePickerDialog {
        return TimePickerDialog(context, TimePickerDialog.OnTimeSetListener { _, mHour, mMinute ->
            cal.set(Calendar.HOUR_OF_DAY, mHour)
            cal.set(Calendar.MINUTE, mMinute)
            textViewTime.text = SimpleDateFormat("h:mm a", Locale.US).format(cal.time)
            textViewTime.setError(null)
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false)
    }

    private fun validate(): Boolean {
        if (editTextTitle.text.length < 3 || editTextTitle.text.length > 50) {
            editTextTitle.error = "Event title must be between 3 and 50 characters"
            utilities.toast("Form Incomplete", "ERROR", context!!, null)
            return false
        } else if (textViewDate.text.length < 1) {
            textViewDate.error = "Please choose a date for the event"
            utilities.toast("Form Incomplete", "ERROR", context!!, null)
            return false
        } else if (textViewTime.text.length < 1) {
            textViewTime.error = "Please choose a time for the event"
            utilities.toast("Form Incomplete", "ERROR", context!!, null)
            return false
        } else if (editTextLocation.text.length < 3 || editTextLocation.text.length > 50) {
            editTextLocation.error = "Location must be between 3 and 50 characters"
            utilities.toast("Form Incomplete", "ERROR", context!!, null)
            return false
        } else if (editTextDescription.text.length < 3 || editTextDescription.text.length > 160) {
            editTextDescription.error = "Description must be between 3 and 160 characters"
            utilities.toast("Form Incomplete", "ERROR", context!!, null)
            return false
        } else if (textViewCategory.text.length < 1) {
            textViewCategory.error = "Please choose a category for the event"
            utilities.toast("Form Incomplete", "ERROR", context!!, null)
            return false
        } else if (editTextLink.text.length > 1 && !URLUtil.isValidUrl(editTextLink.text.toString())) {
            editTextLink.error = "Please enter a valid web link"
            utilities.toast("Form Incomplete", "ERROR", context!!, null)
            return false
        } else if (image.isEmpty()) {
            if (create) {
                textViewPhoto.error = "Please add a photo for the event"
                utilities.toast("Form Incomplete", "ERROR", context!!, null)
                return false
            }
        }
        return true
    }

    private fun createEvent() {

        if (validate()) {
            val refEvents = database.getReference("Events")
            val user = (activity as MainActivity).userUID

            val category = textViewCategory.text.toString()
            val description = editTextDescription.text.toString()
            val editedtime = null
            val eventid_ = refEvents.push().key.toString()
            val eventtime = cal.time.time
            val going = mapOf(user to "nil")
            val host = user
            val invited = mapOf("Db4sWy7ivBMJiJk5EXrNL1gsPx32" to "nil")
            val link = if (editTextLink.text.length > 0) editTextLink.text.toString() else null
            val location = editTextLocation.text.toString()
            val public = if (textViewVisiblity.text == "Public") true else false
            val timeposted = ServerValue.TIMESTAMP
            val title = editTextTitle.text.toString()
            val viewed = mapOf(host to "nil")

            val data = OCEvent(
                category, description, null, null,
                editedtime, eventid_, eventtime, going, host, invited, link, location,
                null, public, null, timeposted, null,
                title, viewed, null
            )

            val refFeed = database.getReference("Feed/" + eventid_ + "/" + host)
            val storageRef =
                FirebaseStorage.getInstance().getReference("events/" + eventid_ + ".png")
            storageRef.putBytes(image).addOnSuccessListener {
                refEvents.child(eventid_).setValue(data).addOnCompleteListener {
                    refFeed.setValue(
                        mapOf(
                            "eventid" to eventid_,
                            "timestamp" to timeposted,
                            "userid" to host
                        )
                    ).addOnCompleteListener {
                        (activity as MainActivity).onBackPressed()
                        utilities.toast("Event Created", "SUCCESS", context!!, null)
                    }
                }
            }
        }
    }


    private fun editEvent() {

        if (validate()) {
            val refEvents = database.getReference("Events")
            val user = (activity as MainActivity).userUID

            val category = textViewCategory.text as String
            val description = editTextDescription.text.toString()
            val editedtime = ServerValue.TIMESTAMP
            val eventid_ = eventid
            val eventtime = cal.time.time
            val going = event.going
            val host = user
            val invited = event.invited
            val link = if (editTextLink.text.length > 0) editTextLink.text.toString() else null
            val location = editTextLocation.text.toString()
            val public = if (textViewVisiblity.text == "Public") true else false
            val timeposted = event.timeposted
            val title = editTextTitle.text.toString()
            val viewed = event.viewed

            val data = OCEvent(
                category, description, null, null,
                editedtime, eventid_, eventtime, going, host, invited, link, location,
                null, public, null, timeposted, null,
                title, viewed, null
            )

            if (image.isNotEmpty()) {
                val storageRef =
                    FirebaseStorage.getInstance().getReference("events/" + eventid + ".png")
                storageRef.putBytes(image)
            }
            refEvents.child(eventid_).setValue(data).addOnCompleteListener {
                (activity as MainActivity).onBackPressed()
                utilities.toast("Event Updated", "SUCCESS", context!!, null)
            }
        }

    }

    /*
    GET PHOTO FROM CAMERA OR GALLERY
    */

    private fun showPictureDialog() {
        val pictureDialog = AlertDialog.Builder(context)
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems = arrayOf("Open Gallery", "Capture Photo")
        pictureDialog.setItems(
            pictureDialogItems
        ) { _, which ->
            when (which) {
                0 -> choosePhotoFromGallery()
                1 -> takePhotoFromCamera()
            }
        }
        pictureDialog.show()
    }

    private fun choosePhotoFromGallery() {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(galleryIntent, GALLERY)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            0 -> {
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
                        null
                    )
                }
                return
            }
        }
    }

    private fun takePhotoFromCamera() {
        if (ContextCompat.checkSelfPermission(
                context!!,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, CAMERA)
        } else if (ContextCompat.checkSelfPermission(
                context!!,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_DENIED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity!!,
                    Manifest.permission.CAMERA
                )
            ) {
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
                    bitmap.compress(Bitmap.CompressFormat.PNG, 75, baos)

                    val temp = baos.toByteArray()

                    if (temp.size > 10000000) {
                        utilities.toast("Image Exceeds File Limit", "ERROR", context!!, null)
                    } else {
                        image = temp
                        textViewPhoto.text = "Image Added"
                        utilities.toast("Image Added", "SUCCESS", context!!, null)
                        textViewPhoto.setError(null)
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                    utilities.toast(
                        "An unknown error occured while retrieving photo",
                        "ERROR",
                        context!!,
                        null
                    )
                }

            }

        } else if (requestCode == CAMERA) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val bitmap = data.extras!!.get("data") as Bitmap
                val baos = ByteArrayOutputStream()
                Bitmap.createScaledBitmap(bitmap, 640, 640, true)
                bitmap.compress(Bitmap.CompressFormat.PNG, 75, baos)

                val temp = baos.toByteArray()

                if (temp.size > 10000000) {
                    utilities.toast("Image Exceeds File Limit", "ERROR", context!!, null)
                } else {
                    image = temp
                    textViewPhoto.text = "Image Added"
                    utilities.toast("Image Added", "SUCCESS", context!!, null)
                    textViewPhoto.setError(null)
                }
            }
        }
    }

    companion object {
        // @return A new instance of fragment EventFormFragment.
        @JvmStatic
        fun newInstance(create: Boolean, eventid: String = "") = EventFormFragment().apply {
            arguments = Bundle().apply {
                putBoolean("CREATE", create)
                putString("EVENTID", eventid)
            }
        }
    }
}
