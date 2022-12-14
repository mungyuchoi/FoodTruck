package com.mungyu.foodtruck

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.renderscript.Sampler.Value
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.formats.NativeAdOptions
import com.google.android.gms.ads.formats.UnifiedNativeAd
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mungyu.foodtruck.databinding.ActivityMapsBinding
import com.mungyu.foodtruck.model.RequestDelete
import com.tbruyelle.rxpermissions3.RxPermissions
import java.text.SimpleDateFormat

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var isCallCurrentPosition = false
    private lateinit var persistentBottomSheetBehavior: BottomSheetBehavior<*>

    private val mapInfo = mutableMapOf<String, String>()
    private val mapDate = mutableMapOf<String, Long>()
    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private var markerLatitude = 0.0
    private var markerLongitude = 0.0
    private var exitDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
        initFirebase()
        initPermission()
        initAdmob()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult requestCode: $requestCode")
        when (requestCode) {
            CALLBACK_REGISTER -> {
//                loadLocationInfo()
                persistentBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                binding.bottomSheetPersistent.title.text = ""
                binding.bottomSheetPersistent.description.text = ""
            }
        }
    }

    override fun onBackPressed() {
        exitDialog?.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.maps, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.register -> {
                val intent = Intent(this, RegisterActivity::class.java).apply {
                    putExtra(Const.CENTER_LATITUDE, map.cameraPosition.target.latitude)
                    putExtra(Const.CENTER_LONGITUDE, map.cameraPosition.target.longitude)
                }
                startActivityForResult(intent, CALLBACK_REGISTER)
            }
            // TOOD ?????? ?????? ??????
//            R.id.delete -> {
//                val auth = FirebaseAuth.getInstance()
//                val userRef = FirebaseDatabase.getInstance().reference.child("users")
//                userRef.orderByChild("email").equalTo(auth.currentUser?.email).ref.removeValue()
//                auth.currentUser?.delete()
//
//                val pref = applicationContext.getSharedPreferences(FOOD_TRUCK, Context.MODE_PRIVATE)
//                val editor = pref.edit()
//                editor.remove("key")
//                editor.commit()
//
//                Toast.makeText(
//                    this@MapsActivity, "?????? ?????????????????????.", Toast.LENGTH_SHORT
//                ).show()
//                finish()
//            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun initFirebase() {
        val auth = FirebaseAuth.getInstance()
        Log.d(TAG, "initFirebase currentUser: ${auth.currentUser}")
        loadUserInfo()
        loadLocationInfo()
    }

    private fun initView() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        persistentBottomSheetBehavior =
            BottomSheetBehavior.from(binding.bottomSheetPersistent.bottomSheetPersistent)
        persistentBottomSheetBehavior.run {
            setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(p0: View, state: Int) {
                    when (state) {
                        BottomSheetBehavior.STATE_EXPANDED -> {

                        }
                    }
                }

                override fun onSlide(p0: View, p1: Float) {
                }
            })
        }

        binding.bottomSheetPersistent.edit.setOnClickListener {
            if ((markerLatitude != 0.0 && markerLongitude != 0.0) && (markerLatitude != currentLatitude && markerLongitude != currentLongitude)) {
                val intent = Intent(this, RegisterActivity::class.java).apply {
                    putExtra(Const.CENTER_LATITUDE, markerLatitude)
                    putExtra(Const.CENTER_LONGITUDE, markerLongitude)
                    putExtra(Const.TITLE, binding.bottomSheetPersistent.title.text)
                    putExtra(Const.DESCRIPTION, binding.bottomSheetPersistent.description.text)
                    putExtra(
                        Const.MAP_KEY,
                        mapInfo[markerLatitude.toString() + markerLongitude.toString()]
                    )
                }
                startActivityForResult(intent, CALLBACK_REGISTER)
            } else {
                Toast.makeText(
                    this@MapsActivity,
                    "????????? ??????????????????.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        findViewById<ImageView>(R.id.my_location).run {
            setOnClickListener {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(currentLatitude, currentLongitude),
                        15f
                    )
                )
            }
        }

        exitDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.exit_dialog)
            findViewById<Button>(R.id.review)?.setOnClickListener {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=$packageName")
                    )
                )
            }
            findViewById<Button>(R.id.exit)?.setOnClickListener {
                finish()
            }
        }

        binding.bottomSheetPersistent.delete.setOnClickListener {
            showDialog()
        }
    }

    private fun showDialog() {
        val dialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(R.layout.custom_dialog)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            findViewById<AppCompatButton>(R.id.btn_detail_delete).setOnClickListener {
                requestDelete()
                dismiss()
            }
            findViewById<AppCompatButton>(R.id.btn_detail_delete_cancel).setOnClickListener { dismiss() }
        }
        dialog.show()
    }

    private fun initPermission() {
        RxPermissions(this).request(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).subscribe { granted ->
            Log.d(TAG, "grated: $granted")
            if (granted) {
                LocationHelper().startListeningUserLocation(
                    this,
                    object : LocationHelper.MyLocationListener {
                        override fun onLocationChanged(location: Location) {
                            if (isCallCurrentPosition) return
                            // Add a marker in Sydney and move the camera
                            Log.d(
                                TAG,
                                "onLocationChanged" + location.latitude + "," + location.longitude
                            )
                            val current = LatLng(location.latitude, location.longitude)
                            currentLatitude = location.latitude
                            currentLongitude = location.longitude
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 15f))
                            isCallCurrentPosition = true
                        }
                    })
            } else {
                requestPermissions()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(TAG, "onMapReady")
        map = googleMap.apply {
            setOnMarkerClickListener { marker ->
                if (!(marker.position.latitude == currentLatitude && marker.position.longitude == currentLongitude)) {
                    binding.bottomSheetPersistent.run {
                        title.text = marker?.title
                        description.text = marker?.snippet
                        marker?.run {
                            val updateDate =
                                SimpleDateFormat("yyyy-MM-dd").format(mapDate[position.latitude.toString() + position.longitude.toString()])
                            date.text = "??????????????????\n$updateDate"
                        }
                    }
                    persistentBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    binding.bottomSheetPersistent.adView.loadAd(AdRequest.Builder().build())
                    markerLatitude = marker?.position?.latitude ?: 0.0
                    markerLongitude = marker?.position?.longitude ?: 0.0
                    false
                }
                false
            }
        }
    }

    private fun loadUserInfo() {
        // TODO ????????? ?????? ????????? ????????? ????????????
    }

    private fun loadLocationInfo() {
        FirebaseDatabase.getInstance().reference.child("Location").apply {
            addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.i(TAG, "loadLocationInfo onDataChange count:${snapshot.children.count()}")
                    mapInfo.clear()
                    mapDate.clear()
                    map.clear()
                    map.addMarker(
                        MarkerOptions().position(
                            LatLng(
                                currentLatitude,
                                currentLongitude
                            )
                        ).title("?????? ??????")
                    )
                    for (location in snapshot.children) {
                        val info =
                            location.getValue(com.mungyu.foodtruck.model.Location::class.java)
                        mapInfo[info!!.latitude.toString() + info!!.longitude.toString()] =
                            location.key!!
                        mapDate[info!!.latitude.toString() + info!!.longitude.toString()] =
                            info.updateDate
                        map.addMarker(MarkerOptions().apply {
                            position(LatLng(info!!.latitude, info!!.longitude))
                            title(info!!.title)
                            snippet(info!!.description)
                            icon(BitmapDescriptorFactory.fromResource(R.drawable.location))
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
    }


    private fun requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this@MapsActivity,
                REQUIRED_PERMISSIONS[0]
            )
        ) {

            // 3-2. ????????? ???????????? ?????? ?????????????????? ???????????? ????????? ????????? ???????????? ????????? ????????????.
            Toast.makeText(
                this@MapsActivity,
                "??? ?????? ??????????????? ?????? ?????? ????????? ???????????????.",
                Toast.LENGTH_LONG
            )
                .show()
            // 3-3. ??????????????? ????????? ????????? ?????????. ?????? ????????? onRequestPermissionResult?????? ???????????????.
            ActivityCompat.requestPermissions(
                this@MapsActivity, REQUIRED_PERMISSIONS,
                PERMISSIONS_REQUEST_CODE
            )
        } else {
            // 4-1. ???????????? ????????? ????????? ??? ?????? ?????? ???????????? ????????? ????????? ?????? ?????????.
            // ?????? ????????? onRequestPermissionResult?????? ???????????????.
            ActivityCompat.requestPermissions(
                this@MapsActivity, REQUIRED_PERMISSIONS,
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    private fun initAdmob() {
        MobileAds.initialize(this) {}
        binding.bottomSheetPersistent.adView.loadAd(AdRequest.Builder().build())
        //Test
//        val adLoader = AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110")
        val adLoader = AdLoader.Builder(this, "ca-app-pub-8549606613390169/6916450490")
            .forNativeAd { ad ->
                exitDialog?.findViewById<TemplateView>(R.id.template)?.setNativeAd(ad)
            }
            .withAdListener(object : AdListener() {
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun requestDelete() {
        if ((markerLatitude != 0.0 && markerLongitude != 0.0) && (markerLatitude != currentLatitude && markerLongitude != currentLongitude)) {
            // ????????????. count 0 ?????? ????????????. firstRegisterKey??? ?????? key??? ????????????.
            // count??? 1?????? snapshot??? ???????????? ?????? ????????? count?????? ??????
            // count?????? ???????????? ?????? firstRegisterKey??? secondRegisterKey??? ??? ?????? ???????????? ????????? ????????? ????????????.dismiss
            // 2??? ?????? Location??? ????????? ?????? ???????????? RequestDelete?????? ????????????.
            // 1????????? count?????? 1 ??????????????? update??????. 1?????? secondRegisterKey??? ??????
            val pref =
                applicationContext.getSharedPreferences(FOOD_TRUCK, Context.MODE_PRIVATE)
            val deleteRef = FirebaseDatabase.getInstance().reference.child("RequestDelete")
            deleteRef.orderByChild("locationKey")
                .equalTo(mapInfo[markerLatitude.toString() + markerLongitude.toString()])
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        Log.i(
                            TAG,
                            "onDataChange RequestDelete count:${snapshot.children.count()}"
                        )
                        if (snapshot.children.count() == 0) {
                            val ref = deleteRef.push()
                            ref.setValue(
                                RequestDelete(
                                    count = 1,
                                    locationKey = mapInfo[markerLatitude.toString() + markerLongitude.toString()],
                                    firstRegisterKey = pref.getString("key", null)
                                )
                            )
                        } else {
                            for (info in snapshot.children) {
                                info.getValue(RequestDelete::class.java)?.run {
                                    val myKey = pref.getString("key", null)
                                    Log.i(
                                        TAG,
                                        "myKey: $myKey, firstRegisterKey:$firstRegisterKey, secondRegisterKey:$secondRegisterKey, count:$count"
                                    )
                                    if (firstRegisterKey == myKey || secondRegisterKey == myKey) {
                                        Toast.makeText(
                                            this@MapsActivity,
                                            "?????? ?????????????????????.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@run
                                    }
                                    if (count == 2) {
                                        deleteRef.orderByChild("locationKey")
                                            .equalTo(mapInfo[markerLatitude.toString() + markerLongitude.toString()]!!)
                                            .addValueEventListener(object : ValueEventListener {
                                                override fun onDataChange(snapshot: DataSnapshot) {
                                                    for (info in snapshot.children) {
                                                        deleteRef.child(info.key!!)
                                                            .removeValue()
                                                        persistentBottomSheetBehavior.state =
                                                            BottomSheetBehavior.STATE_COLLAPSED
                                                        binding.bottomSheetPersistent.title.text =
                                                            ""
                                                        binding.bottomSheetPersistent.description.text =
                                                            ""
                                                    }
                                                }

                                                override fun onCancelled(error: DatabaseError) {
                                                }
                                            })
                                        FirebaseDatabase.getInstance().reference.child("Location")
                                            .child(mapInfo[markerLatitude.toString() + markerLongitude.toString()]!!)
                                            .removeValue()
                                        Toast.makeText(
                                            this@MapsActivity,
                                            "????????? ?????????????????????.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else if (count <= 1) {
                                        count++
                                        secondRegisterKey = pref.getString("key", null)
                                        deleteRef.orderByChild("locationKey")
                                            .equalTo(mapInfo[markerLatitude.toString() + markerLongitude.toString()]!!)
                                            .addValueEventListener(object : ValueEventListener {
                                                override fun onDataChange(snapshot: DataSnapshot) {
                                                    for (info in snapshot.children) {
                                                        deleteRef.child(info.key!!)
                                                            .setValue(this@run)
                                                        Toast.makeText(
                                                            this@MapsActivity,
                                                            "?????? ?????????????????????.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }

                                                override fun onCancelled(error: DatabaseError) {
                                                }
                                            })

                                    }
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }

                })
        } else {
            Toast.makeText(
                this@MapsActivity,
                "????????? ??????????????????.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        const val TAG = "FoodTruckMaps"
        const val PERMISSIONS_REQUEST_CODE = 100
        val CALLBACK_REGISTER = 9002
        var REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}