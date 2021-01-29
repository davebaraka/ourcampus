package dev.davebaraka.ourcampus

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.storage.FirebaseStorage

import kotlinx.android.synthetic.main.fragment_home_vh_activity.view.*
import kotlinx.android.synthetic.main.fragment_home_vh_activity.view.textViewEventName
import kotlinx.android.synthetic.main.fragment_home_vh.view.*
import kotlinx.coroutines.*

class HomeFragment : Fragment() {

    private val utilities = Utilities.instance

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerViewHome: RecyclerView
    private lateinit var recyclerViewActivity: RecyclerView
    private lateinit var textViewQuick: TextView
    private lateinit var textViewActivity: TextView
    private lateinit var constraintLayout: ConstraintLayout
    private lateinit var cardViewCreate: CardView
    private lateinit var cardViewSubmit: CardView
    private lateinit var cardViewSubscribe: CardView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.material_gray))
        swipeRefresh.isEnabled = false
        textViewQuick = view.findViewById(R.id.textViewQuick)
        textViewActivity = view.findViewById(R.id.textViewActivity)
        constraintLayout = view.findViewById(R.id.constraintLayout)
        cardViewSubmit = view.findViewById(R.id.cardViewSubmit)
        cardViewCreate = view.findViewById(R.id.cardViewCreate)
        cardViewSubscribe = view.findViewById(R.id.cardViewSubscribe)
        textViewQuick.visibility = View.INVISIBLE
        textViewActivity.visibility = View.INVISIBLE
        constraintLayout.visibility = View.INVISIBLE

        GlobalScope.launch(Dispatchers.Main) {
            recyclerViewHome = view.findViewById(R.id.recyclerViewHome)
            PagerSnapHelper().attachToRecyclerView(recyclerViewHome)
            recyclerViewHome.isNestedScrollingEnabled = false
            recyclerViewHome.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            recyclerViewHome.adapter = HomeAdapter(activity as MainActivity, setHomeAdapterLst())

            textViewQuick.visibility = View.VISIBLE
            constraintLayout.visibility = View.VISIBLE
            textViewActivity.visibility = View.VISIBLE

            cardViewSubmit.setOnClickListener {
                utilities.toast("Coming soon", "INFO", context!!, null)
            }
            cardViewSubscribe.setOnClickListener {
                (activity as MainActivity).inflate(SubscriptionsFragment.newInstance(), (activity as MainActivity).NAV_HOME)
            }
            cardViewCreate.setOnClickListener {
                (activity as MainActivity).inflate(EventFormFragment.newInstance(true), (activity as MainActivity).NAV_HOME)
            }

            recyclerViewActivity = view.findViewById(R.id.recyclerViewActivity) as RecyclerView
            recyclerViewActivity.isNestedScrollingEnabled = false
            recyclerViewActivity.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            recyclerViewActivity.adapter = ActivityAdapter(activity as MainActivity, setActivityAdapterLst())

            swipeRefresh.setOnRefreshListener {
                swipeRefresh.isEnabled = false
                swipeRefresh.isRefreshing = true
                GlobalScope.launch(Dispatchers.Main) {
                    recyclerViewHome.adapter = HomeAdapter(activity as MainActivity, setHomeAdapterLst())
                    recyclerViewActivity.adapter = ActivityAdapter(activity as MainActivity, setActivityAdapterLst())
                    swipeRefresh.isRefreshing = false
                    swipeRefresh.isEnabled = true
                }
            }
            swipeRefresh.isEnabled = true
        }

        return view
    }

    private fun setHomeAdapterLst(): MutableList<OCEvent> {
        val usersSnap = (activity as MainActivity).usersSnapshot
        val eventsSnap = (activity as MainActivity).eventsSnapshot
        val lst = mutableListOf<OCEvent>()
        for (eventSnap in eventsSnap.children) {
            val event = eventSnap.getValue(OCEvent::class.java)!!
            if (utilities.validateEvent(event) && event.public!!
                && (event.eventtime as Long) > (activity as MainActivity).time) {
                event.display = usersSnap.child(event.host + "/display").value as String
                lst.add(event)
            }
        }
        lst.sortWith(compareByDescending { it.going!!.size })
        return lst.take(7) as MutableList<OCEvent>
    }

    private fun setActivityAdapterLst() : MutableList<OCEvent> {
        val lst = mutableListOf<OCEvent>()
        val feedSnap = (activity as MainActivity).feedSnapshot
        val usersSnap = (activity as MainActivity).usersSnapshot
        val eventsSnap = (activity as MainActivity).eventsSnapshot
        for (feedSnapChild in feedSnap.children) {
            for (postSnap in feedSnapChild.children) {
                val event = eventsSnap.child(feedSnapChild.key!!).getValue(OCEvent::class.java)!!
                if (utilities.validateEvent(event) && event.public!!
                    && !(usersSnap.child(postSnap.key + "/incognito").value as Boolean)
                    && (event.eventtime as Long) > (activity as MainActivity).time
                ) {
                    event.display= usersSnap.child(event.host + "/display").value as String
                    event.timestampActivity = postSnap.child("timestamp").value as Long
                    event.displayActivity = usersSnap.child(postSnap.key + "/display").value as String
                    event.userActivity = postSnap.key
                    lst.add(event)
                }
            }
        }
        lst.sortWith(compareByDescending { it.timestampActivity })
        return lst
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}

class HomeAdapter(val activity: MainActivity, val lst: MutableList<OCEvent>) : RecyclerView.Adapter<ViewHolder>() {

    private val storage = FirebaseStorage.getInstance().getReference("events")
    private val utilities = Utilities.instance

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val viewHolder = layoutInflater.inflate(R.layout.fragment_home_vh, parent, false)
        return ViewHolder(viewHolder)
    }

    override fun getItemCount(): Int {
        return lst.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = lst.get(position)
        utilities.loadImage(activity ,storage.child(event.eventid +".png"), holder.view.imgView, "DEFAULT")
        holder.view.textViewTitle.text = event.title
        holder.itemView.setOnClickListener{
            holder.itemView.isClickable = false
            val fragment = EventFragment.newInstance(event, (activity as MainActivity).NAV_HOME)
            activity.inflate(fragment, (activity as MainActivity).NAV_HOME)
            Handler().postDelayed({
                holder.itemView.isClickable = true
            }, 1000)
        }
    }

}

class ActivityAdapter (val activity: MainActivity, val lst: MutableList<OCEvent>): RecyclerView.Adapter<ViewHolder>() {

    private val storage = FirebaseStorage.getInstance().getReference("users")
    private val utilities = Utilities.instance

    override fun getItemCount(): Int {
        return lst.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val viewHolder = layoutInflater.inflate(R.layout.fragment_home_vh_activity, parent, false)
        return ViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = lst.get(position)
        utilities.loadImage(activity ,storage.child(event.userActivity +".png"), holder.view.imageViewProPic, "CIRCLE")
        holder.view.textViewEventName.text = event.title
        if (event.userActivity != event.host) {
            holder.view.textViewUser.text = event.displayActivity + " joined..."
        } else {
            holder.view.textViewUser.text = event.displayActivity + " created..."
        }
        holder.itemView.setOnClickListener{
            holder.itemView.isClickable = false
            val fragment = EventFragment.newInstance(event, (activity as MainActivity).NAV_HOME)
            activity.inflate(fragment, (activity as MainActivity).NAV_HOME)
            Handler().postDelayed({
                //doSomethingHere()
                holder.itemView.isClickable = true
            }, 1000)
        }
    }
}
