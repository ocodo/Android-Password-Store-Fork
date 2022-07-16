/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.passwordstore.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import app.passwordstore.ui.crypto.BasePgpActivity
import app.passwordstore.ui.crypto.DecryptActivity
import app.passwordstore.ui.passwords.PasswordStore
import app.passwordstore.util.auth.BiometricAuthenticator
import app.passwordstore.util.auth.BiometricAuthenticator.Result
import app.passwordstore.util.extensions.sharedPrefs
import app.passwordstore.util.features.Features
import app.passwordstore.util.settings.PreferenceKeys
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LaunchActivity : AppCompatActivity() {

  @Inject lateinit var features: Features

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val prefs = sharedPrefs
    if (prefs.getBoolean(PreferenceKeys.BIOMETRIC_AUTH, false)) {
      BiometricAuthenticator.authenticate(this) { result ->
        when (result) {
          is Result.Success -> {
            startTargetActivity(false)
          }
          is Result.HardwareUnavailableOrDisabled -> {
            prefs.edit { remove(PreferenceKeys.BIOMETRIC_AUTH) }
            startTargetActivity(false)
          }
          is Result.Failure,
          Result.Cancelled -> {
            finish()
          }
          is Result.Retry -> {}
        }
      }
    } else {
      startTargetActivity(true)
    }
  }

  private fun getDecryptIntent(): Intent {
    return Intent(this, DecryptActivity::class.java)
  }

  private fun startTargetActivity(noAuth: Boolean) {
    val intentToStart =
      if (intent.action == ACTION_DECRYPT_PASS)
        getDecryptIntent().apply {
          putExtra(
            BasePgpActivity.EXTRA_FILE_PATH,
            intent.getStringExtra(BasePgpActivity.EXTRA_FILE_PATH)
          )
          putExtra(
            BasePgpActivity.EXTRA_REPO_PATH,
            intent.getStringExtra(BasePgpActivity.EXTRA_REPO_PATH)
          )
        }
      else Intent(this, PasswordStore::class.java)
    startActivity(intentToStart)

    Handler(Looper.getMainLooper()).postDelayed({ finish() }, if (noAuth) 0L else 500L)
  }

  companion object {

    const val ACTION_DECRYPT_PASS = "DECRYPT_PASS"
  }
}