package de.klophil.pinlockcomposedemo

import android.app.Application
import de.klophil.pin_lock_compose.PinManager

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // initialize pin manager in application
        PinManager.initialize(this)
    }
}