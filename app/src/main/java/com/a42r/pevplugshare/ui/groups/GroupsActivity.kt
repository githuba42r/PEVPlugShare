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
import com.a42r.pevplugshare.data.api.ServerFriendGroup
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GroupsActivity : AppCompatActivity() {

    private val viewModel: GroupsViewModel by viewModels()

    private lateinit var errorText: TextView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var groupsList: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groups)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        setSupportActionBar(toolbar)

        errorText = findViewById(R.id.errorText)
        loadingProgress = findViewById(R.id.loadingProgress)
        groupsList = findViewById(R.id.groupsList)
        groupsList.layoutManager = LinearLayoutManager(this)

        findViewById<MaterialButton>(R.id.createGroupButton).setOnClickListener {
            showCreateGroupDialog()
        }

        observeState()
        viewModel.loadGroups()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadGroups()
    }

    private fun showCreateGroupDialog() {
        val dialogView = layoutInflater.inflate(android.R.layout.activity_list_item, null)
        // Build dialog manually with EditTexts
        val dialogLayout = layoutInflater.inflate(
            android.R.layout.two_line_list_item, null
        )

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Create Group")

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
        }

        val nameInput = com.google.android.material.textfield.TextInputEditText(this).apply {
            hint = "Group name"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        val nameLayout = com.google.android.material.textfield.TextInputLayout(this).apply {
            hint = "Group name"
            addView(nameInput)
        }

        val descInput = com.google.android.material.textfield.TextInputEditText(this).apply {
            hint = "Description (optional)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        val descLayout = com.google.android.material.textfield.TextInputLayout(this).apply {
            hint = "Description (optional)"
            addView(descInput)
        }

        container.addView(nameLayout)
        container.addView(descLayout)
        builder.setView(container)

        builder.setPositiveButton("Create") { _, _ ->
            val name = nameInput.text?.toString()?.trim() ?: ""
            val desc = descInput.text?.toString()?.trim()
            if (name.isNotEmpty()) {
                viewModel.createGroup(name, desc)
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
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
            viewModel.groups.collect { groups ->
                groupsList.adapter = GroupAdapter(groups) { group ->
                    val intent = Intent(this@GroupsActivity, GroupDetailActivity::class.java)
                    intent.putExtra(GroupDetailActivity.EXTRA_GROUP_ID, group.id)
                    startActivity(intent)
                }
            }
        }
    }

    inner class GroupAdapter(
        private val items: List<ServerFriendGroup>,
        private val onClick: (ServerFriendGroup) -> Unit
    ) : RecyclerView.Adapter<GroupAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.groupName)
            val description: TextView = view.findViewById(R.id.groupDescription)
            val badge: TextView = view.findViewById(R.id.memberCountBadge)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_group, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.name.text = item.name
            if (!item.description.isNullOrBlank()) {
                holder.description.visibility = View.VISIBLE
                holder.description.text = item.description
            } else {
                holder.description.visibility = View.GONE
            }
            holder.badge.text = ""
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size
    }
}
