package org.openedx.core.data.model

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

data class CourseEnrollments(
    @SerializedName("enrollments")
    val enrollments: DashboardCourseList,

    @SerializedName("config")
    val configs: AppConfig,
) {
    class Deserializer : JsonDeserializer<CourseEnrollments> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): CourseEnrollments {
            val enrollments = deserializeEnrollments(json)
            val appConfig = deserializeAppConfig(json)

            return CourseEnrollments(enrollments, appConfig)
        }

        private fun deserializeEnrollments(json: JsonElement?): DashboardCourseList {
            return try {
                Gson().fromJson(
                    (json as JsonObject).get("enrollments"),
                    DashboardCourseList::class.java
                )
            } catch (ex: Exception) {
                DashboardCourseList(
                    next = null,
                    previous = null,
                    count = 0,
                    numPages = 0,
                    currentPage = 0,
                    results = listOf()
                )
            }
        }

        /**
         * To remove dependency on the backend, all the data related to Remote Config
         * will be received under the `configs` key. The `config` is the key under
         * 'configs` which defines the data that is related to the configuration of the
         * app.
         */
        private fun deserializeAppConfig(json: JsonElement?): AppConfig {
            return try {
                val config = (json as JsonObject)
                    .getAsJsonObject("configs")
                    .getAsJsonPrimitive("config")

                Gson().fromJson(
                    config.asString,
                    AppConfig::class.java
                )
            } catch (ex: Exception) {
                AppConfig()
            }
        }
    }
}
