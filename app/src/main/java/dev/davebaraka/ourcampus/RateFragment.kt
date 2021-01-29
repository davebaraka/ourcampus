package dev.davebaraka.ourcampus

import android.app.AlertDialog
import android.content.Context
import android.net.InetAddresses
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.fragment_rate_vh.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*

class RateFragment : Fragment() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var imgBtnFilter: ImageButton
    private lateinit var imgBtnCreate: ImageButton
    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private lateinit var recyclerViewFilter: RecyclerView
    private lateinit var recyclerViewSearch: RecyclerView
    private lateinit var textViewFilterTitle: TextView
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var imgBtnDismiss: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_rate, container, false)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        imgBtnFilter = view.findViewById(R.id.imgBtnFilter)
        imgBtnCreate = view.findViewById(R.id.imgBtnCreate)
        searchView = view.findViewById(R.id.searchView)
        recyclerViewFilter = view.findViewById(R.id.recyclerViewFilter)
        recyclerViewSearch = view.findViewById(R.id.recyclerViewSearch)
        textViewFilterTitle = view.findViewById(R.id.textViewFilterTitle)
        appBarLayout = view.findViewById(R.id.appBarLayout)
        imgBtnDismiss = view.findViewById(R.id.imgBtnDismiss)

        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.material_gray))
        swipeRefresh.isEnabled = false

        GlobalScope.launch(Dispatchers.Main) {
            val srcLst = setRateAdapterLst()

            var rateAdapterLst =  srcLst.filter { (it["reviews"] as MutableList<OCReview>).size > 4 } as MutableList<Map<String, Any?>>
            rateAdapterLst.sortWith(compareByDescending { it["overallRating"] as Double })
            rateAdapterLst =  rateAdapterLst.take(50) as MutableList<Map<String, Any?>>

            recyclerViewFilter.isNestedScrollingEnabled = false
            recyclerViewFilter.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            recyclerViewFilter.adapter = RateAdapter(activity as MainActivity, rateAdapterLst)

            recyclerViewSearch.isNestedScrollingEnabled = false
            recyclerViewSearch.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            val recyclerViewSearchAdapter = SearchProfessorsAdapter(activity as MainActivity, srcLst)
            recyclerViewSearch.adapter = recyclerViewSearchAdapter

            searchView.setOnQueryTextListener(object: androidx.appcompat.widget.SearchView.OnQueryTextListener {
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
                        textViewFilterTitle.visibility = View.GONE
                        recyclerViewFilter.visibility = View.GONE
                        recyclerViewSearch.visibility = View.VISIBLE
                        imgBtnDismiss.visibility = View.VISIBLE
                        //recyclerViewSearch.adapter!!.notifyDataSetChanged()
                        //swipeRefresh.isEnabled = false
                    } else {
                        recyclerViewSearch.visibility = View.GONE
                        textViewFilterTitle.visibility = View.VISIBLE
                        recyclerViewFilter.visibility = View.VISIBLE
                        appBarLayout.setExpanded(true)
                        imgBtnDismiss.visibility = View.GONE
                        //swipeRefresh.isEnabled = true
                    }
                }

            })

            imgBtnDismiss.setOnClickListener {
                if (searchView.hasFocus()) {
                    val imm = (activity as MainActivity).getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    searchView.clearFocus()
                    recyclerViewSearch.visibility = View.GONE
                    textViewFilterTitle.visibility = View.VISIBLE
                    recyclerViewFilter.visibility = View.VISIBLE
                }
            }
        }

        buildAlertDialog().show()

        return view
    }

    private fun buildAlertDialog():AlertDialog.Builder{
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Attention")
        builder.setMessage("Research has shown unconscious biases in regards to race, gender, and other identities can at times negatively influence our professor evaluations. While these reviews do not directly impact teacher tenure and hiring, they could potentially influence a professor's class size, and therefore their career. Please keep these potential biases in mind when filling out an evaluation.")
        builder.setPositiveButton("DISMISS") { _, _ -> }
        builder.setCancelable(false)
        builder.create()
        return builder
    }

    fun setRateAdapterLst(): MutableList<Map<String, Any?>> {
        val ratingsSnap = (activity as MainActivity).ratingsSnapshot
        val rateLst = mutableListOf<Map<String, Any?>>()
        for (professorSnap in ratingsSnap.children) {
            val professor = professorSnap.key
            var overallRating = 0.0
            var overallDifficulty = 0.0
            val lst: MutableList<OCReview> = mutableListOf()
            for (reviewSnap in professorSnap.children) {
                val accessible = reviewSnap.child("accessible").value as String
                val course = reviewSnap.child("class").value as String
                val comment = reviewSnap.child("comments").value as String
                val difficulty = (reviewSnap.child("difficulty").value as String).toInt()
                val discussion = (reviewSnap.child("discussion").value as String).toInt()
                val feedback = reviewSnap.child("feedback").value as String
                val grade = reviewSnap.child("grade").value as String
                val lecture = (reviewSnap.child("lecture").value as String).toInt()
                val rate = (reviewSnap.child("rate").value as String).toInt()
                val recommend = reviewSnap.child("recommend").value as String
                val returns = (reviewSnap.child("returns").value as String).toInt()
                val date = (reviewSnap.child("timestamp").value as String).replace("/", "-").replace(".", "-").replace(",", "")
                val timestamp = if (date.endsWith("M")) SimpleDateFormat("MM-dd-yy h:mm a", Locale.US).parse(date)!!.time else SimpleDateFormat("MM-dd-yy hh:mm", Locale.US).parse(date)!!.time
                overallRating += rate
                overallDifficulty += difficulty

                val review: OCReview = OCReview(accessible, course, comment, difficulty, discussion, feedback, grade, lecture, rate, recommend, returns, timestamp)
                lst.add(review)

            }
            lst.sortWith(compareByDescending { it.timestamp })
            rateLst.add(mapOf(
                "professor" to professor,
                "overallRating" to overallRating/lst.size,
                "overallDifficulty" to overallDifficulty/lst.size,
                "reviews" to lst
            ))
        }
        return rateLst
    }

    interface OnFragmentInteractionListener

    companion object {
        // @return A new instance of fragment RateFragment.
        @JvmStatic
        fun newInstance() = RateFragment()
    }
}

class RateAdapter (val activity: MainActivity, val lst: MutableList<Map<String, Any?>>): RecyclerView.Adapter<ViewHolder>() {

    private val utilities = Utilities.instance

    override fun getItemCount(): Int {
        return lst.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val viewHolder = layoutInflater.inflate(R.layout.fragment_rate_vh, parent, false)
        return ViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rating = lst.get(position)
        val overallRating = rating["overallRating"] as Double
        val overallDifficulty = rating["overallDifficulty"] as Double
        val array = arrayListOf<Parcelable>()
        array.addAll(rating["reviews"] as MutableList<OCReview>)
        val professor = rating["professor"] as String

        holder.view.textViewOverallRating.text = overallRating.toString().take(3)
        holder.view.textViewProfessor.text = professor

        holder.itemView.setOnClickListener{
            holder.itemView.isClickable = false
            activity.inflate(ProfessorFragment.newInstance(professor, overallRating, overallDifficulty, array), activity.NAV_RATE)
            Handler().postDelayed({
                holder.itemView.isClickable = true
            }, 1000)
        }
    }
}

class SearchProfessorsAdapter (val activity: MainActivity, val lst: MutableList<Map<String, Any?>>): RecyclerView.Adapter<ViewHolder>(),
    Filterable {

    private var lstFull = lst.toMutableList()

    override fun getItemCount(): Int {
        return lst.size
    }

    override fun getFilter(): Filter {
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
                    var query = item["professor"] as String
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val viewHolder = layoutInflater.inflate(R.layout.fragment_rate_vh_search, parent, false)
        return ViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rating = lst.get(position)
        val overallRating = rating["overallRating"] as Double
        val overallDifficulty = rating["overallDifficulty"] as Double
        val array = arrayListOf<Parcelable>()
        array.addAll(rating["reviews"] as MutableList<OCReview>)
        val professor = rating["professor"] as String

        holder.view.textViewOverallRating.text = overallRating.toString().take(3)
        holder.view.textViewProfessor.text = professor

        holder.itemView.setOnClickListener{
            holder.itemView.isClickable = false
            activity.inflate(ProfessorFragment.newInstance(professor, overallRating, overallDifficulty, array), activity.NAV_RATE)
            Handler().postDelayed({
                holder.itemView.isClickable = true
            }, 1000)
        }
    }
}
