package com.a42r.pevplugshare.ui.debug

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.databinding.FragmentBackupRestoreBinding
import com.a42r.pevplugshare.viewmodel.BackupRestoreViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

/**
 * Dialog fragment for restoring backups in debug builds.
 * Only available when BuildConfig.DEBUG is true.
 */
@AndroidEntryPoint
class BackupRestoreDialogFragment : DialogFragment() {

    private var _binding: FragmentBackupRestoreBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BackupRestoreViewModel
    private var selectedFile: File? = null
    private var progressDialog: AlertDialog? = null
    private var progressDialogView: View? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            handleFileSelected(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupRestoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel with proper parent
        viewModel = ViewModelProvider(requireActivity()).get(BackupRestoreViewModel::class.java)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        binding.selectFileButton.setOnClickListener {
            filePickerLauncher.launch(arrayOf("application/zip"))
        }

        binding.restoreButton.setOnClickListener {
            selectedFile?.let {
                showRestoreConfirmation(it)
            }
        }

        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }

    private fun setupObservers() {
        viewModel.restoreState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BackupRestoreViewModel.RestoreState.Idle -> {
                    binding.statusText.text = "Ready to restore backup"
                    binding.statusText.setTextColor(
                        requireContext().getColor(R.color.md_theme_secondary)
                    )
                    binding.progressBar.visibility = View.GONE
                    binding.selectFileButton.isEnabled = true
                    binding.restoreButton.isEnabled = false
                }

                is BackupRestoreViewModel.RestoreState.SelectingFile -> {
                    binding.statusText.text = "Select a backup file..."
                    binding.progressBar.visibility = View.GONE
                    binding.selectFileButton.isEnabled = true
                    binding.restoreButton.isEnabled = false
                }

                is BackupRestoreViewModel.RestoreState.FileSelected -> {
                    hideProgressDialog()
                    binding.statusText.text = "File: ${state.fileName}"
                    binding.statusText.setTextColor(
                        requireContext().getColor(R.color.md_theme_secondary)
                    )
                    binding.progressBar.visibility = View.GONE
                    binding.selectFileButton.isEnabled = true
                    binding.restoreButton.isEnabled = true
                }

                is BackupRestoreViewModel.RestoreState.Validating -> {
                    updateProgressDialog("Validating backup...", 10)
                    binding.statusText.text = "Validating backup..."
                    binding.progressBar.visibility = View.VISIBLE
                    binding.selectFileButton.isEnabled = false
                    binding.restoreButton.isEnabled = false
                }

                is BackupRestoreViewModel.RestoreState.Restoring -> {
                    updateProgressDialog(state.message, state.progress)
                    binding.statusText.text = state.message
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressBar.progress = state.progress
                    binding.selectFileButton.isEnabled = false
                    binding.restoreButton.isEnabled = false
                }

                is BackupRestoreViewModel.RestoreState.Success -> {
                    hideProgressDialog()
                    binding.statusText.text = "✓ Backup restored successfully!"
                    binding.statusText.setTextColor(
                        requireContext().getColor(android.R.color.holo_green_dark)
                    )
                    binding.progressBar.visibility = View.GONE
                    binding.selectFileButton.isEnabled = true
                    binding.restoreButton.isEnabled = false

                    showSuccessDialog()
                }

                is BackupRestoreViewModel.RestoreState.Error -> {
                    hideProgressDialog()
                    binding.statusText.text = "✗ Error: ${state.message}"
                    binding.statusText.setTextColor(
                        requireContext().getColor(android.R.color.holo_red_dark)
                    )
                    binding.progressBar.visibility = View.GONE
                    binding.selectFileButton.isEnabled = true
                    binding.restoreButton.isEnabled = false
                }
            }
        }

        viewModel.backupInfo.observe(viewLifecycleOwner) { info ->
            if (info != null) {
                binding.infoText.text = """
                    Database: ${if (info.hasDatabase) "✓ Present" else "✗ Missing"}
                    Images: ${info.imageCount} files
                    Size: ${formatFileSize(info.fileSize)}
                """.trimIndent()
                binding.infoText.visibility = View.VISIBLE
            } else {
                binding.infoText.visibility = View.GONE
            }
        }
    }

    private fun handleFileSelected(uri: Uri) {
        try {
            showProgressDialog("Loading backup file...")

            val file = File(uri.path ?: return)
            if (file.exists() && file.canRead()) {
                selectedFile = file
                viewModel.onFileSelected(file)
            } else {
                // Try to copy to cache if direct access fails
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val cacheFile = File(requireContext().cacheDir, "backup.zip")
                    cacheFile.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                    selectedFile = cacheFile
                    viewModel.onFileSelected(cacheFile)
                }
            }
        } catch (e: Exception) {
            hideProgressDialog()
            Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showProgressDialog(message: String) {
        hideProgressDialog()

        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_progress, null)
        progressDialogView = view

        val messageText = view.findViewById<TextView>(R.id.progressMessage)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val percentText = view.findViewById<TextView>(R.id.progressPercent)

        messageText.text = message
        progressBar.progress = 0
        percentText.text = "0%"

        progressDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .setCancelable(false)
            .show()
    }

    private fun updateProgressDialog(message: String, progress: Int) {
        progressDialogView?.let { view ->
            val messageText = view.findViewById<TextView>(R.id.progressMessage)
            val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
            val percentText = view.findViewById<TextView>(R.id.progressPercent)

            messageText?.text = message
            progressBar?.progress = progress
            percentText?.text = "$progress%"
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
        progressDialogView = null
    }

    private fun showRestoreConfirmation(file: File) {
        val info = viewModel.backupInfo.value
        val message = buildString {
            append("Restore from: ${file.name}?\n\n")
            if (info != null) {
                append("Database: ${if (info.hasDatabase) "✓" else "✗"}\n")
                append("Images: ${info.imageCount} files\n\n")
            }
            append("This will replace your current data.")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Restore Backup")
            .setMessage(message)
            .setPositiveButton("Restore") { _, _ ->
                selectedFile?.let {
                    viewModel.restoreBackup(it)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun showSuccessDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Restore Complete")
            .setMessage("✓ Backup restored successfully!\n\n" +
                "The app will now close and restart to load the restored data.\n\n" +
                "You will see:\n" +
                "1. App will close (screen goes black)\n" +
                "2. App will reopen automatically\n" +
                "3. Login screen appears\n" +
                "4. Your 35 locations and photos will be available")
            .setPositiveButton("Close and Restart App") { _, _ ->
                restartApp()
            }
            .setCancelable(false)
            .show()
    }

    private fun restartApp() {
        // Kill app process to force full reload (including database)
        try {
            val packageName = requireContext().packageName
            val intent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                requireContext().startActivity(intent)
            }

            // Give it a moment to start, then kill this process
            Thread.sleep(500)
        } catch (e: Exception) {
            // If restart fails, still try to kill
        }

        // Force kill the app so it fully reloads
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
