package com.a42r.pevplugshare.ui.wizard

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.a42r.pevplugshare.ui.wizard.steps.AiFeaturesFragment
import com.a42r.pevplugshare.ui.wizard.steps.CompletionFragment
import com.a42r.pevplugshare.ui.wizard.steps.ConnectAccountFragment
import com.a42r.pevplugshare.ui.wizard.steps.CreateFolderFragment
import com.a42r.pevplugshare.ui.wizard.steps.InstallOsmandFragment
import com.a42r.pevplugshare.ui.wizard.steps.LoginOsmandFragment
import com.a42r.pevplugshare.ui.wizard.steps.SyncCloudFragment
import com.a42r.pevplugshare.ui.wizard.steps.VerifyFolderFragment
import com.a42r.pevplugshare.ui.wizard.steps.WelcomeFragment

class WizardPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = SetupWizardViewModel.TOTAL_STEPS

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> WelcomeFragment()
            1 -> InstallOsmandFragment()
            2 -> LoginOsmandFragment()
            3 -> CreateFolderFragment()
            4 -> SyncCloudFragment()
            5 -> ConnectAccountFragment()
            6 -> VerifyFolderFragment()
            7 -> AiFeaturesFragment()
            8 -> CompletionFragment()
            else -> WelcomeFragment()
        }
    }
}
