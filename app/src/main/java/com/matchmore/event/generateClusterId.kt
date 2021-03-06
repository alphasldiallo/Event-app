package com.matchmore.event

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.gun0912.tedpermission.PermissionListener
import io.matchmore.sdk.api.models.Publication
import java.util.*
import io.matchmore.sdk.Matchmore
import com.gun0912.tedpermission.TedPermission
import io.matchmore.sdk.api.models.MatchmoreLocation
import io.matchmore.sdk.api.models.Subscription
import kotlinx.android.synthetic.main.createcluster.*
import kotlin.collections.HashMap

class generateClusterId : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_cluster_id)

        val API_KEY = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJhbHBzIiwic3ViIjoiNjQyZDJmOTktZTcxYS00NzA5LWJmMWYtZjhkZWFkY2Q3OGE1IiwiYXVkIjpbIlB1YmxpYyJdLCJuYmYiOjE1MzIwMDQwMjgsImlhdCI6MTUzMjAwNDAyOCwianRpIjoiMSJ9.EWvR_yTClTvc7nT-avTitW7doImVE0Il7zjM5gEf1LIjKxyAi4ADj0bDpt2XZvUAH9lfAkVEtXiAYjCjc9wApA"
        if (!Matchmore.isConfigured())
        {
            Matchmore.config(this,API_KEY,false)
        }
        checkLocationPermission()

        val clusterId = findViewById(R.id.clusterId) as TextView

        val id = generateRandom()

        val bundle = intent.extras

        var name:String = bundle.get("name").toString()


        Log.d("debug", "Name = "+name)
        clusterId.setText(id.toString())

        //Propagate the cluster ID
        createPub(id, name)

        //Get the information of the users who subscribed
        createSub(id)


        getMatches(name)


    }

    fun generateRandom(): Int {
        //This function will generate a random number between 1000 and 9999
        return Random().nextInt(9999 + 1 - 1000) + 1000
    }

    private fun createPub(i: Int, n: String)
    {
        Matchmore.instance.apply {
            startUsingMainDevice ({ device ->

                Log.d("debug","Currently using Device "+device.name)

                val pub = Publication("cluster", 500.0, 180.0)
                pub.properties = hashMapOf("created_by" to n, "room_number" to i.toString())
                createPublicationForMainDevice(pub,
                        { result ->

                            Log.d("debug","Publication made successfully with properties "+pub.properties.toString())
                        }, Throwable::printStackTrace)
            }, Throwable::printStackTrace)
        }
    }

    private fun createSub(i: Int)
    {
        Log.d ("debug", "createSub")
        Matchmore.instance.apply {
            startUsingMainDevice ({ d ->
                //Log.d("debug", "location_sub "+d.location.toString())
                Log.d("debug", "location_sub "+Matchmore.instance.locationManager.lastLocation.toString())
                val subscription = Subscription("cluster_joined_"+i.toString(), 500.0, 180.0)


                Log.d("debug", subscription.selector.toString())
                createSubscriptionForMainDevice(subscription, { result ->
                    Log.d("debug", "Subscription made successfully with topic ${result.topic}")

                }, Throwable::printStackTrace)
            }, Throwable::printStackTrace)
        }
    }

    private fun getMatches (name:String)
    {
        val listView = findViewById (R.id.list_members) as ListView

        // Empty Array that will used to store the properties of the publications
        var rsl: ArrayList<String> = ArrayList()
        var buddy: String
        var buddy_location: String
        var location:HashMap<String, String> = HashMap <String, String>()

        Log.d("debug", "Check matches")
        rsl.add(name)


        Matchmore.instance.apply {

            //Start fetching matches
            matchMonitor.addOnMatchListener { matches, _ ->
                //We should get there every time a match occur

                Log.d("debug", "We got ${matches.size} matches")

                val first = matches.first()

                Log.d("debug", "Matches = "+first.publication!!.properties["location"].toString())
                //Let's fill our Array with the properties of the publication
                buddy = first.publication!!.properties["created_by"].toString()

                buddy_location = first.publication!!.properties["location"].toString()


                rsl.add(buddy)

                location.put(buddy, buddy_location)


                var adapter = ArrayAdapter(this@generateClusterId, android.R.layout.simple_list_item_1, rsl)
                listView.adapter = adapter as ListAdapter?

                val findBtn = findViewById(R.id.generate_find_btn) as Button

                findBtn.setOnClickListener()
                {
                    intent.setClass(this@generateClusterId, live::class.java)
                    intent.putExtra("data", location)
                    Log.d("debug", "location = "+location.toString())
                    startActivity(intent)
                }



            }
            matchMonitor.startPollingMatches(100)
        }

    }

    private fun fullscreen() {
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    private fun checkLocationPermission() {
        val permissionListener = object : PermissionListener {
            @SuppressLint("MissingPermission")
            override fun onPermissionGranted() {
                Matchmore.instance.apply {
                    startUpdatingLocation()
                    startRanging()

                }
            }

            override fun onPermissionDenied(deniedPermissions: ArrayList<String>) {
                Toast.makeText(this@generateClusterId, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
        TedPermission.with(this)
                .setPermissionListener(permissionListener)
                .setDeniedMessage("Permission Denied")
                .setPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
                .check()
    }
}