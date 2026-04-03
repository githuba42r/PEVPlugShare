package com.a42r.pevplugshare.ui.suggestions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.data.api.ServerChangeSuggestion
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONObject

@AndroidEntryPoint
class IncomingSuggestionsActivity : AppCompatActivity() {

    private val viewModel: IncomingSuggestionsViewModel by viewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var loadingProgress: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var emptyText: TextView
    private lateinit var suggestionsList: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_suggestions)

        toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        setSupportActionBar(toolbar)

        loadingProgress = findViewById(R.id.loadingProgress)
        errorText = findViewById(R.id.errorText)
        emptyText = findViewById(R.id.emptyText)
        suggestionsList = findViewById(R.id.suggestionsList)
        suggestionsList.layoutManager = LinearLayoutManager(this)

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
            viewModel.suggestions.collect { suggestions ->
                emptyText.visibility = if (suggestions.isEmpty()) View.VISIBLE else View.GONE
                suggestionsList.adapter = SuggestionsAdapter(
                    suggestions,
                    onAccept = { suggestion -> showResponseDialog(suggestion, accept = true) },
                    onReject = { suggestion -> showResponseDialog(suggestion, accept = false) }
                )
            }
        }
    }

    private fun showResponseDialog(suggestion: ServerChangeSuggestion, accept: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_suggestion_response, null)
        val responseInput = dialogView.findViewById<TextInputEditText>(R.id.responseInput)

        AlertDialog.Builder(this)
            .setTitle(if (accept) "Accept Suggestion" else "Reject Suggestion")
            .setView(dialogView)
            .setPositiveButton(if (accept) "Accept" else "Reject") { _, _ ->
                val msg = responseInput.text?.toString()?.takeIf { it.isNotBlank() }
                if (accept) {
                    viewModel.accept(suggestion.id, msg)
                } else {
                    viewModel.reject(suggestion.id, msg)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun countChanges(changesJson: String): Int {
        return try {
            JSONObject(changesJson).length()
        } catch (e: Exception) {
            0
        }
    }

    inner class SuggestionsAdapter(
        private val items: List<ServerChangeSuggestion>,
        private val onAccept: (ServerChangeSuggestion) -> Unit,
        private val onReject: (ServerChangeSuggestion) -> Unit
    ) : RecyclerView.Adapter<SuggestionsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val suggestionTitle: TextView = view.findViewById(R.id.suggestionTitle)
            val suggestionDetails: TextView = view.findViewById(R.id.suggestionDetails)
            val suggestionMessage: TextView = view.findViewById(R.id.suggestionMessage)
            val acceptButton: MaterialButton = view.findViewById(R.id.acceptButton)
            val rejectButton: MaterialButton = view.findViewById(R.id.rejectButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_suggestion, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val changeCount = countChanges(item.changesJson)
            holder.suggestionTitle.text = "Location: ${item.locationId}"
            holder.suggestionDetails.text = "$changeCount field${if (changeCount != 1) "s" else ""} changed • by ${item.suggestedBy}"
            if (item.message != null) {
                holder.suggestionMessage.visibility = View.VISIBLE
                holder.suggestionMessage.text = "\"${item.message}\""
            } else {
                holder.suggestionMessage.visibility = View.GONE
            }
            if (item.status == "pending") {
                holder.acceptButton.visibility = View.VISIBLE
                holder.rejectButton.visibility = View.VISIBLE
                holder.acceptButton.setOnClickListener { onAccept(item) }
                holder.rejectButton.setOnClickListener { onReject(item) }
            } else {
                holder.acceptButton.visibility = View.GONE
                holder.rejectButton.visibility = View.GONE
            }
        }

        override fun getItemCount() = items.size
    }
}
