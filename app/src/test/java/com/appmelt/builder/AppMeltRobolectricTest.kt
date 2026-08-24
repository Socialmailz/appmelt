package com.appmelt.builder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.appmelt.builder.analyzer.ProjectAnalyzer
import com.appmelt.builder.errortranslator.ErrorTranslator
import com.appmelt.builder.model.ErrorActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppMeltRobolectricTest {

    @Test
    fun `verify app name resource is AppMelt`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AppMelt", appName)
    }

    @Test
    fun `verify ErrorTranslator maps missing SDK correctly`() {
        val rawLog = "Failed to find target with hash string 'android-36' in: /sdk"
        val error = ErrorTranslator.translate(rawLog)
        assertNotNull(error)
        assertEquals("Android SDK 36 Missing", error.title)
        assertEquals(ErrorActionType.INSTALL_SDK, error.actionType)
    }

    @Test
    fun `verify ErrorTranslator maps JDK version mismatch`() {
        val rawLog = "Unsupported class file major version 65"
        val error = ErrorTranslator.translate(rawLog)
        assertNotNull(error)
        assertTrue(error.title.contains("JDK Version") || error.title.contains("Java"))
    }
}
