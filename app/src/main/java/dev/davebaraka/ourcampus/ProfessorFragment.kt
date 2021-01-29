package dev.davebaraka.ourcampus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.*
import android.widget.DatePicker
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_professor_vh.view.*
import kotlinx.android.synthetic.main.fragment_profile.*
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ProfessorFragment : Fragment() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var textViewProfessor: TextView
    private lateinit var textViewDifficulty: TextView
    private lateinit var textViewOverallRating: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var imgBtnBack: ImageButton

    private lateinit var professor: String
    private var rating = 0.0
    private var difficulty = 0.0
    private lateinit var reviews: ArrayList<OCReview>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_professor, container, false)
        professor = arguments?.get("PROFESSOR") as String
        rating = arguments?.get("RATING") as Double
        difficulty= arguments?.get("DIFFICULTY") as Double
        reviews = arguments?.get("REVIEWS") as ArrayList<OCReview>

        textViewProfessor= view.findViewById(R.id.textViewProfessor)
        textViewDifficulty = view.findViewById(R.id.textViewDifficulty)
        textViewOverallRating = view.findViewById(R.id.textViewOverallRating)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        recyclerView = view.findViewById(R.id.recyclerView)
        imgBtnBack = view.findViewById(R.id.imgBtnBack)

        swipeRefresh.isEnabled = false
        textViewProfessor.text = professor
        textViewOverallRating.text = rating.toString().take(3)
        textViewDifficulty.text = difficulty.toString().take(3)

        recyclerView.isNestedScrollingEnabled = false
        recyclerView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = ReviewsAdapter(activity as MainActivity, reviews)

        imgBtnBack.setOnClickListener {
            (activity as MainActivity).onBackPressed()
        }


        return view
    }

    companion object {
        // @return A new instance of fragment ProfileFragment.
        @JvmStatic
        fun newInstance(professor:String, rating: Double, difficulty:Double, reviews: ArrayList<Parcelable>) = ProfessorFragment().apply {
            arguments = Bundle().apply {
                putString("PROFESSOR", professor)
                putDouble("RATING", rating)
                putDouble("DIFFICULTY", difficulty)
                putParcelableArrayList("REVIEWS", reviews)
            }
        }
    }
}

class ReviewsAdapter (val activity: MainActivity, val lst: ArrayList<OCReview>): RecyclerView.Adapter<ViewHolder>() {

    private val utilities = Utilities.instance
    private val data = BooleanArray(lst.size) {false}

    override fun getItemCount(): Int {
        return lst.size
    }

    fun decode(value: Int): String {
        when (value) {
            5 -> return "Excellent"
            4 -> return "Very Good"
            3 -> return "Good"
            2 -> return "Fair"
            else -> return "Ineffective"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val viewHolder = layoutInflater.inflate(R.layout.fragment_professor_vh, parent, false)
        return ViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = lst.get(position)
        holder.itemView.ratingBar.rating = review.rate!!.toFloat()
        holder.itemView.textViewDate.text = SimpleDateFormat("MMM d, yyyy", Locale.US).format(review.timestamp)
        if (review.rate!! == 5) {
            holder.itemView.textViewSummary.text = "Awesome"
        } else if (review.rate!! >= 4)  {
            holder.itemView.textViewSummary.text = "Good"
        } else if (review.rate!! >= 3) {
            holder.itemView.textViewSummary.text = "Average"
        } else if (review.rate!! >= 2) {
            holder.itemView.textViewSummary.text = "Poor"
        } else {
            holder.itemView.textViewSummary.text = "Awful"
        }

        holder.itemView.constraintLayout.visibility  = if (data[position]) View.VISIBLE else View.GONE
        holder.itemView.imgBtnDropdown.setImageDrawable(if (data[position]) ContextCompat.getDrawable(activity, R.drawable.ic_chevron_up_outline) else ContextCompat.getDrawable(activity, R.drawable.ic_chevron_down_outline))

        holder.itemView.imgBtnDropdown.setOnClickListener {
            if (data[position]) {
                data[position] = false
                holder.itemView.constraintLayout.visibility = View.GONE
                holder.itemView.imgBtnDropdown.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_chevron_down_outline))
            } else {
                data[position] = true
                holder.itemView.constraintLayout.visibility = View.VISIBLE
                holder.itemView.imgBtnDropdown.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_chevron_up_outline))
            }
        }

        holder.itemView.textViewOverallRating.text = review.rate.toString().take(3)
        holder.itemView.textViewDifficulty.text = review.difficulty.toString().take(3)


        holder.itemView.textViewClass.text = review.course
        holder.itemView.textViewRecommended.text = review.recommend

        holder.itemView.textViewSkill.text = decode(review.lecture!!)
        holder.itemView.textViewAccessible.text = if (review.accessible == "Accessible") "Yes" else "No"
        holder.itemView.textViewReturn.text = decode(review.returns!!)
        holder.itemView.textViewFeedback.text = review.feedback
        holder.itemView.textViewDiscussion.text = decode(review.discussion!!)
        holder.itemView.textViewGrade.text = if (review.grade != "") review.grade else "N/A"
        holder.itemView.textViewComment.text = review.comment


    }
}

