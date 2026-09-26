package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.formatBytes
import com.example.data.model.formatSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MediaFetch Pro", appName)
  }

  @Test
  fun `test byte and speed formatting`() {
    val sizeText = formatBytes(10485760L) // 10 MB
    assertTrue(sizeText.contains("10"))

    val speedText = formatSpeed(2097152L) // 2 MB/s
    assertTrue(speedText.contains("2"))
  }
}

