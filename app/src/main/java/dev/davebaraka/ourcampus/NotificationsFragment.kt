package dev.davebaraka.ourcampus

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_notifications_vh.view.*
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.*

class NotificationsFragment : Fragment() {

    private val utilities = Utilities.instance
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var imgBtnBack: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var textViewNull: TextView
    private lateinit var adapterLst: MutableList<OCEvent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        textViewNull = view.findViewById(R.id.textViewNull)
        imgBtnBack = view.findViewById(R.id.imgBtnBack)
        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.material_gray))
        swipeRefresh.isEnabled = false

        imgBtnBack.setOnClickListener {
            (activity as MainActivity).onBackPressed()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.recyclerViewNotifications) as RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.isMotionEventSplittingEnabled = false
        adapterLst = setAdapterLst()
        recyclerView.adapter = NotificationsAdapter(activity as MainActivity, adapterLst)

        swipeRefresh.setOnRefreshListener {
            swipeRefresh.isEnabled = false
            swipeRefresh.isRefreshing = true
            recyclerView.visibility = View.GONE
            adapterLst = setAdapterLst()
            recyclerView.adapter = NotificationsAdapter(activity as MainActivity, adapterLst)
            recyclerView.visibility = View.VISIBLE
            swipeRefresh.isRefreshing = false
            swipeRefresh.isEnabled = true
        }
        swipeRefresh.isEnabled = true
    }

    private fun setAdapterLst():MutableList<OCEvent> {
        adapterLst = mutableListOf()
        val user = (activity as MainActivity).userUID
        val usersSnap = (activity as MainActivity).usersSnapshot
        val notificationsSnap = usersSnap.child(user + "/Notifications")
        val eventsSnap = (activity as MainActivity).eventsSnapshot
        for (notificationSnap in notificationsSnap.children) {
            val eventid = notificationSnap.child("eventid").value as String
            if (eventsSnap.hasChild(eventid)) {
                val event = eventsSnap.child(eventid).getValue(OCEvent::class.java)!!
                if (utilities.validateEvent(event)) {
                    event.display = usersSnap.child(event.host + "/display").value as String
                    event.timestampActivity = notificationSnap.child("timestamp").value as Long //Make a better name
                    adapterLst.add(event)
                }
            }
        }
        if (adapterLst.isEmpty()) {
            textViewNull.visibility = View.VISIBLE
        } else {
            textViewNull.visibility = View.GONE
        }
        adapterLst.sortWith(compareByDescending { it.timestampActivity })
        return adapterLst
    }

    companion object {
        // @return A new instance of fragment RateFragment.
        @JvmStatic
        fun newInstance() = NotificationsFragment()
    }
}

class NotificationsAdapter(val activity: MainActivity, val lst:MutableList<OCEvent>): RecyclerView.Adapter<ViewHolder>() {

    private val database = FirebaseDatabase.getInstance()
    private val utilities = Utilities.instance
    private val storage = FirebaseStorage.getInstance()

    override fun getItemCount(): Int {
        return lst.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val viewHolder = layoutInflater.inflate(R.layout.fragment_notifications_vh, parent, false)
        return ViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = lst.get(position)
        val storageRef = storage.getReference("users/" + event.host + ".png")
        utilities.loadImage(activity, storageRef, holder.view.imageViewProPic, "CIRCLE")
        holder.view.textViewUser.text = event.display + " invited you to..."
        holder.view.textViewEventName.text = event.title
        if (!((activity).usersSnapshot.child((activity).userUID + "/Notifications/" + event.eventid + "/viewed").value as Boolean)) {
            holder.view.constraintLayout.setBackgroundColor(ContextCompat.getColor(activity, R.color.colorNew))
        }

        holder.itemView.setOnClickListener {
            holder.itemView.isClickable = false
            holder.view.constraintLayout.setBackgroundColor(ContextCompat.getColor(activity, R.color.colorPrimary))
            database.getReference("Users/" + (activity).userUID + "/Notifications/" + event.eventid + "/viewed").setValue(true)
            activity.inflate(EventFragment.newInstance(event, activity.NAV_PROFILE), activity.NAV_PROFILE )
            Handler().postDelayed({
                holder.itemView.isClickable = true
            }, 1000)
        }
    }
}
