package com.raccoongang.newedx

import android.app.Application
import com.google.firebase.FirebaseOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.raccoongang.core.BuildConfig
import com.raccoongang.newedx.di.appModule
import com.raccoongang.newedx.di.networkingModule
import com.raccoongang.newedx.di.screenModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class NewEdXApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@NewEdXApp)
            modules(
                appModule,
                networkingModule,
                screenModule
            )
        }

        if (BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
            val options = FirebaseOptions.Builder()
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setApplicationId(getString(com.raccoongang.core.R.string.google_app_id))
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setGcmSenderId(BuildConfig.FIREBASE_GCM_SENDER_ID)
                .build()
            Firebase.initialize(this, options)
        }
    }

}