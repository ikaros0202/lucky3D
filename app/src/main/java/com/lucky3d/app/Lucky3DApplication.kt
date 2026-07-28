package com.lucky3d.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.lucky3d.app.app.LifecycleSyncObserver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class Lucky3DApplication : Application() {
    @Inject
    lateinit var lifecycleSyncObserver: LifecycleSyncObserver

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleSyncObserver)
    }
}
