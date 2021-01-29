package dev.davebaraka.ourcampus

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.storage.FirebaseStorage

import kotlinx.android.synthetic.main.fragment_events_vh.view.*
import kotlinx.android.synthetic.main.fragment_events_vh.view.textViewDate
import kotlinx.android.synthetic.main.fragment_events_vh.view.textViewTime
import kotlinx.android.synthetic.main.fragment_events_vh.view.textViewTitle
import java.text.SimpleDateFormat
import java.util.*

class EventsFragment : Fragment() {

    private var root = true
    private val utilities = Utilities.instance
    private lateinit var adapterLst: MutableList<OCEvent>
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var imgBtnBack: ImageButton
    private lateinit var textViewFragmentName: TextView
    private lateinit var imgBtnCategory: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var textViewNull: TextView
    private lateinit var imgBtnCreate: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_events, container, false)

        root = arguments?.get("ROOT") as Boolean
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        imgBtnBack = view.findViewById(R.id.imgBtnBack)
        textViewFragmentName = view.findViewById(R.id.textViewFragmentName)
        imgBtnCategory = view.findViewById(R.id.imgBtnCategory)
        recyclerView = view.findViewById(R.id.recyclerView)
        textViewNull = view.findViewById(R.id.textViewNull)
        imgBtnCreate = view.findViewById(R.id.imgBtnCreate)

        if (!root) {
            textViewFragmentName.text = "My Schedule"
            imgBtnBack.visibility = View.VISIBLE
            imgBtnBack.setOnClickListener{ (activity as MainActivity).onBackPressed() }
            imgBtnCategory.visibility = View.GONE
            imgBtnCreate.visibility = View.GONE
        } else {
            buildDialog()
            imgBtnCategory.setOnClickListener {
                imgBtnCategory.isEnabled = false
                buildDialog().show()
            }
            imgBtnCreate.setOnClickListener {
                (activity as MainActivity).inflate(EventFormFragment.newInstance(true), (activity as MainActivity).NAV_EVENTS)
            }
        }

        recyclerView = view.findViewById(R.id.recyclerView) as RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.isMotionEventSplittingEnabled = false
        adapterLst = setAdapterLst()
        recyclerView.adapter = EventsAdapter(activity as MainActivity, adapterLst, root)

        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.material_gray))
        swipeRefresh.setOnRefreshListener {
            swipeRefresh.isEnabled = false
            swipeRefresh.isRefreshing = true
            recyclerView.visibility = View.GONE
            adapterLst = setAdapterLst()
            val fragmentName = textViewFragmentName.text as String
            reloadAdapter(if (fragmentName == "Events" || fragmentName == "My Schedule") "All" else fragmentName)
            recyclerView.visibility = View.VISIBLE
            swipeRefresh.isRefreshing = false
            swipeRefresh.isEnabled = true
        }

        return view
    }

    private fun setAdapterLst(): MutableList<OCEvent> {
        val lst = mutableListOf<OCEvent>()
        val usersSnap = (activity as MainActivity).usersSnapshot
        val eventsSnap = (activity as MainActivity).eventsSnapshot
        if (!root) {
            for (eventSnap in eventsSnap.children) {
                val event = eventSnap.getValue(OCEvent::class.java)!!
                if (utilities.validateEvent(event) && (event.going!!.containsKey((activity as MainActivity).userUID) || event.host == (activity as MainActivity).userUID)
                    && (event.eventtime as Long) > (activity as MainActivity).time)
                {
                    event.display = usersSnap.child(event.host + "/display").value as String
                    lst.add(event)
                }
            }
        } else {
            for (eventSnap in eventsSnap.children) {
                val event = eventSnap.getValue(OCEvent::class.java)!!
                if (utilities.validateEvent(event) && (event.public!! || event.invited!!.containsKey((activity as MainActivity).userUID) || event.host == (activity as MainActivity).userUID)
                    && (event.eventtime as Long) > (activity as MainActivity).time)
                {
                    event.display = usersSnap.child(event.host + "/display").value as String
                    lst.add(event)
                }
            }
        }
        inflateNoEvents(lst)
        lst.sortWith(compareBy { it.eventtime })
        return lst
    }

    private fun buildDialog():AlertDialog.Builder{
        val array = arrayOf("All", "Athletics", "Activism", "Education", "Shows", "Social", "Rides", "Other")
        var checkedItem = 0
        if (textViewFragmentName.text != "Events") {
            checkedItem =  array.indexOf(textViewFragmentName.text)
        }
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Select a Category")
        builder.setItems(array) { dialog, which ->
            reloadAdapter(array[which])
            imgBtnCategory.isEnabled = true
        }
        builder.setOnDismissListener {
            imgBtnCategory.isEnabled = true
        }
        builder.create()
        return builder
    }

    private fun reloadAdapter(category: String) {
        if (category != "All") {
            val lst = adapterLst.filter { it.category == category } as MutableList<OCEvent>
            textViewFragmentName.text = category
            inflateNoEvents(lst)
            recyclerView.adapter = EventsAdapter(activity as MainActivity, lst, root)
        } else if (!root) {
            recyclerView.adapter = EventsAdapter(activity as MainActivity, adapterLst, root)
            inflateNoEvents(adapterLst)
        } else {
            textViewFragmentName.text = "Events"
            recyclerView.adapter = EventsAdapter(activity as MainActivity, adapterLst, root)
            inflateNoEvents(adapterLst)
        }
    }

    private fun inflateNoEvents (lst: MutableList<OCEvent>) {
        if (lst.isEmpty()) {
            recyclerView.visibility = View.GONE
            textViewNull.visibility = View.VISIBLE
        } else {
            textViewNull.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    interface OnFragmentInteractionListener {
    }

    companion object {
        @JvmStatic
        fun newInstance(root: Boolean = true) = EventsFragment().apply {
            arguments = Bundle().apply {
                putBoolean("ROOT", root)
            }
        }
    }
}

class EventsAdapter(val activity: MainActivity, val lst:MutableList<OCEvent>, val root: Boolean): RecyclerView.Adapter<ViewHolder>() {

    private val utilities = Utilities.instance
    private val storage = FirebaseStorage.getInstance()

    override fun getItemCount(): Int {
        return lst.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val viewHolder = layoutInflater.inflate(R.layout.fragment_events_vh, parent, false)
        return ViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = lst.get(position)
        val storageRef = storage.getReference("users/" + event.host + ".png")
        utilities.loadImage(activity, storageRef, holder.view.imgViewProPic, "CIRCLE")
        if (!event.public!!) {
            holder.view.textViewPrivy.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_eye_off_outline, 0, 0, 0)
            holder.view.textViewPrivy.text = "Private"
        }
        holder.view.textViewDescription.text = event.description
        holder.view.textViewDate.text = SimpleDateFormat("E, MMM d", Locale.US).format(event.eventtime as Long)
        holder.view.textViewTime.text = SimpleDateFormat("h:mm a", Locale.US).format(event.eventtime as Long)
        holder.view.textViewTitle.text = event.title
        holder.view.textViewLocation.text = event.location
        holder.view.textViewJoin.text = event.going!!.size.toString() + " Joined"

        holder.itemView.setOnClickListener {
            holder.itemView.isClickable = false
            if (root) {
                activity.inflate(EventFragment.newInstance(event, activity.NAV_EVENTS), activity.NAV_EVENTS )
            } else {
                activity.inflate(EventFragment.newInstance(event, activity.NAV_PROFILE), activity.NAV_PROFILE )
            }
            Handler().postDelayed({
                //doSomethingHere()
                holder.itemView.isClickable = true
            }, 1000)

        }

    }
}

class ViewHolder(val view: View): RecyclerView.ViewHolder(view) {
}

