package org.openedx.core.presentation

interface IAPAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
}

enum class IAPAnalyticsEvent(val eventName: String, val biValue: String) {
    // In App Purchases Events
    IAP_UPGRADE_NOW_CLICKED(
        "Payments: Upgrade Now Clicked",
        "edx.bi.app.payments.upgrade_now.clicked"
    ),
    IAP_COURSE_UPGRADE_SUCCESS(
        "Payments: Course Upgrade Success",
        "edx.bi.app.payments.course_upgrade_success"
    ),
    IAP_PAYMENT_ERROR("Payments: Payment Error", "edx.bi.app.payments.payment_error"),
    IAP_PAYMENT_CANCELED("Payments: Canceled by User", "edx.bi.app.payments.canceled_by_user"),
    IAP_COURSE_UPGRADE_ERROR(
        "Payments: Course Upgrade Error",
        "edx.bi.app.payments.course_upgrade_error"
    ),
    IAP_PRICE_LOAD_ERROR("Payments: Price Load Error", "edx.bi.app.payments.price_load_error"),
    IAP_ERROR_ALERT_ACTION(
        "Payments: Error Alert Action",
        "edx.bi.app.payments.error_alert_action"
    ),
    IAP_UNFULFILLED_PURCHASE_INITIATED(
        "Payments: Unfulfilled Purchase Initiated",
        "edx.bi.app.payments.unfulfilled_purchase.initiated"
    ),
    IAP_RESTORE_PURCHASE_CLICKED(
        "Payments: Restore Purchases Clicked",
        "edx.bi.app.payments.restore_purchases.clicked"
    ),

    // Value Prop Events
    VALUE_PROP_LEARN_MORE_CLICKED(
        "Value Prop Learn More Clicked",
        "edx.bi.app.value.prop.learn.more.clicked"
    ),
    VALUE_PROP_MODAL_VIEW("Value Prop Modal View", ""),
    VALUE_PROP_MESSAGE_VIEWED("Value Prop Message Viewed", "edx.bi.app.value_prop_message.viewed"),
    COURSE_UNIT_LOCKED_CONTENT(
        "Value Prop Locked Content Clicked",
        "edx.bi.app.course.unit.locked.content.clicked"
    ),
    VALUE_PROP_SHOW_MORE_CLICKED(
        "Value Prop Show More Clicked",
        "edx.bi.app.value_prop.show_more.clicked"
    ),
    VALUE_PROP_SHOW_LESS_CLICKED(
        "Value Prop Show Less Clicked",
        "edx.bi.app.value_prop.show_less.clicked"
    )
}

enum class IAPAnalyticsKeys(val key: String) {
    NAME("name"),
    CATEGORY("category"),
    IN_APP_PURCHASES("in_app_purchases"),
    COURSE_ID("course_id"),
    PACING("pacing"),
    SELF("self"),
    INSTRUCTOR("instructor"),
    IAP_FLOW_TYPE("flow_type"),
    PRICE("price"),
    COMPONENT_ID("component_id"),
    ELAPSED_TIME("elapsed_time"),
    ERROR("error"),
    ERROR_ACTION("error_action"),
    ACTION("action"),
    SCREEN_NAME("screen_name"),
    GET_HELP("get_help"),
}

enum class IAPAnalyticsScreen(val screenName: String) {
    COURSE_ENROLLMENT("course_enrollment"),
    COURSE_DASHBOARD("course_dashboard"),
    COURSE_COMPONENT("course_component")
}
