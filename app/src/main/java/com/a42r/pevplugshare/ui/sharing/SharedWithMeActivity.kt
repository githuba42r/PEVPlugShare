package com.a42r.pevplugshare.ui.sharing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.data.api.ServerLocation
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SharedWithMeActivity : AppCompatActivity() {

    private val viewModel: SharedWithMeViewModel by viewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var loadingProgress: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var emptyText: TextView
    private lateinit var locationsList: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shared_with_me)

        toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        setSupportActionBar(toolbar)

        loadingProgress = findViewById(R.id.loadingProgress)
        errorText = findViewById(R.id.errorText)
        emptyText = findViewById(R.id.emptyText)
        locationsList = findViewById(R.id.locationsList)
        locationsList.layoutManager = LinearLayoutManager(this)

        observeState()
        viewModel.load()
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.loading.collect { loading ->
                loadingProgress.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }
        lifecycleScope.launch {
            viewModel.error.collect { error ->
                if (error != null) {
                    errorText.visibility = View.VISIBLE
                    errorText.text = error
                } else {
                    errorText.visibility = View.GONE
                }
            }
        }
        lifecycleScope.launch {
            viewModel.locations.collect { locations ->
                emptyText.visibility = if (locations.isEmpty()) View.VISIBLE else View.GONE
                locationsList.adapter = SharedLocationsAdapter(
                    locations,
                    onView = { location ->
                        Toast.makeText(this@SharedWithMeActivity, "Location: ${location.name}", Toast.LENGTH_SHORT).show()
                    },
                    onHide = { location ->
                        viewModel.hideShare(location.id)
                    }
                )
            }
        }
    }

    inner class SharedLocationsAdapter(
        private val items: List<ServerLocation>,
        private val onView: (ServerLocation) -> Unit,
        private val onHide: (ServerLocation) -> Unit
    ) : RecyclerView.Adapter<SharedLocationsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val locationName: TextView = view.findViewById(R.id.locationName)
            val locationInfo: TextView = view.findViewById(R.id.locationInfo)
            val viewButton: MaterialButton = view.findViewById(R.id.viewButton)
            val hideButton: MaterialButton = view.findViewById(R.id.hideButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_shared_location, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.locationName.text = item.name
            holder.locationInfo.text = item.description ?: item.streetAddress ?: ""
            holder.viewButton.setOnClickListener { onView(item) }
            holder.hideButton.setOnClickListener { onHide(item) }
        }

        override fun getItemCount() = items.size
    }
}
