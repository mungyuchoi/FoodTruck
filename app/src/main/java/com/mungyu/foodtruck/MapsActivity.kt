package com.mungyu.foodtruck

import android.Manifest
import android.content.Intent
import android.graphics.*
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
import com.tbruyelle.rxpermissions3.RxPermissions


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var isCallCurrentPosition = false
    private lateinit var persistentBottomSheetBehavior: BottomSheetBehavior<*>

    private val mapInfo = mutableMapOf<String, String>()
    private var markerLatitude = 0.0
    private var markerLongitude = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
        initFirebase()
        initPermission()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CALLBACK_REGISTER -> {
                persistentBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                binding.bottomSheetPersistent.title.text = ""
                binding.bottomSheetPersistent.description.text = ""
            }
        }
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

        binding.bottomSheetPersistent.edit.setOnClickListener{
            if (markerLatitude != 0.0 && markerLongitude != 0.0) {
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
                    "권한이 없습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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
                            map.addMarker(MarkerOptions().position(current).title("현재 위치"))
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
                binding.bottomSheetPersistent.run {
                    title.text = marker?.title
                    description.text = marker?.snippet
                }
                persistentBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                // TODO binding.adview load 해야합니다.
                markerLatitude = marker?.position?.latitude ?: 0.0
                markerLongitude = marker?.position?.longitude ?: 0.0
                false
            }
        }
    }

    private fun loadUserInfo() {
        // TODO 사용할 일이 있으면 불러서 사용하자
    }

    private fun loadLocationInfo() {
        FirebaseDatabase.getInstance().reference.child("Location").apply {
            addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.i(TAG, "onDataChange")
                    mapInfo.clear()
                    for (location in snapshot.children) {
                        val info =
                            location.getValue(com.mungyu.foodtruck.model.Location::class.java)
                        mapInfo[info!!.latitude.toString() + info!!.longitude.toString()] = location.key!!
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

            // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
            Toast.makeText(
                this@MapsActivity,
                "이 앱을 실행하려면 위치 접근 권한이 필요합니다.",
                Toast.LENGTH_LONG
            )
                .show()
            // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
            ActivityCompat.requestPermissions(
                this@MapsActivity, REQUIRED_PERMISSIONS,
                PERMISSIONS_REQUEST_CODE
            )
        } else {
            // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
            // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
            ActivityCompat.requestPermissions(
                this@MapsActivity, REQUIRED_PERMISSIONS,
                PERMISSIONS_REQUEST_CODE
            )
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