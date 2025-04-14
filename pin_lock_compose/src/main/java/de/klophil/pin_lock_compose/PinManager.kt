package de.klophil.pin_lock_compose

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

object PinManager {

    private const val FILE_NAME = "pin_lock_preferences"

    private var dataStore: DataStore<PinPreferences>? = null

    /**
     * Returns encrypted DataStore.
     *
     * @param context
     * Application context is preferred.
     *
     * @return EncryptedSharedPreferences.
     */
    private fun initializePreferences(application: Application): DataStore<PinPreferences> {
        return DataStoreFactory.create(
            serializer = PinPreferencesSerializer,
            produceFile = { application.preferencesDataStoreFile(FILE_NAME) }
        )
    }

    /**
     * Returns non null DataStore.
     *
     * @param context
     * Context.
     *
     * @return non null SharedPreferences.
     *
     * @throws IllegalStateException
     * If SharedPreferences is not initialized yet.
     */
    private fun getPreferences(): DataStore<PinPreferences> {
        return dataStore ?: throw IllegalStateException("Do you forget to call initialize() first?")
    }

    /**
     * Joins list of int to String.
     *
     * @param pin
     * List of numbers.
     *
     * @return joined pin of string.
     *
     * @throws IllegalStateException
     * If list size does not match required pin length.
     */
    private fun fromIntList(pin: List<Int>): String {
        if (pin.size != PinConst.PIN_LENGTH) throw IllegalStateException("Pin size does not match length. Actual: ${pin.size}. Expected: ${PinConst.PIN_LENGTH}")
        return pin.joinToString { it.toString() }
    }

    ///////////////////////////////////////////////////////////////////////////
    // INTERNAL API
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Saves the pin in encrypted DataStore.
     *
     * @param pin
     * List of pin numbers.
     */
    internal suspend fun savePin(pin: List<Int>) {
        getPreferences().updateData {
            PinPreferences(
                pin = fromIntList(pin)
            )
        }
    }

    /**
     * Checks the passed pin with saved pin.
     *
     * @return true if passed pin matches exactly as saved pin, false if pin does not match saved pin.
     *
     * @param pin
     * List of pin numbers.
     */
    internal suspend fun checkPin(pin: List<Int>): Boolean {
        val savedPin = getPreferences().data.map { it.component1() }.firstOrNull() ?: return false
        return savedPin == fromIntList(pin)
    }

    ///////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Initializes the pin lock. Prefer calling this function inside Application class.
     *
     * @param application
     * Need application context to initialize.
     */
    @Synchronized
    fun initialize(application: Application) {
        if (dataStore == null) dataStore = initializePreferences(application)
    }

    /**
     * Checks if there is already saved pin.
     *
     * @return true if there is already saved pin, false if there is no saved pin.
     */
    fun pinExists(): Flow<Boolean> = getPreferences().data.map { !it.component1().isNullOrEmpty() }

    /**
     * Clears the saved pin. By calling this function, you can clear the saved pin so that user can create a new pin without remembering
     * the saved pin.
     */
    suspend fun clearPin() {
        getPreferences().updateData {
            PinPreferences(
                pin = null
            )
        }
    }
}