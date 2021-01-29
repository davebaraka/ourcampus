package dev.davebaraka.ourcampus

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.InetAddresses
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.fragment_event.view.*
import kotlinx.android.synthetic.main.fragment_subscriptions_vh_courses.view.*
import kotlinx.android.synthetic.main.fragment_subscriptions_vh_courses.view.textViewTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import java.net.InetAddress
import java.util.*

class SubscriptionsFragment : Fragment() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var imgBtnBack: ImageButton
    private lateinit var searchView: SearchView
    private lateinit var constraintLayout: ConstraintLayout
    private lateinit var recyclerViewSubscriptions: RecyclerView
    private lateinit var recyclerViewSearch: RecyclerView
    private lateinit var imgBtnInfo: ImageButton
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var textViewNull: TextView
    private lateinit var coursesAdapterLst: MutableList<Map<String, Any?>>
    private lateinit var subscriptionsAdapterLst: MutableList<Map<String, Any?>>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_subscriptions, container, false)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        imgBtnBack = view.findViewById(R.id.imgBtnBack)
        searchView = view.findViewById(R.id.searchView)
        constraintLayout = view.findViewById(R.id.constraintLayout)
        recyclerViewSubscriptions = view.findViewById(R.id.recyclerViewSubscriptions)
        recyclerViewSearch = view.findViewById(R.id.recyclerViewSearch)
        textViewNull = view.findViewById(R.id.textViewNull)
        appBarLayout = view.findViewById(R.id.appBarLayout)
        imgBtnInfo = view.findViewById(R.id.imgBtnInfo)

        recyclerViewSubscriptions.layoutManager = LinearLayoutManager(activity)
        recyclerViewSubscriptions.isMotionEventSplittingEnabled = false
        subscriptionsAdapterLst = setSubscriptionsAdapterLst()
        recyclerViewSubscriptions.adapter = CoursesAdapter(activity as MainActivity, subscriptionsAdapterLst)

        recyclerViewSearch.layoutManager = LinearLayoutManager(activity)
        recyclerViewSearch.isMotionEventSplittingEnabled = false
        coursesAdapterLst = setCoursesAdapterLst()
        val recyclerViewSearchAdapter = CoursesAdapter(activity as MainActivity, coursesAdapterLst)
        recyclerViewSearch.adapter = recyclerViewSearchAdapter

        imgBtnInfo.setOnClickListener {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("What are Course Subscriptions?")
            builder.setMessage("Course Subscriptions allows you to be notified when a seat becomes available for a subscribed course during adjustment period. A subscription is valid when a course currently has 0 seats available on WesMaps.")
            builder.setPositiveButton("DISMISS") { _, _ -> }
            builder.create()
            builder.show()
        }

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                recyclerViewSearchAdapter.filter.filter(newText)
                return false
            }

        })

        searchView.setOnQueryTextFocusChangeListener(object: View.OnFocusChangeListener {
            override fun onFocusChange(p0: View?, p1: Boolean) {
                if (p1) {
                    recyclerViewSearch.visibility = View.VISIBLE
                    constraintLayout.visibility = View.GONE
                    imgBtnBack.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_chevron_down_outline))
                    recyclerViewSearch.adapter!!.notifyDataSetChanged()
                    swipeRefresh.isEnabled = false
                } else {
                    recyclerViewSearch.visibility = View.GONE
                    constraintLayout.visibility = View.VISIBLE
                    appBarLayout.setExpanded(true)
                    imgBtnBack.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_chevron_left_outline))
                    subscriptionsAdapterLst = setSubscriptionsAdapterLst()
                    recyclerViewSubscriptions.adapter = CoursesAdapter(activity as MainActivity, subscriptionsAdapterLst)
                    swipeRefresh.isEnabled = true
                }
            }

        })

        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.material_gray))
        swipeRefresh.setOnRefreshListener {
            swipeRefresh.isEnabled = false
            constraintLayout.visibility = View.GONE
            subscriptionsAdapterLst = setSubscriptionsAdapterLst()
            recyclerViewSubscriptions.adapter = CoursesAdapter(activity as MainActivity, subscriptionsAdapterLst)
            constraintLayout.visibility = View.VISIBLE
            swipeRefresh.isRefreshing = false
            swipeRefresh.isEnabled = true
        }

        imgBtnBack.setOnClickListener {
            if (searchView.hasFocus()) {
                val imm = (activity as MainActivity).getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                searchView.clearFocus()
                recyclerViewSearch.visibility = View.GONE
                constraintLayout.visibility = View.VISIBLE
                imgBtnBack.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_chevron_left_outline))
                subscriptionsAdapterLst = setSubscriptionsAdapterLst()
                recyclerViewSubscriptions.adapter = CoursesAdapter(activity as MainActivity, subscriptionsAdapterLst)
                swipeRefresh.isEnabled = true
            } else {
                (activity as MainActivity).onBackPressed()
            }
        }

        return view
    }

    fun setCoursesAdapterLst():MutableList<Map<String, Any?>> {
        val lst = mutableListOf<Map<String, Any?>>()
        val coursesSnap = (activity as MainActivity).coursesSnapshot
        for (courseSnap in coursesSnap.children) {
            val description = courseSnap.child("description").value as String?
            val instructor = courseSnap.child("instructor").value as String?
            val courseLink = courseSnap.child("course_link").value as String?
            val courseId = courseSnap.key as String
            lst.add(mapOf(
                "description" to description,
                "instructor" to instructor,
                "courseLink" to courseLink,
                "courseId" to courseId,
                "subscribed" to false
                ))
        }
        return lst
    }

    fun setSubscriptionsAdapterLst():MutableList<Map<String, Any?>> {
        val lst = mutableListOf<Map<String, Any?>>()
        val coursesSnap = (activity as MainActivity).coursesSnapshot
        val usersSnap = (activity as MainActivity).usersSnapshot
        val userUID = (activity as MainActivity).userUID
        for (courseSnap in coursesSnap.children) {
            if (usersSnap.hasChild(userUID + "/Subscriptions/" + courseSnap.key as String)) {
                val description = courseSnap.child("description").value as String?
                val instructor = courseSnap.child("instructor").value as String?
                val courseLink = courseSnap.child("course_link").value as String?
                val courseId = courseSnap.key as String
                lst.add(mapOf(
                    "description" to description,
                    "instructor" to instructor,
                    "courseLink" to courseLink,
                    "courseId" to courseId,
                    "subscribed" to false
                ))
            }
        }
        if (lst.isEmpty()) {
            recyclerViewSubscriptions.visibility = View.GONE
            textViewNull.visibility = View.VISIBLE
        } else {
            textViewNull.visibility = View.GONE
            recyclerViewSubscriptions.visibility = View.VISIBLE
        }
        return lst
    }

    companion object {
        @JvmStatic
        fun newInstance() = SubscriptionsFragment()
    }
}

class CoursesAdapter(val activity: MainActivity, val lst:MutableList<Map<String, Any?>>): RecyclerView.Adapter<ViewHolder>(), Filterable {

    private val utilities = Utilities.instance
    private val usersSnap = (activity as MainActivity).usersSnapshot
    private val userUID = (activity as MainActivity).userUID
    private val database = FirebaseDatabase.getInstance()

    private var lstFull = lst.toMutableList()

    override fun getItemCount(): Int {
        return lst.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val viewHolder = layoutInflater.inflate(R.layout.fragment_subscriptions_vh_courses, parent, false)
        return ViewHolder(viewHolder)
    }

    override fun getFilter(): Filter{
        return searchFilter
    }

    private val searchFilter = object: Filter() {
        override fun performFiltering(p0: CharSequence?): FilterResults {
            val filteredLst = mutableListOf<Map<String, Any?>>()
            if (p0 == null || p0.length == 0) {
                filteredLst.addAll(lstFull)
            } else {
                val filterPattern = p0.toString().toLowerCase(Locale.US).trim()
                for (item in lstFull) {
                    var query = item["instructor"] as String + " " + item["description"] as String + " " + item["courseId"] as String
                    query = query.toLowerCase(Locale.US).trim()

                    if (query.contains(filterPattern)) {
                        filteredLst.add(item)
                    }
                }
            }
            val results = FilterResults()
            results.values = filteredLst
            return results
        }
        override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
            lst.clear()
            lst.addAll(p1!!.values as MutableList<Map<String, Any?>>)
            notifyDataSetChanged()
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = lst.get(position)
        holder.view.textViewInstructor.text = data["instructor"] as String
        holder.view.textViewTitle.text = data["description"] as String
        holder.view.textViewCourseId.text = data["courseId"] as String

        holder.view.cardViewUnsubscribe.setOnClickListener {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("Unsubscribe")
            builder.setMessage("Are you sure you want to unsubscribe from " + data["courseId"] as String + "?")
            builder.setPositiveButton("Yes") { _, _ ->
                database.getReference("Subscriptions/" + data["courseId"] as String + "/" + userUID).removeValue()
                database.getReference("Users/" + userUID +"/Subscriptions/" + data["courseId"]).removeValue()
                holder.view.cardViewUnsubscribe.visibility = View.GONE
                holder.view.cardViewSubscribe.visibility = View.VISIBLE
                utilities.toast("You have unsubscribed from " + data["courseId"], "SUCCESS", activity, null)
            }
            builder.setNegativeButton("No", null)
            builder.create()
            builder.show()
        }
        holder.view.cardViewSubscribe.setOnClickListener{
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("Subscribe")
            builder.setMessage("Would you like to subscribe to " + data["courseId"] as String + "?")
            builder.setPositiveButton("Yes") { _, _ ->
                database.getReference("Subscriptions/" + data["courseId"] as String + "/" + userUID).setValue("nil")
                database.getReference("Users/" + userUID +"/Subscriptions/" + data["courseId"]).setValue("nil")
                utilities.toast("You have subcribed to " + data["courseId"], "SUCCESS", activity, null)
                holder.view.cardViewUnsubscribe.visibility = View.VISIBLE
                holder.view.cardViewSubscribe.visibility = View.GONE
            }
            builder.setNegativeButton("No", null)
            builder.create()
            builder.show()
        }

        val path = userUID + "/Subscriptions/" + data["courseId"]
        holder.view.cardViewUnsubscribe.visibility =
            if (activity.usersSnapshot.hasChild(path)) View.VISIBLE else View.GONE
        holder.view.cardViewSubscribe.visibility =
            if (!activity.usersSnapshot.hasChild(path)) View.VISIBLE else View.GONE

        if (URLUtil.isValidUrl(data["courseLink"] as String?)) {
            holder.view.imgBtnLink.setOnClickListener {
                startActivity(activity, Intent(Intent.ACTION_VIEW, Uri.parse(data["courseLink"] as String?)), null)
            }
        } else {
            holder.view.imgBtnLink.setOnClickListener {
                utilities.toast("No link", "INFO", activity, null)
            }
        }
    }
}
