/*
 * This file is part of Adblock Plus <https://adblockplus.org/>,
 * Copyright (C) 2006-present eyeo GmbH
 *
 * Adblock Plus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * Adblock Plus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Adblock Plus.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.adblockplus.adblockplussbrowser.preferences.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.adblockplus.adblockplussbrowser.base.databinding.DataBindingFragment
import org.adblockplus.adblockplussbrowser.preferences.R
import org.adblockplus.adblockplussbrowser.preferences.databinding.FragmentMainPreferencesBinding

@AndroidEntryPoint
internal class MainPreferencesFragment :
    DataBindingFragment<FragmentMainPreferencesBinding>(R.layout.fragment_main_preferences) {

    private val viewModel: MainPreferencesViewModel by viewModels()

    override fun onBindView(binding: FragmentMainPreferencesBinding) {
        binding.viewModel = viewModel
        val supportActionBar = (activity as AppCompatActivity).supportActionBar
        supportActionBar?.subtitle = getString(R.string.app_subtitle)

        binding.mainPreferencesAdBlockingInclude.mainPreferencesPrimarySubscriptions.setOnClickListener {
            supportActionBar?.subtitle = null
            val direction = MainPreferencesFragmentDirections
                .actionMainPreferencesFragmentToPrimarySubscriptionsFragment()
            findNavController().navigate(direction)
        }
        binding.mainPreferencesAdBlockingInclude.mainPreferencesOtherSubscriptions.setOnClickListener {
            supportActionBar?.subtitle = null
            val direction = MainPreferencesFragmentDirections
                .actionMainPreferencesFragmentToOtherSubscriptionsFragment()
            findNavController().navigate(direction)
        }
        binding.mainPreferencesAdBlockingInclude.mainPreferencesAllowlist.setOnClickListener {
            supportActionBar?.subtitle = null
            val direction = MainPreferencesFragmentDirections
                .actionMainPreferencesFragmentToAllowlistFragment()
            findNavController().navigate(direction)
        }
        binding.mainPreferencesAdBlockingInclude.mainPreferencesUpdateSubscriptions.setOnClickListener {
            supportActionBar?.subtitle = null
            val direction = MainPreferencesFragmentDirections
                .actionMainPreferencesFragmentToUpdateSubscriptionsFragment()
            findNavController().navigate(direction)
        }
        binding.mainPreferencesLanguagesOnboardingInclude.mainPreferencesLanguagesOnboardingOptionAdd.setOnClickListener {
            supportActionBar?.subtitle = null
            viewModel.markLanguagesOnboardingComplete(true)
            val direction = MainPreferencesFragmentDirections
                .actionMainPreferencesFragmentToPrimarySubscriptionsFragment()
            findNavController().navigate(direction)
        }
        binding.mainPreferencesLanguagesOnboardingInclude.mainPreferencesLanguagesOnboardingOptionSkip.setOnClickListener {
            viewModel.markLanguagesOnboardingComplete(false)
        }
        binding.mainPreferencesAcceptableAdsInclude.mainPreferencesAcceptableAds.setOnClickListener {
            supportActionBar?.subtitle = null
            val direction = MainPreferencesFragmentDirections
                .actionMainPreferencesFragmentToAcceptableAdsFragment()
            findNavController().navigate(direction)
        }
        binding.mainPreferencesAboutInclude.mainPreferencesAbout.setOnClickListener {
            supportActionBar?.subtitle = null
            val direction = MainPreferencesFragmentDirections
                .actionMainPreferencesFragmentToAboutFragment()
            findNavController().navigate(direction)
        }
    }
}