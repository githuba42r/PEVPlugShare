package com.a42r.pevplugshare.ui.sharing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.data.api.ServerFriendGroup
import com.a42r.pevplugshare.data.api.ServerLocationShare
import com.a42r.pevplugshare.data.api.ServerUser
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShareLocationActivity : AppCompatActivity() {

    private val viewModel: ShareLocationViewModel by viewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var loadingProgress: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var friendSearch: AutoCompleteTextView
    private lateinit var shareWithFriendButton: MaterialButton
    private lateinit var groupSpinner: Spinner
    private lateinit var shareWithGroupButton: MaterialButton
    private lateinit var generateDirectLinkButton: MaterialButton
    private lateinit var currentSharesList: RecyclerView

    private var locationId: String = ""
    private var friendsList: List<ServerUser> = emptyList()
    private var groupsList: List<ServerFriendGroup> = emptyList()

    companion object {
        const val EXTRA_LOCATION_ID = "locationId"

        fun start(context: Context, locationId: String) {
            val intent = Intent(context, ShareLocationActivity::class.java)
            intent.putExtra(EXTRA_LOCATION_ID, locationId)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_location)

        locationId = intent.getStringExtra(EXTRA_LOCATION_ID) ?: ""
        if (locationId.isEmpty()) {
            Toast.makeText(this, "No location specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        setSupportActionBar(toolbar)

        loadingProgress = findViewById(R.id.loadingProgress)
        errorText = findViewById(R.id.errorText)
        friendSearch = findViewById(R.id.friendSearch)
        shareWithFriendButton = findViewById(R.id.shareWithFriendButton)
        groupSpinner = findViewById(R.id.groupSpinner)
        shareWithGroupButton = findViewById(R.id.shareWithGroupButton)
        generateDirectLinkButton = findViewById(R.id.generateDirectLinkButton)
        currentSharesList = findViewById(R.id.currentSharesList)

        currentSharesList.layoutManager = LinearLayoutManager(this)

        shareWithFriendButton.setOnClickListener {
            val friendName = friendSearch.text?.toString()?.trim() ?: ""
            val friend = friendsList.find { it.displayName == friendName || it.email == friendName }
            if (friend != null) {
                viewModel.shareWithUser(locationId, friend.id)
            } else {
                Toast.makeText(this, "Please select a friend from the list", Toast.LENGTH_SHORT).show()
            }
        }

        shareWithGroupButton.setOnClickListener {
            val position = groupSpinner.selectedItemPosition
            if (position >= 0 && position < groupsList.size) {
                viewModel.shareWithGroup(locationId, groupsList[position].id)
            } else {
                Toast.makeText(this, "Please select a group", Toast.LENGTH_SHORT).show()
            }
        }

        generateDirectLinkButton.setOnClickListener {
            viewModel.generateDirectLink(locationId, singleUse = false)
        }

        observeState()
        viewModel.load(locationId)
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
            viewModel.success.collect { msg ->
                if (msg != null) {
                    Toast.makeText(this@ShareLocationActivity, msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearSuccess()
                }
            }
        }
        lifecycleScope.launch {
            viewModel.friends.collect { friends ->
                friendsList = friends
                val names = friends.map { it.displayName }
                val adapter = ArrayAdapter(this@ShareLocationActivity, android.R.layout.simple_dropdown_item_1line, names)
                friendSearch.setAdapter(adapter)
            }
        }
        lifecycleScope.launch {
            viewModel.groups.collect { groups ->
                groupsList = groups
                val names = groups.map { it.name }
                val adapter = ArrayAdapter(this@ShareLocationActivity, android.R.layout.simple_spinner_item, names)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                groupSpinner.adapter = adapter
            }
        }
        lifecycleScope.launch {
            viewModel.currentShares.collect { shares ->
                currentSharesList.adapter = SharesAdapter(shares) { share ->
                    viewModel.revokeShare(locationId, share.id)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.directShareResult.collect { result ->
                if (result != null) {
                    showDirectShareDialog(result.url)
                    viewModel.clearDirectShareResult()
                }
            }
        }
    }

    private fun showDirectShareDialog(url: String) {
        AlertDialog.Builder(this)
            .setTitle("Direct Share Link")
            .setMessage(url)
            .setPositiveButton("Copy") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Share Link", url))
                Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Share") { _, _ ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                startActivity(Intent.createChooser(intent, "Share location link"))
            }
            .setNegativeButton("Close", null)
            .show()
    }

    inner class SharesAdapter(
        private val items: List<ServerLocationShare>,
        private val onRevoke: (ServerLocationShare) -> Unit
    ) : RecyclerView.Adapter<SharesAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val shareInfo: TextView = view.findViewById(R.id.shareInfo)
            val revokeButton: MaterialButton = view.findViewById(R.id.revokeButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_location_share, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val target = when {
                item.sharedWithUser != null -> "User: ${item.sharedWithUser}"
                item.sharedWithGroup != null -> "Group: ${item.sharedWithGroup}"
                else -> "Unknown"
            }
            holder.shareInfo.text = target
            holder.revokeButton.setOnClickListener { onRevoke(item) }
        }

        override fun getItemCount() = items.size
    }
}
