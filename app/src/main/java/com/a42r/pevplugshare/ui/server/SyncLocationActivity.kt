package com.a42r.pevplugshare.ui.server

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.data.db.entity.LocationEntity
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SyncLocationActivity : AppCompatActivity() {

    private val viewModel: SyncLocationViewModel by viewModels()

    private lateinit var selectAllCheckbox: CheckBox
    private lateinit var locationList: RecyclerView
    private lateinit var statusText: TextView
    private lateinit var uploadButton: MaterialButton
    private lateinit var uploadProgress: ProgressBar

    private val adapter = SyncLocationAdapter { updateStatusText() }
    private var locations: List<LocationEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync_location)

        bindViews()
        observeViewModel()
    }

    private fun bindViews() {
        selectAllCheckbox = findViewById(R.id.selectAllCheckbox)
        locationList = findViewById(R.id.locationList)
        statusText = findViewById(R.id.statusText)
        uploadButton = findViewById(R.id.uploadButton)
        uploadProgress = findViewById(R.id.uploadProgress)

        locationList.layoutManager = LinearLayoutManager(this)
        locationList.adapter = adapter

        selectAllCheckbox.setOnCheckedChangeListener { _, isChecked ->
            adapter.setAllSelected(isChecked)
            updateStatusText()
        }

        uploadButton.setOnClickListener {
            val selected = adapter.getSelectedIds()
            if (selected.isEmpty()) {
                Toast.makeText(this, "No locations selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.uploadSelected(selected)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.locations.collect { list ->
                locations = list
                adapter.submitList(list)
                updateStatusText()
            }
        }

        lifecycleScope.launch {
            viewModel.uploadState.collect { state ->
                when (state) {
                    is SyncLocationViewModel.UploadState.Idle -> {
                        uploadProgress.visibility = View.GONE
                        uploadButton.isEnabled = true
                    }
                    is SyncLocationViewModel.UploadState.Uploading -> {
                        uploadProgress.visibility = View.VISIBLE
                        uploadButton.isEnabled = false
                        statusText.text = "Uploading ${state.current} of ${state.total}..."
                    }
                    is SyncLocationViewModel.UploadState.Done -> {
                        uploadProgress.visibility = View.GONE
                        uploadButton.isEnabled = true
                        Toast.makeText(
                            this@SyncLocationActivity,
                            "Upload complete: ${state.succeeded} succeeded, ${state.failed} failed",
                            Toast.LENGTH_LONG
                        ).show()
                        updateStatusText()
                    }
                    is SyncLocationViewModel.UploadState.Error -> {
                        uploadProgress.visibility = View.GONE
                        uploadButton.isEnabled = true
                        Toast.makeText(this@SyncLocationActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun updateStatusText() {
        val count = adapter.getSelectedIds().size
        statusText.text = "$count selected"
    }

    // Adapter
    inner class SyncLocationAdapter(
        private val onSelectionChanged: () -> Unit
    ) : RecyclerView.Adapter<SyncLocationAdapter.ViewHolder>() {

        private var items: List<LocationEntity> = emptyList()
        private val selected = mutableSetOf<String>()

        fun submitList(list: List<LocationEntity>) {
            items = list
            notifyDataSetChanged()
        }

        fun setAllSelected(select: Boolean) {
            selected.clear()
            if (select) selected.addAll(items.map { it.id })
            notifyDataSetChanged()
        }

        fun getSelectedIds(): Set<String> = selected.toSet()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_sync_location, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val checkbox: CheckBox = view.findViewById(R.id.checkbox)
            private val locationName: TextView = view.findViewById(R.id.locationName)
            private val locationDetail: TextView = view.findViewById(R.id.locationDetail)
            private val syncStatus: TextView = view.findViewById(R.id.syncStatus)

            fun bind(entity: LocationEntity) {
                locationName.text = entity.name
                locationDetail.text = if (entity.streetAddress != null) {
                    entity.streetAddress
                } else {
                    "%.4f, %.4f".format(entity.latitude, entity.longitude)
                }

                checkbox.isChecked = entity.id in selected
                checkbox.setOnCheckedChangeListener(null)
                checkbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selected.add(entity.id) else selected.remove(entity.id)
                    onSelectionChanged()
                }

                itemView.setOnClickListener {
                    checkbox.isChecked = !checkbox.isChecked
                }

                when (entity.serverSyncStatus) {
                    "SYNCED" -> {
                        syncStatus.text = "SYNCED"
                        syncStatus.setTextColor(Color.parseColor("#4CAF50"))
                    }
                    "MODIFIED" -> {
                        syncStatus.text = "MODIFIED"
                        syncStatus.setTextColor(Color.parseColor("#FF9800"))
                    }
                    else -> {
                        syncStatus.text = "LOCAL"
                        syncStatus.setTextColor(Color.GRAY)
                    }
                }
            }
        }
    }
}
