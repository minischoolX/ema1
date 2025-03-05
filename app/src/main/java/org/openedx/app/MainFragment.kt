package org.openedx.app

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.app.databinding.FragmentMainBinding
import org.openedx.app.deeplink.HomeTab
import org.openedx.core.adapter.NavigationFragmentAdapter
import org.openedx.core.presentation.global.appupgrade.UpgradeRequiredFragment
import org.openedx.core.presentation.global.viewBinding
import org.openedx.discovery.presentation.DiscoveryRouter
import org.openedx.downloads.presentation.download.DownloadsFragment
import org.openedx.learn.presentation.LearnFragment
import org.openedx.learn.presentation.LearnTab
import org.openedx.profile.presentation.profile.ProfileFragment

class MainFragment : Fragment(R.layout.fragment_main) {

    private val binding by viewBinding(FragmentMainBinding::bind)
    private val viewModel by viewModel<MainViewModel>()
    private val router by inject<DiscoveryRouter>()

    private lateinit var adapter: NavigationFragmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        setFragmentResultListener(UpgradeRequiredFragment.REQUEST_KEY) { _, _ ->
            binding.bottomNavView.selectedItemId = R.id.fragmentProfile
            viewModel.enableBottomBar(false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireArguments().apply {
            getString(ARG_COURSE_ID).takeIf { it.isNullOrBlank().not() }?.let { courseId ->
                val infoType = getString(ARG_INFO_TYPE)
                if (viewModel.isDiscoveryTypeWebView && infoType != null) {
                    router.navigateToCourseInfo(parentFragmentManager, courseId, infoType)
                } else {
                    router.navigateToCourseDetail(parentFragmentManager, courseId)
                }
                putString(ARG_COURSE_ID, "")
                putString(ARG_INFO_TYPE, "")
            }
        }

        val openTabArg = requireArguments().getString(ARG_OPEN_TAB, HomeTab.LEARN.name)
        val learnFragment = LearnFragment.newInstance(
            openTab = if (openTabArg == HomeTab.PROGRAMS.name) {
                LearnTab.PROGRAMS.name
            } else {
                LearnTab.COURSES.name
            }
        )
        val tabList = mutableListOf<Pair<Int, Fragment>>().apply {
            add(R.id.fragmentLearn to learnFragment)
            add(R.id.fragmentDiscover to viewModel.getDiscoveryFragment)
            if (viewModel.isDownloadsFragmentEnabled) {
                add(R.id.fragmentDownloads to DownloadsFragment())
            }
            add(R.id.fragmentProfile to ProfileFragment())
        }

        val menu = binding.bottomNavView.menu
        menu.clear()
        val tabTitles = mapOf(
            R.id.fragmentLearn to resources.getString(R.string.app_navigation_learn),
            R.id.fragmentDiscover to resources.getString(R.string.app_navigation_discovery),
            R.id.fragmentDownloads to resources.getString(R.string.app_navigation_downloads),
            R.id.fragmentProfile to resources.getString(R.string.app_navigation_profile),
        )
        val tabIcons = mapOf(
            R.id.fragmentLearn to R.drawable.app_ic_rows,
            R.id.fragmentDiscover to R.drawable.app_ic_home,
            R.id.fragmentDownloads to R.drawable.app_ic_download_cloud,
            R.id.fragmentProfile to R.drawable.app_ic_profile
        )
        for ((id, _) in tabList) {
            val menuItem = menu.add(Menu.NONE, id, Menu.NONE, tabTitles[id] ?: "")
            tabIcons[id]?.let { menuItem.setIcon(it) }
        }

        initViewPager(tabList)

        val menuIdToIndex = tabList.mapIndexed { index, pair -> pair.first to index }.toMap()

        binding.bottomNavView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.fragmentLearn -> viewModel.logLearnTabClickedEvent()
                R.id.fragmentDiscover -> viewModel.logDiscoveryTabClickedEvent()
                R.id.fragmentDownloads -> viewModel.logDownloadsTabClickedEvent()
                R.id.fragmentProfile -> viewModel.logProfileTabClickedEvent()
            }
            menuIdToIndex[menuItem.itemId]?.let { index ->
                binding.viewPager.setCurrentItem(index, false)
            }
            true
        }

        viewModel.isBottomBarEnabled.observe(viewLifecycleOwner) { isBottomBarEnabled ->
            enableBottomBar(isBottomBarEnabled)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.navigateToDiscovery.collect { shouldNavigateToDiscovery ->
                if (shouldNavigateToDiscovery) {
                    binding.bottomNavView.selectedItemId = R.id.fragmentDiscover
                }
            }
        }

        val initialMenuId = when (openTabArg) {
            HomeTab.LEARN.name, HomeTab.PROGRAMS.name -> R.id.fragmentLearn
            HomeTab.DISCOVER.name -> R.id.fragmentDiscover
            HomeTab.DOWNLOADS.name -> if (viewModel.isDownloadsFragmentEnabled) R.id.fragmentDownloads else R.id.fragmentLearn
            HomeTab.PROFILE.name -> R.id.fragmentProfile
            else -> R.id.fragmentLearn
        }
        binding.bottomNavView.selectedItemId = initialMenuId

        requireArguments().remove(ARG_OPEN_TAB)
    }

    @Suppress("MagicNumber")
    private fun initViewPager(tabList: List<Pair<Int, Fragment>>) {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.offscreenPageLimit = tabList.size

        adapter = NavigationFragmentAdapter(this).apply {
            tabList.forEach { (_, fragment) ->
                addFragment(fragment)
            }
        }
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
    }

    private fun enableBottomBar(enable: Boolean) {
        binding.bottomNavView.menu.forEach {
            it.isEnabled = enable
        }
    }

    companion object {
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_INFO_TYPE = "info_type"
        private const val ARG_OPEN_TAB = "open_tab"
        fun newInstance(
            courseId: String? = null,
            infoType: String? = null,
            openTab: String = HomeTab.LEARN.name
        ): MainFragment {
            val fragment = MainFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_INFO_TYPE to infoType,
                ARG_OPEN_TAB to openTab
            )
            return fragment
        }
    }
}
