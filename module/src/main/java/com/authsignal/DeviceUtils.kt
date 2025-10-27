package com.authsignal

import android.os.Build

object DeviceUtils {
  fun getDeviceName(): String {
    val manufacturer = Build.MANUFACTURER
    val model = Build.MODEL

    if (model.startsWith(manufacturer)) {
      return model
    }

    return "$manufacturer $model"
  }
}