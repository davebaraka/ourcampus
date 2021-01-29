package dev.davebaraka.ourcampus

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcelable
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.Placeholder
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.StorageReference
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.GrayscaleTransformation
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class Utilities {

    fun getInterest(going: Map<String, String>): String {
        val size = going.size
        if (size == 1) {
            return "1 person is interested"
        } else {
            return (size.toString() + " people are interested")
        }
    }

    fun getDate(eventtime: Long): String {
        return SimpleDateFormat("d", Locale.US).format(Date(eventtime))
    }

    fun getTime(eventtime: Long): String {
        return SimpleDateFormat("E, h:mm a", Locale.US).format(Date(eventtime))
    }

    fun getMonth(eventtime: Long): String {
        return SimpleDateFormat("MMM", Locale.US).format(Date(eventtime))
    }

    fun loadImage(context: Context, storageRef: StorageReference, imgView: ImageView, effect: String) {
        when (effect) {
            "CIRCLE" -> {
                Glide.with(context)
                    .load(storageRef)
                    .apply(RequestOptions.circleCropTransform())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imgView)
            }
            "DEFAULT" -> {
                Glide.with(context)
                    .load(storageRef)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imgView)
            }
        }
    }

    fun toast(message: String, type: String, context: Context, viewGroup: ViewGroup?) {
        val toast : Toast = Toast(context)
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.toast, viewGroup, false)
        val textView = view.findViewById<TextView>(R.id.textView)
        when (type) {
            "INFO" -> {
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_info, 0, 0, 0)
            }
            "SUCCESS" -> {
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_success, 0, 0, 0)
            }
            "ERROR" -> {
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_error, 0, 0, 0)
            }
        }
        textView.text = message
        toast.view = view
        toast.duration = Toast.LENGTH_LONG
        toast.show()
    }

    fun validateEvent(event: OCEvent): Boolean {
        /*
        The optional cases, no guarantee, (things you will have to check manually if they are null)
        event.display, event.editedtime, event.link, event.opened, event.received
         */
        return event.category != null && event.description != null && event.eventid != null
                && event.eventtime != null && event.host != null && event.location != null
                && event.public != null && event.timeposted != null && event.title != null
    }

    companion object {
        val instance = Utilities()
    }
}

@Parcelize
data class OCEvent (
    var category: String? = null,
    var description: String? = null,
    var display: String? = null,
    var displayActivity: String? = null,
    var editedtime: @RawValue Any? = null,
    var eventid: String? = null,
    var eventtime: Long? = null,
    var going: Map<String, String>? = emptyMap(),
    var host: String? = null,
    var invited: Map<String, String>? = emptyMap(),
    var link: String? = null,
    var location: String? = null,
    var opened: Boolean? = null,
    var public: Boolean? = null,
    var received: Long?= null,
    var timeposted: @RawValue Any? = null,
    var timestampActivity: Long? = null,
    var title: String? = null,
    var viewed: Map<String, String>? = emptyMap(),
    var userActivity: String? = null
) : Parcelable

@Parcelize
data class OCReview (
    var accessible: String? = null,
    var course: String? = null,
    var comment: String? = null,
    var difficulty: Int? = null,
    var discussion: Int? = null,
    var feedback: String? = null,
    var grade: String? = null,
    var lecture: Int? = null,
    var rate: Int? = null,
    var recommend: String? = null,
    var returns: Int?= null,
    var timestamp: Long? = null
) : Parcelable

@GlideModule
class AppGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.append(StorageReference::class.java, InputStream::class.java, FirebaseImageLoader.Factory())
    }
}