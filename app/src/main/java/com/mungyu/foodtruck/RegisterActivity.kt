package com.mungyu.foodtruck

import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.database.FirebaseDatabase
import com.mungyu.foodtruck.databinding.ActivityRegisterBinding
import com.mungyu.foodtruck.model.Location

class RegisterActivity : AppCompatActivity() {
    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var persistentBottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var locationManager: LocationManager
    private var isEdit = false
    private var key = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        initView()
        initPersistentBottomSheetBehavior()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.register, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.name_register -> {
                if (validCheckInfo()) {
                    val pref =
                        applicationContext.getSharedPreferences(FOOD_TRUCK, Context.MODE_PRIVATE)
                    if (isEdit) {
                        FirebaseDatabase.getInstance().reference.child("Location")
                            .child(key).setValue(
                                Location(
                                    latitude = map.cameraPosition.target.latitude,
                                    longitude = map.cameraPosition.target.longitude,
                                    title = binding.registerPersistent.title.text.toString(),
                                    description = binding.registerPersistent.description.text.toString(),
                                    registerKey = pref.getString("key", null),
                                    updateDate = System.currentTimeMillis()
                                )
                            )
                        Toast.makeText(this, "수정되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        val registerRef =
                            FirebaseDatabase.getInstance().reference.child("Location")
                                .push()
                        Log.i(TAG, "registerRef:$registerRef")
                        registerRef.setValue(
                            Location(
                                latitude = map.cameraPosition.target.latitude,
                                longitude = map.cameraPosition.target.longitude,
                                title = binding.registerPersistent.title.text.toString(),
                                description = binding.registerPersistent.description.text.toString(),
                                registerKey = pref.getString("key", null),
                                registerDate = System.currentTimeMillis(),
                                updateDate = System.currentTimeMillis()
                            )
                        )
                        Toast.makeText(this, "추가되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                } else {
                    Toast.makeText(this, "이름, 내용을 입력해주세요!!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun validCheckInfo(): Boolean =
        binding.registerPersistent.title.text.isNotEmpty() && binding.registerPersistent.description.text.isNotEmpty()


    private fun initView() {
        supportActionBar?.run {
            title = if (isEdit) {
                "지점 편집"
            } else {
                "지점 등록"
            }
            setDisplayHomeAsUpEnabled(true)
        }
        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).let {
            it.getMapAsync { googleMap ->
                map = googleMap
                val latitude = intent.getDoubleExtra(Const.CENTER_LATITUDE, 0.0)
                val longitude = intent.getDoubleExtra(Const.CENTER_LONGITUDE, 0.0)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 15f))
                intent.getStringExtra(Const.TITLE)?.run {
                    binding.registerPersistent.title.setText(this)
                    binding.registerPersistent.description.setText(intent.getStringExtra(Const.DESCRIPTION))
                    isEdit = true
                    key = intent.getStringExtra(Const.MAP_KEY)!!
                    binding.registerPersistent.thumbnail.setImageResource(R.drawable.foodtruck)
                }
            }
        }
    }

    private fun initPersistentBottomSheetBehavior() {
        persistentBottomSheetBehavior =
            BottomSheetBehavior.from(binding.registerPersistent.registerPersistent)
        persistentBottomSheetBehavior.run {
            state = BottomSheetBehavior.STATE_EXPANDED
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
    }

    companion object {
        const val TAG = "FoodTruckRegister"
    }
}