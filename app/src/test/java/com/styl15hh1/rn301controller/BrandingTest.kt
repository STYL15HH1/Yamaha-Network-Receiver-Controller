package com.styl15hh1.rn301controller

import android.content.res.Configuration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class BrandingTest {
    @Test fun applicationAndAboutNameUseNewBrandInEverySupportedLocale() {
        val app=RuntimeEnvironment.getApplication()
        val info=app.packageManager.getApplicationInfo(BuildConfig.APPLICATION_ID,0)
        assertEquals(R.string.app_name,info.labelRes)
        for(tag in listOf("en","es","de","fr","it","pl","ko","ja")) {
            val config=Configuration(app.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
            val context=app.createConfigurationContext(config)
            assertEquals(tag,"Yamaha Receiver Controller",context.getString(info.labelRes))
            assertTrue(context.getString(R.string.unofficial).contains("Yamaha Corporation"))
        }
    }
    @Test fun rebrandingPreservesReleaseIdentityAndEnglishDisclaimer() {
        assertEquals("com.styl15hh1.rn301controller",BuildConfig.APPLICATION_ID)
        assertEquals("1.1.0",BuildConfig.VERSION_NAME)
        assertEquals(16,BuildConfig.VERSION_CODE)
        val app=RuntimeEnvironment.getApplication()
        val config=Configuration(app.resources.configuration).apply { setLocale(Locale.ENGLISH) }
        assertEquals("Independent open-source project. Not affiliated with or endorsed by Yamaha Corporation.",
            app.createConfigurationContext(config).getString(R.string.unofficial))
    }
}
