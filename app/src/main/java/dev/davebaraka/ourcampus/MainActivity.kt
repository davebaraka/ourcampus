package dev.davebaraka.ourcampus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.TypefaceCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.internal.InternalTokenResult
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MainActivity : AppCompatActivity(),
    EventsFragment.OnFragmentInteractionListener,
    RateFragment.OnFragmentInteractionListener,
    ProfileFragment.OnFragmentInteractionListener {

    // There was a problem that the stack was not loaded when pressing back before this fragment
    // completly loaded, maybe look into it...
    var loaded = false

    /*
    Instantiate main fragments
     */
    private var homeFragment: HomeFragment = HomeFragment.newInstance()
    private var eventsFragment: EventsFragment = EventsFragment.newInstance()
    private var rateFragment: RateFragment = RateFragment.newInstance()
    private var profileFragment: ProfileFragment = ProfileFragment.newInstance()

    /*
    Firebase database objects
     */
    lateinit var eventsSnapshot: DataSnapshot
    lateinit var usersSnapshot: DataSnapshot
    lateinit var feedSnapshot: DataSnapshot
    lateinit var ratingsSnapshot: DataSnapshot
    lateinit var coursesSnapshot: DataSnapshot
    lateinit var userUID: String
    lateinit var userDisplayName: String

    /*
    Navigation back stacks
    */
    val NAV_HOME = 0
    val NAV_EVENTS = 1
    val NAV_RATE = 2
    val NAV_PROFILE = 3
    private var queue = NAV_HOME //Starting stack
    private var backstack = mapOf<Int, MutableList<Fragment>>(
        NAV_HOME to mutableListOf(),
        NAV_EVENTS to mutableListOf(),
        NAV_RATE to mutableListOf(),
        NAV_PROFILE to mutableListOf()
    )

    /*
    Threshold for event time
    */
    val time : Double = System.currentTimeMillis().toDouble() - 10800000.toDouble() //-3hrs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        main()
    }

    override fun onBackPressed() {
        if (loaded) {
            val stack = backstack[queue]!!
            if (stack.size == 1) {
                return
            } else {
                //I did not know this returns the fragment when removed, can prolly clean up above funciton
                val _fragment= stack.removeAt(0)
                val fragment = stack[0]
                supportFragmentManager.beginTransaction().remove(_fragment)
                    .show(fragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE).commit()
            }
        }
    }

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                transactNavigation(homeFragment, NAV_HOME)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_events -> {
                transactNavigation(eventsFragment, NAV_EVENTS)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_rate -> {
                transactNavigation(rateFragment, NAV_RATE)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_profile -> {
                transactNavigation(profileFragment, NAV_PROFILE)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    fun inflate(fragment: Fragment, stackGroup: Int) {
        val _fragment = backstack[queue]!![0]
        backstack[stackGroup]!!.add(0, fragment)
        supportFragmentManager.beginTransaction().hide(_fragment)
            .add(R.id.fragment_container, fragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commitNow()
        queue = stackGroup
    }

    fun popBackStack(stackGroup: Int) {
        val fragment = backstack[stackGroup]!![0]
        backstack[stackGroup]!!.removeAt(0)
        val _fragment = backstack[stackGroup]!![0]
        supportFragmentManager.beginTransaction().remove(fragment).show(_fragment).commit()
        queue = stackGroup
    }

    //For Main Naviagtion Buttons
    private fun transactNavigation(fragment: Fragment, stackGroup: Int) {
        if (queue != stackGroup) {
            val _fragment = backstack[queue]!![0]
            if (backstack[stackGroup]!!.isEmpty()) {
                backstack[stackGroup]!!.add(0, fragment)
                supportFragmentManager.beginTransaction().hide(_fragment)
                    .add(R.id.fragment_container, fragment).commit()
            } else {
                if (backstack[stackGroup]!![0] != fragment) {
                    supportFragmentManager.beginTransaction().hide(_fragment)
                        .show(backstack[stackGroup]!![0]).commit()
                } else {
                    supportFragmentManager.beginTransaction().hide(_fragment)
                        .show(backstack[stackGroup]!![0]).commit()
                }
            }
        } else {
            if (backstack[stackGroup]!!.isEmpty()) {
                backstack[stackGroup]!!.add(0, fragment)
                supportFragmentManager.beginTransaction().add(R.id.fragment_container, fragment).commit()
            } else {
                if (backstack[stackGroup]!![0] != fragment) {
                    val stack = backstack[queue]!![0]
                    backstack[stackGroup]!!.removeAt(0)
                    supportFragmentManager.beginTransaction().remove(stack)
                        .show(backstack[stackGroup]!![0]).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE).commit()
                }
            }
        }
        queue = stackGroup
        return
    }

    private fun main() {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }
                // Get new Instance ID token
                val token = task.result?.token as String
                val database = FirebaseDatabase.getInstance()
                val user = FirebaseAuth.getInstance().currentUser!!.uid
                val ref = database.getReference("Users/" + user + "/token")
                ref.setValue(token)

                FirebaseMessaging.getInstance().subscribeToTopic("general")
                FirebaseMessaging.getInstance().isAutoInitEnabled = true
            })

        val utilities = Utilities.instance

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val swipeRefresh: SwipeRefreshLayout = findViewById(R.id.swipeRefresh)

        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent))
        swipeRefresh.isEnabled = false
        swipeRefresh.isRefreshing = true

        /*
        The following loads all the data snapshots and loads the first fragment
        */
        val database = FirebaseDatabase.getInstance()
        val refEvents = database.getReference("Events")
        val refFeed = database.getReference("Feed")
        val refUsers = database.getReference("Users")
        val refCourses = database.getReference("Spring20")
        val refRatings = database.getReference("Ratings")

        refUsers.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                utilities.toast(
                    "An unknown error occured",
                    "ERROR",
                    this@MainActivity,
                    null)
            }
            override fun onDataChange(p0: DataSnapshot) {
                usersSnapshot = p0
                refEvents.addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                        utilities.toast(
                            "An unknown error occured",
                            "ERROR",
                            this@MainActivity,
                            null)
                    }
                    override fun onDataChange(p0: DataSnapshot) {
                        eventsSnapshot = p0
                        refFeed.addListenerForSingleValueEvent(object: ValueEventListener{
                            override fun onCancelled(p0: DatabaseError) {
                                utilities.toast(
                                    "An unknown error occured",
                                    "ERROR",
                                    this@MainActivity,
                                    null)
                            }
                            override fun onDataChange(p0: DataSnapshot) {
                                feedSnapshot = p0
                                refCourses.addListenerForSingleValueEvent(object: ValueEventListener{
                                    override fun onCancelled(p0: DatabaseError) {
                                        utilities.toast(
                                            "An unknown error occured",
                                            "ERROR",
                                            this@MainActivity,
                                            null)
                                    }
                                    override fun onDataChange(p0: DataSnapshot) {
                                        coursesSnapshot = p0
                                        refRatings.addListenerForSingleValueEvent(object: ValueEventListener {
                                            override fun onCancelled(p0: DatabaseError) {
                                                utilities.toast(
                                                    "An unknown error occured",
                                                    "ERROR",
                                                    this@MainActivity,
                                                    null)
                                            }

                                            override fun onDataChange(p0: DataSnapshot) {
                                                ratingsSnapshot = p0
                                                userUID = FirebaseAuth.getInstance().currentUser!!.uid
                                                userDisplayName =
                                                    usersSnapshot.child(userUID).child("display").value as String
                                                swipeRefresh.isRefreshing = false
                                                navView.visibility = View.VISIBLE
                                                backstack[NAV_HOME]!!.add(0, homeFragment)
                                                supportFragmentManager.beginTransaction().add(R.id.fragment_container, homeFragment).commit()
                                                loaded = true
                                            }
                                        })
                                    }
                                })
                            }
                        })
                    }
                })
            }
        })

        /*
        The following will keep the data snapshots updated. So when refreshing each fragment,
        calling the data snapshot from this activity and doing what you need to do should be enough.
         */
        refUsers.addValueEventListener(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                usersSnapshot = p0
            }
        })
        refCourses.addValueEventListener(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                coursesSnapshot = p0
            }
        })
        refEvents.addValueEventListener(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                eventsSnapshot = p0
            }
        })
        refFeed.addValueEventListener(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                feedSnapshot = p0
            }
        })
        refRatings.addValueEventListener(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                ratingsSnapshot = p0
            }
        })
    }
}

class FirebaseMsgService: FirebaseMessagingService(){
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        sendRegistrationToken(token)
    }

    override fun onMessageReceived(p0: RemoteMessage) {
        val notification = p0.notification
        if (notification != null) {
            sendNotification(notification)
        }
        super.onMessageReceived(p0)
    }

    private fun sendNotification(notification: RemoteMessage.Notification) {
        val title = notification.title.toString()
        val messageBody = notification.body.toString()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(this, R.color.colorAccent))
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    private fun sendRegistrationToken(token: String) {
        if (FirebaseAuth.getInstance().currentUser != null) {
            val database = FirebaseDatabase.getInstance()
            val user = FirebaseAuth.getInstance().currentUser!!.uid
            val refUsers = database.getReference("Users/" + user + "/display")
            refUsers.addValueEventListener(object: ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                }
                override fun onDataChange(userSnapshot: DataSnapshot) {
                    if (userSnapshot.exists()) {
                        val ref = database.getReference("Users/" + user + "/token")
                        ref.setValue(token)
                    }
                }
            })
        }
    }
}