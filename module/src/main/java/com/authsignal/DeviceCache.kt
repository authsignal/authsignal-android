package com.authsignal

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DeviceCache private constructor() {
  private var context: Context? = null
  private var _deviceId: String? = null

  fun initialize(context: Context, deviceId: String? = null) {
    this.context = context.applicationContext
    this._deviceId = deviceId
  }

  suspend fun getDefaultDeviceId(): String {
    if (_deviceId != null) {
      return _deviceId!!
    }

    val store = context?.dataStore ?: return ""

    val defaultDeviceId = store.data
      .map { preferences -> preferences[defaultDeviceIdPreferencesKey] }
      .first()

    return defaultDeviceId ?: UUID.randomUUID().toString().also { newId ->
      store.edit { preferences ->
        preferences[defaultDeviceIdPreferencesKey] = newId
      }
    }
  }

  companion object {
    val shared: DeviceCache by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { DeviceCache() }
    internal val defaultDeviceIdPreferencesKey = stringPreferencesKey("@as_device_id")
  }
}
