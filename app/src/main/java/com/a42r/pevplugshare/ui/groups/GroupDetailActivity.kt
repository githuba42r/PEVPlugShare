package com.a42r.pevplugshare.ui.groups

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.data.api.ServerGroupMember
import com.a42r.pevplugshare.data.remote.ServerApiClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GroupDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
    }

    private val viewModel: GroupDetailViewModel by viewModels()

    @Inject
    lateinit var serverApiClient: ServerApiClient

    private lateinit var toolbar: MaterialToolbar
    private lateinit var groupNameText: TextView
    private lateinit var groupDescriptionText: TextView
    private lateinit var errorText: TextView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var membersList: RecyclerView
    private lateinit var addMemberButton: MaterialButton
    private lateinit var generateInviteButton: MaterialButton
    private lateinit var deleteGroupButton: MaterialButton

    private var groupId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_detail)

        toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        setSupportActionBar(toolbar)

        groupNameText = findViewById(R.id.groupNameText)
        groupDescriptionText = findViewById(R.id.groupDescriptionText)
        errorText = findViewById(R.id.errorText)
        loadingProgress = findViewById(R.id.loadingProgress)
        membersList = findViewById(R.id.membersList)
        addMemberButton = findViewById(R.id.addMemberButton)
        generateInviteButton = findViewById(R.id.generateInviteButton)
        deleteGroupButton = findViewById(R.id.deleteGroupButton)

        membersList.layoutManager = LinearLayoutManager(this)

        addMemberButton.setOnClickListener {
            showAddMemberDialog()
        }

        generateInviteButton.setOnClickListener {
            groupId?.let { id -> viewModel.generateInvite(id) }
        }

        deleteGroupButton.setOnClickListener {
            groupId?.let { id -> confirmDeleteGroup(id) }
        }

        // Check for deep link invite token
        val uri = intent?.data
        if (uri != null && uri.scheme == "pevplugshare" && uri.host == "group") {
            val token = uri.lastPathSegment
            if (token != null) {
                acceptInviteFromDeepLink(token)
                return
            }
        }

        // Normal launch via intent extra
        groupId = intent?.getStringExtra(EXTRA_GROUP_ID)
        observeState()
        groupId?.let { viewModel.loadGroup(it) }
    }

    private fun acceptInviteFromDeepLink(token: String) {
        loadingProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val service = serverApiClient.getService()
                val acceptResp = service.acceptGroupInvite(token)
                if (acceptResp.isSuccessful) {
                    // Fetch invite details to get group ID
                    val detailsResp = service.getGroupInviteDetails(token)
                    if (detailsResp.isSuccessful) {
                        val gid = detailsResp.body()?.groupId
                        if (gid != null) {
                            groupId = gid
                            observeState()
                            viewModel.loadGroup(gid)
                        } else {
                            showError("Could not determine group from invite")
                        }
                    } else {
                        showError("Failed to load invite details: ${detailsResp.code()}")
                    }
                } else {
                    showError("Failed to accept invite: ${acceptResp.code()}")
                }
            } catch (e: Exception) {
                showError(e.message ?: "Failed to accept invite")
            } finally {
                loadingProgress.visibility = View.GONE
            }
        }
    }

    private fun showError(message: String) {
        errorText.visibility = View.VISIBLE
        errorText.text = message
    }

    private fun showAddMemberDialog() {
        val id = groupId ?: return
        val input = android.widget.EditText(this).apply {
            hint = "User ID or email"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        AlertDialog.Builder(this)
            .setTitle("Add Member")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val value = input.text?.toString()?.trim() ?: ""
                if (value.isNotEmpty()) {
                    viewModel.addMembers(id, listOf(value))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteGroup(id: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Group?")
            .setMessage("This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteGroup(id)
            }
            .setNegativeButton("Cancel", null)
            .show()
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
            viewModel.group.collect { group ->
                if (group != null) {
                    groupNameText.text = group.name
                    toolbar.title = group.name
                    if (!group.description.isNullOrBlank()) {
                        groupDescriptionText.visibility = View.VISIBLE
                        groupDescriptionText.text = group.description
                    } else {
                        groupDescriptionText.visibility = View.GONE
                    }
                    deleteGroupButton.visibility = View.VISIBLE
                    addMemberButton.visibility = View.VISIBLE
                }
            }
        }
        lifecycleScope.launch {
            viewModel.members.collect { members ->
                membersList.adapter = MemberAdapter(members) { userId ->
                    groupId?.let { gid -> viewModel.removeMember(gid, userId) }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.inviteUrl.collect { url ->
                if (url != null) {
                    showInviteDialog(url)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.deleted.collect { deleted ->
                if (deleted) {
                    finish()
                }
            }
        }
    }

    private fun showInviteDialog(url: String) {
        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
        AlertDialog.Builder(this)
            .setTitle("Invite Link")
            .setMessage(url)
            .setPositiveButton("Copy") { _, _ ->
                val clip = android.content.ClipData.newPlainText("Invite URL", url)
                clipboard.setPrimaryClip(clip)
            }
            .setNeutralButton("Share") { _, _ ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                startActivity(Intent.createChooser(shareIntent, "Share invite link"))
            }
            .setNegativeButton("Close", null)
            .show()
    }

    inner class MemberAdapter(
        private val items: List<ServerGroupMember>,
        private val onRemove: (String) -> Unit
    ) : RecyclerView.Adapter<MemberAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val userId: TextView = view.findViewById(R.id.memberUserId)
            val role: TextView = view.findViewById(R.id.memberRoleBadge)
            val removeButton: MaterialButton = view.findViewById(R.id.removeMemberButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_group_member, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.userId.text = item.userId
            holder.role.text = item.role.replaceFirstChar { it.uppercase() }
            if (item.role != "owner") {
                holder.removeButton.visibility = View.VISIBLE
                holder.removeButton.setOnClickListener { onRemove(item.userId) }
            } else {
                holder.removeButton.visibility = View.GONE
            }
        }

        override fun getItemCount() = items.size
    }
}
