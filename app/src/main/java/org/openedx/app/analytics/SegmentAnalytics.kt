package org.openedx.app.analytics

import android.content.Context
import com.segment.analytics.kotlin.destinations.braze.BrazeDestination
import com.segment.analytics.kotlin.destinations.firebase.FirebaseDestination
import org.openedx.app.BuildConfig
import org.openedx.core.config.Config
import org.openedx.core.utils.Logger
import com.segment.analytics.kotlin.android.Analytics as SegmentAnalyticsBuilder
import com.segment.analytics.kotlin.core.Analytics as SegmentTracker

class SegmentAnalytics(context: Context, config: Config) : Analytics {

    private val logger = Logger(TAG)
    private var tracker: SegmentTracker

    init {
        // Create an analytics client with the given application context and Segment write key.
        tracker = SegmentAnalyticsBuilder(config.getSegmentConfig().segmentWriteKey, context) {
            // Automatically track Lifecycle events
            trackApplicationLifecycleEvents = true
            flushAt = 20
            flushInterval = 30
        }
        if (config.getFirebaseConfig().isSegmentAnalyticsSource()) {
            tracker.add(plugin = FirebaseDestination(context = context))
        }

        if (config.getFirebaseConfig()
                .isSegmentAnalyticsSource() && config.getBrazeConfig().isEnabled
        ) {
            tracker.add(plugin = BrazeDestination(context))
        }
        SegmentTracker.debugLogsEnabled = BuildConfig.DEBUG
        logger.d { "Segment Analytics Builder Initialised" }
    }

    override fun logScreenEvent(screenName: String, params: Map<String, Any?>) {
        logger.d { "Segment Analytics log Screen Event: $screenName + $params" }
        tracker.screen(screenName, params)
    }

    override fun logEvent(eventName: String, params: Map<String, Any?>) {
        logger.d { "Segment Analytics log Event $eventName: $params" }
        tracker.track(eventName, params)
    }

    override fun logUserId(userId: Long) {
        logger.d { "Segment Analytics User Id log Event: $userId" }
        tracker.identify(userId.toString())
    }

    private companion object {
        const val TAG = "SegmentAnalytics"
    }
}
