package org.openedx.discovery.presentation

import androidx.fragment.app.FragmentManager

interface DiscoveryRouter {

    fun navigateToCourseDetail(fm: FragmentManager, courseId: String)

    fun navigateToCourseSearch(fm: FragmentManager, querySearch: String)

    fun navigateToUpgradeRequired(fm: FragmentManager)

    fun navigateToCourseInfo(fm: FragmentManager, courseId: String, infoType: String)
}
