package com.ak.momapp

import android.app.Application
import com.ak.momapp.notify.BreakNotifier

class BrainBreakApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Channels must exist before the first alarm fires; creating them
        // repeatedly is a no-op.
        BreakNotifier.createChannels(this)
    }
}
