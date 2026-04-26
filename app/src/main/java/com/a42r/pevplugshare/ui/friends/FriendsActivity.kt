package com.a42r.pevplugshare.ui.friends

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
import com.a42r.pevplugshare.data.api.ServerFriendship
import com.a42r.pevplugshare.data.api.ServerUser
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FriendsActivity : AppCompatActivity() {

    private val viewModel: FriendsViewModel by viewModels()

    private lateinit var emailLayout: TextInputLayout
    private lateinit var emailInput: TextInputEditText
    private lateinit var addFriendButton: MaterialButton
    private lateinit var errorText: TextView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var friendsList: RecyclerView
    private lateinit var incomingRequestsList: RecyclerView
    private lateinit var outgoingRequestsList: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        setSupportActionBar(toolbar)

        emailLayout = findViewById(R.id.emailLayout)
        emailInput = findViewById(R.id.emailInput)
        addFriendButton = findViewById(R.id.addFriendButton)
        errorText = findViewById(R.id.errorText)
        loadingProgress = findViewById(R.id.loadingProgress)
        friendsList = findViewById(R.id.friendsList)
        incomingRequestsList = findViewById(R.id.incomingRequestsList)
        outgoingRequestsList = findViewById(R.id.outgoingRequestsList)

        friendsList.layoutManager = LinearLayoutManager(this)
        incomingRequestsList.layoutManager = LinearLayoutManager(this)
        outgoingRequestsList.layoutManager = LinearLayoutManager(this)

        addFriendButton.setOnClickListener {
            val email = emailInput.text?.toString()?.trim() ?: ""
            if (email.isEmpty()) {
                emailLayout.error = "Email is required"
                return@setOnClickListener
            }
            emailLayout.error = null
            viewModel.sendRequest(email)
        }

        observeState()
        viewModel.loadAll()
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
            viewModel.friends.collect { friends ->
                friendsList.adapter = FriendAdapter(friends) { userId ->
                    viewModel.removeFriend(userId)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.incomingRequests.collect { requests ->
                incomingRequestsList.adapter = FriendRequestAdapter(
                    requests,
                    isIncoming = true,
                    onAccept = { id -> viewModel.acceptRequest(id) },
                    onDecline = { id -> viewModel.declineRequest(id) }
                )
            }
        }
        lifecycleScope.launch {
            viewModel.outgoingRequests.collect { requests ->
                outgoingRequestsList.adapter = FriendRequestAdapter(
                    requests,
                    isIncoming = false,
                    onAccept = {},
                    onDecline = {}
                )
            }
        }
    }

    // Inline adapter for friends list
    inner class FriendAdapter(
        private val items: List<ServerUser>,
        private val onRemove: (String) -> Unit
    ) : RecyclerView.Adapter<FriendAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val displayName: TextView = view.findViewById(R.id.friendDisplayName)
            val email: TextView = view.findViewById(R.id.friendEmail)
            val removeButton: MaterialButton = view.findViewById(R.id.removeFriendButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_friend, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.displayName.text = item.displayName
            holder.email.text = item.email
            holder.removeButton.setOnClickListener { onRemove(item.id) }
        }

        override fun getItemCount() = items.size
    }

    // Inline adapter for friend requests (incoming and outgoing)
    inner class FriendRequestAdapter(
        private val items: List<ServerFriendship>,
        private val isIncoming: Boolean,
        private val onAccept: (String) -> Unit,
        private val onDecline: (String) -> Unit
    ) : RecyclerView.Adapter<FriendRequestAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val userInfo: TextView = view.findViewById(R.id.requestUserInfo)
            val acceptButton: MaterialButton = view.findViewById(R.id.acceptButton)
            val declineButton: MaterialButton = view.findViewById(R.id.declineButton)
            val pendingLabel: TextView = view.findViewById(R.id.pendingLabel)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_friend_request, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            if (isIncoming) {
                holder.userInfo.text = "From: ${item.requesterId}"
                holder.acceptButton.visibility = View.VISIBLE
                holder.declineButton.visibility = View.VISIBLE
                holder.pendingLabel.visibility = View.GONE
                holder.acceptButton.setOnClickListener { onAccept(item.id) }
                holder.declineButton.setOnClickListener { onDecline(item.id) }
            } else {
                holder.userInfo.text = "To: ${item.addresseeId}"
                holder.acceptButton.visibility = View.GONE
                holder.declineButton.visibility = View.GONE
                holder.pendingLabel.visibility = View.VISIBLE
            }
        }

        override fun getItemCount() = items.size
    }
}
