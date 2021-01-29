package dev.davebaraka.ourcampus

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.*
import android.webkit.URLUtil
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_home_vh.*
import java.text.SimpleDateFormat
import java.util.*

import kotlin.collections.ArrayList

class EventFragment() : Fragment() {

    private var stack = 0
    private val storage = FirebaseStorage.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val utilities = Utilities.instance
    private lateinit var event: OCEvent
    private lateinit var imgBtnBack: ImageButton
    private lateinit var imgBtnInfo: ImageButton
    private lateinit var imgViewEvent: ImageView
    private lateinit var textViewTitle: TextView
    private lateinit var textViewLocation: TextView
    private lateinit var textViewDate: TextView
    private lateinit var textViewTime: TextView
    private lateinit var textViewJoined: TextView
    private lateinit var textViewCategory: TextView
    private lateinit var textViewPrivy: TextView
    private lateinit var textViewLink: TextView
    private lateinit var textViewDescription: TextView
    private lateinit var imgViewProPic: ImageView
    private lateinit var textViewDisplay: TextView
    private lateinit var cardViewJoin: CardView
    private lateinit var cardViewNope: CardView
    private lateinit var cardViewInterest: CardView
    private lateinit var textViewInterest: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_event, container, false)
        event = arguments?.get("OCEVENT") as OCEvent
        stack = arguments?.get("STACK") as Int

        imgBtnBack = view.findViewById(R.id.imgBtnBack)
        imgBtnInfo = view.findViewById(R.id.imgBtnInfo)
        imgViewEvent = view.findViewById(R.id.imgViewEvent)
        textViewTitle = view.findViewById(R.id.textViewTitle)
        textViewLocation = view.findViewById(R.id.textViewLocation)
        textViewDate = view.findViewById(R.id.textViewDate)
        textViewTime = view.findViewById(R.id.textViewTime)
        textViewJoined = view.findViewById(R.id.textViewJoined)
        textViewCategory = view.findViewById(R.id.textViewCategory)
        textViewPrivy = view.findViewById(R.id.textViewPrivy)
        textViewLink = view.findViewById(R.id.textViewLink)
        textViewDescription = view.findViewById(R.id.textViewDescription)
        imgViewProPic = view.findViewById(R.id.imgViewProPic)
        textViewDisplay = view.findViewById(R.id.textViewDisplay)
        cardViewJoin = view.findViewById(R.id.cardViewJoin)
        cardViewNope = view.findViewById(R.id.cardViewNope)
        cardViewInterest = view.findViewById(R.id.cardViewInterest)
        textViewInterest = view.findViewById(R.id.textViewInterest)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.material_gray))

        swipeRefresh.setOnRefreshListener {
            /*
            Note that I am only refreshing a subset, since I'm lazy to check if the other things are null
            or maybe there is a more efficient way, always have to figure out the picture caching situation...
             */
            val eventsSnap = (activity as MainActivity).eventsSnapshot
            if (eventsSnap.hasChild(event.eventid!! + "/timeposted")) {
                event.title = eventsSnap.child(event.eventid!! + "/title").value as String
                event.location = eventsSnap.child(event.eventid!! + "/location").value as String
                event.eventtime = eventsSnap.child(event.eventid!! + "/eventtime").value as Long
                event.description = eventsSnap.child(event.eventid!! + "/description").value as String
                event.category = eventsSnap.child(event.eventid!! + "/category").value as String
                event.public = eventsSnap.child(event.eventid!! + "/public").value as Boolean
                if (eventsSnap.child(event.eventid!! + "/link").exists()) {
                    event.link = eventsSnap.child(event.eventid!! + "/link").value as String
                } else {
                    event.link = null
                }
            } else {
                utilities.toast("Event No Longer Exists", "INFO", context!!, null)
                (activity as MainActivity).onBackPressed()
            }
            inflateView()
            swipeRefresh.isRefreshing = false
        }
        imgBtnBack.setOnClickListener { (activity as MainActivity).onBackPressed() }

        inflateView()

        return view
    }

    private fun inflateView(){
        utilities.loadImage(context!!, storage.getReference("events/" + event.eventid + ".png"), imgViewEvent, "DEFAULT")
        utilities.loadImage(context!!, storage.getReference("users/" + event.host + ".png"), imgViewProPic, "CIRCLE")

        val user = (activity as MainActivity).userUID
        val eventSnap = (activity as MainActivity).eventsSnapshot.child(event.eventid!!)
        textViewTitle.text = event.title
        textViewLocation.text = event.location
        textViewDate.text = SimpleDateFormat("E, MMM d", Locale.US).format(event.eventtime as Long)
        textViewTime.text = SimpleDateFormat("h:mm a", Locale.US).format(event.eventtime as Long)
        textViewDescription.text = event.description
        textViewJoined.text = event.going!!.size.toString() + " Joined"
        textViewCategory.text = event.category
        textViewDisplay.text = "Created By: " + event.display

        if (event.public!!) {
            textViewPrivy.text = "Public"
            textViewPrivy.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_eye_outline, 0, 0, 0)
        } else {
            textViewPrivy.text = "Private"
            textViewPrivy.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_eye_off_outline, 0, 0, 0)
        }

        if (URLUtil.isValidUrl(event.link)) {
            textViewLink.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(event.link)))
            }
        } else {
            textViewLink.setOnClickListener {
                utilities.toast("No link", "INFO", context!!, null)
            }
        }

        if(eventSnap.hasChild("going/" + user)) {
            cardViewJoin.visibility = View.GONE
            cardViewNope.visibility = View.GONE
            cardViewInterest.visibility = View.VISIBLE
            textViewInterest.text = "Joined"
            textViewInterest.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_plus_circle, 0, 0, 0)
            textViewInterest.setTextColor(ContextCompat.getColor(context!!, R.color.colorSuccess))

        } else if (eventSnap.hasChild("viewed/" + user)) {
            cardViewJoin.visibility = View.GONE
            cardViewNope.visibility = View.GONE
            cardViewInterest.visibility = View.VISIBLE
            textViewInterest.text = "Not Interested"
            textViewInterest.setTextColor(ContextCompat.getColor(context!!, R.color.colorDanger))
            textViewInterest.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_minus_circle, 0, 0, 0)

        } else {
            cardViewJoin.visibility = View.VISIBLE
            cardViewNope.visibility = View.VISIBLE
            cardViewInterest.visibility = View.GONE

        }

        cardViewJoin.setOnClickListener {
            cardViewJoin.visibility = View.GONE
            cardViewNope.visibility = View.GONE
            cardViewInterest.visibility = View.VISIBLE
            textViewInterest.text = "Joined"
            textViewInterest.setTextColor(ContextCompat.getColor(context!!, R.color.colorSuccess))
            textViewInterest.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_plus_circle, 0, 0, 0)
            if ((activity as MainActivity).eventsSnapshot.hasChild(event.eventid!! + "/timeposted")) {
                database.getReference("Events/" + event.eventid as String + "/going/" + user).setValue("nil")
                database.getReference("Events/" + event.eventid as String + "/viewed/" + user).setValue("nil")
                val refFeed = database.getReference("Feed/" + event.eventid + "/" + user)
                refFeed.setValue(mapOf("eventid" to event.eventid, "timestamp" to ServerValue.TIMESTAMP, "userid" to user))
            } else {
                utilities.toast("Event No Longer Exists", "INFO", context!!, null)
                (activity as MainActivity).onBackPressed()
            }
        }
        cardViewNope.setOnClickListener {
            cardViewJoin.visibility = View.GONE
            cardViewNope.visibility = View.GONE
            cardViewInterest.visibility = View.VISIBLE
            textViewInterest.text = "Not Interested"
            textViewInterest.setTextColor(ContextCompat.getColor(context!!, R.color.colorDanger))
            textViewInterest.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_minus_circle, 0, 0, 0)
            if ((activity as MainActivity).eventsSnapshot.hasChild(event.eventid!! + "/timeposted")) {
                database.getReference("Events/" + event.eventid as String + "/going/" + user).removeValue()
                database.getReference("Events/" + event.eventid as String + "/viewed/" + user).setValue("nil")
                val refFeed = database.getReference("Feed/" + event.eventid as String + "/" + user)
                refFeed.removeValue()
            } else {
                utilities.toast("Event No Longer Exists", "INFO", context!!, null)
                (activity as MainActivity).onBackPressed()
            }
        }
        cardViewInterest.setOnClickListener {
            if (textViewInterest.text == "Joined") {
                textViewInterest.text = "Not Interested"
                textViewInterest.setTextColor(ContextCompat.getColor(context!!, R.color.colorDanger))
                textViewInterest.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_minus_circle, 0, 0, 0)
                if ((activity as MainActivity).eventsSnapshot.hasChild(event.eventid!! + "/timeposted")) {
                    database.getReference("Events/" + event.eventid as String + "/going/" + user).removeValue()
                    database.getReference("Events/" + event.eventid as String + "/viewed/" + user).setValue("nil")
                    val refFeed = database.getReference("Feed/" + event.eventid as String + "/" + user)
                    refFeed.removeValue()
                } else {
                    utilities.toast("Event No Longer Exists", "INFO", context!!, null)
                    (activity as MainActivity).onBackPressed()
                }
            } else {
                textViewInterest.text = "Joined"
                textViewInterest.setTextColor(ContextCompat.getColor(context!!, R.color.colorSuccess))
                textViewInterest.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_plus_circle, 0, 0, 0)
                if ((activity as MainActivity).eventsSnapshot.hasChild(event.eventid!! + "/timeposted")) {
                    database.getReference("Events/" + event.eventid as String + "/going/" + user).setValue("nil")
                    database.getReference("Events/" + event.eventid as String + "/viewed/" + user).setValue("nil")
                    val refFeed = database.getReference("Feed/" + event.eventid + "/" + user)
                    refFeed.setValue(mapOf("eventid" to event.eventid, "timestamp" to ServerValue.TIMESTAMP, "userid" to user))
                } else {
                    utilities.toast("Event No Longer Exists", "INFO", context!!, null)
                    (activity as MainActivity).onBackPressed()
                }
            }

        }

        if (event.host != (activity as MainActivity).userUID) {
            val dialog = buildReportDialog()
            imgBtnInfo.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_info_outline))
            imgBtnInfo.setOnClickListener { dialog.show() }
        } else {
            imgBtnInfo.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_edit_outline))
            imgBtnInfo.setOnClickListener{
                (activity as MainActivity).inflate(EventFormFragment.newInstance(false, event.eventid!!), stack)
            }
        }

        // May need to add this to coroutine or all of inflate to couroutine...
        val joinedDialog = buildJoinedDialog()
        textViewJoined.setOnClickListener { joinedDialog.show() }
    }

    private fun buildJoinedDialog():AlertDialog.Builder{
        val lst = mutableListOf<String>()
        val usersSnap = (activity as MainActivity).usersSnapshot
        for (user in event.going!!.keys) {
            if (usersSnap.hasChild(user + "/incognito") && !(usersSnap.child(user + "/incognito").value as Boolean)) {
                val display = usersSnap.child(user + "/display").value as String
                lst.add(display)
            }
        }
        val array = lst.toTypedArray().sortedArray()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Joined")
        builder.setPositiveButton("Close") {_, _ ->}
        builder.setItems(array) { _, _ -> }
        builder.create()
        return builder
    }

    private fun buildReportDialog():AlertDialog.Builder{
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Report Event")
        builder.setMessage("Would you like to report this event?")
        builder.setPositiveButton("Yes") { _, _ ->
            database.getReference("ReportedEvents").updateChildren(
                mapOf(event.eventid as String to mapOf("user" to (activity as MainActivity).userUID)))
            utilities.toast("Event Reported", "SUCCESS", context!!, null)
        }
        builder.setNegativeButton("No", null)
        builder.create()
        return builder
    }

    companion object {
        // @return A new instance of fragment EventsInfoFragment.
        @JvmStatic
        fun newInstance(event: Parcelable, stack: Int) = EventFragment().apply {
            arguments = Bundle().apply {
                putParcelable("OCEVENT", event)
                putInt("STACK", stack)
            }
        }
    }
}
