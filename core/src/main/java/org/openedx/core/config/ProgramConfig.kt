package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class ProgramConfig(
    @SerializedName("TYPE")
    private val viewType: String = Config.ViewType.NATIVE.name,
    @SerializedName("WEBVIEW")
    val webViewConfig: ProgramWebViewConfig = ProgramWebViewConfig(),
){
    fun isViewTypeWebView(): Boolean {
        return Config.ViewType.WEBVIEW.name.equals(viewType, ignoreCase = true)
    }
}

data class ProgramWebViewConfig(
    @SerializedName("PROGRAM_URL")
    val programUrl: String = "",
    @SerializedName("PROGRAM_DETAIL_URL_TEMPLATE")
    val programDetailUrlTemplate: String = "",
)
