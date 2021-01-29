package dev.davebaraka.ourcampus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_profile.*

class ProfileFragment : Fragment() {

    private val utilities = Utilities.instance
    private val storage = FirebaseStorage.getInstance()
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var textViewFragmentName: TextView
    private lateinit var imgViewProPic: ImageView
    private lateinit var imgBtnNotifications: ImageButton
    private lateinit var cardViewSchedule: CardView
    private lateinit var cardViewReviews: CardView
    private lateinit var cardViewSubscriptions: CardView
    private lateinit var cardViewGroups: CardView
    private lateinit var cardViewMore: CardView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_profile, container, false)
        textViewFragmentName = view.findViewById<TextView>(R.id.textViewFragmentName)
        cardViewReviews = view.findViewById(R.id.cardViewReviews)
        cardViewSchedule = view.findViewById(R.id.cardViewSchedule)
        cardViewSubscriptions = view.findViewById(R.id.cardViewSubscriptions)
        cardViewGroups = view.findViewById(R.id.cardViewGroups)
        cardViewMore = view.findViewById(R.id.cardViewMore)
        imgViewProPic = view.findViewById<ImageView>(R.id.imgViewProPic)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        imgBtnNotifications = view.findViewById(R.id.imgBtnNotifications)

        swipeRefresh.isEnabled = false

        utilities.loadImage(context!!, storage.getReference("users/" +(activity as MainActivity).userUID+ ".png"), imgViewProPic, "CIRCLE")

        textViewFragmentName.text = (activity as MainActivity).userDisplayName
        cardViewSchedule.setOnClickListener {
            (activity as MainActivity).inflate(EventsFragment.newInstance(false), (activity as MainActivity).NAV_PROFILE)
        }
        cardViewReviews.setOnClickListener {
            utilities.toast("Coming soon", "INFO", context!!, null)
        }
        cardViewSubscriptions.setOnClickListener {
            (activity as MainActivity).inflate(SubscriptionsFragment.newInstance(), (activity as MainActivity).NAV_PROFILE)
        }
        cardViewGroups.setOnClickListener {
            utilities.toast("Coming soon", "INFO", context!!, null)
        }
        cardViewMore.setOnClickListener {
            val usersSnap = (activity as MainActivity).usersSnapshot
            val intent = Intent(context, SettingsActivity::class.java)
            intent.putExtra("incognito", usersSnap.child((activity as MainActivity).userUID + "/incognito").value as Boolean)
            intent.putExtra("display", usersSnap.child((activity as MainActivity).userUID + "/display").value as String)
            intent.putExtra("year", usersSnap.child((activity as MainActivity).userUID + "/year").value as String)
            startActivity(intent)
        }
        imgBtnNotifications.setOnClickListener{
            (activity as MainActivity).inflate(NotificationsFragment.newInstance(), (activity as MainActivity).NAV_PROFILE)
        }

        return view
    }

    interface OnFragmentInteractionListener

    companion object {
        // @return A new instance of fragment ProfileFragment.
        @JvmStatic
        fun newInstance() = ProfileFragment()
    }
}

