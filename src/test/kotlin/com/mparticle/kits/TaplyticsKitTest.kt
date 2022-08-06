package com.mparticle.kits

import android.content.Context
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class KitTests {
    private val kit: KitIntegration
         get() = TaplyticsKit()

    @Before
    fun testDefaultStaticFields() {
        //make sure we are not carrying over any static settings in between tests
        Assert.assertFalse(TaplyticsKit.started)
        Assert.assertFalse(TaplyticsKit.delayInitializationUntilSessionStart)
    }

    @Test
    @Throws(Exception::class)
    fun testGetName() {
        val name = kit.name
        Assert.assertTrue(!name.isNullOrEmpty())
    }

    /**
     * Kit *should* throw an exception when they're initialized with the wrong settings.
     *
     */
    @Test
    @Throws(Exception::class)
    fun testOnKitCreate() {
        var e: Exception? = null
        try {
            val kit = kit
            val settings = HashMap<String, String>()
            settings["fake setting"] = "fake"
            kit.onKitCreate(settings, Mockito.mock(Context::class.java))
        } catch (ex: Exception) {
            e = ex
        }
        Assert.assertNotNull(e)
        reset()
    }

    @Test
    @Throws(Exception::class)
    fun testClassName() {
        val factory = KitIntegrationFactory()
        val integrations = factory.knownIntegrations
        val className = kit.javaClass.name
        for (integration in integrations) {
            if (integration.value == className) {
                return
            }
        }
        Assert.fail("$className not found as a known integration.")
        reset()
    }

    @Test
    fun testInitializationNoSessionStartDelayFlag() {
        val taplyticsKit = MockTaplyticsKit()

        //starts without SessionStart delay flag
        taplyticsKit.onKitCreate(
            emptyMap(), Mockito.mock(
                Context::class.java
            )
        )
        Assert.assertTrue(taplyticsKit.startCalled)
        Assert.assertFalse(TaplyticsKit.delayInitializationUntilSessionStart)
        Assert.assertTrue(TaplyticsKit.started)

        //make sure there are no duplicate starts
        taplyticsKit.startCalled = false
        taplyticsKit.onKitCreate(
            emptyMap(), Mockito.mock(
                Context::class.java
            )
        )
        Assert.assertFalse(taplyticsKit.startCalled)
        Assert.assertTrue(TaplyticsKit.started)

        //alse make sure there is no duplicate start in onSessionStart
        taplyticsKit.startCalled = false
        taplyticsKit.onSessionStart()
        Assert.assertFalse(taplyticsKit.startCalled)
        Assert.assertTrue(TaplyticsKit.started)
        reset()
    }

    @Test
    fun testInitializationWithSessionStartDelayFlag() {
        TaplyticsKit.started = false
        TaplyticsKit.delayInitializationUntilSessionStart = false

        //doesn't start in onCreate with SessionStart delay flag
        val taplyticsKit = MockTaplyticsKit()
        TaplyticsKit.delayInitializationUntilSessionStart = true
        taplyticsKit.onKitCreate(
            emptyMap(), Mockito.mock(
                Context::class.java
            )
        )
        Assert.assertFalse(taplyticsKit.startCalled)
        Assert.assertTrue(TaplyticsKit.delayInitializationUntilSessionStart)
        Assert.assertFalse(TaplyticsKit.started)

        //does start in onSessionStart with SessionStart delay flag
        taplyticsKit.onSessionStart()
        Assert.assertTrue(taplyticsKit.startCalled)
        Assert.assertTrue(TaplyticsKit.delayInitializationUntilSessionStart)
        Assert.assertTrue(TaplyticsKit.started)

        //make sure there are no duplicate starts
        taplyticsKit.startCalled = false
        taplyticsKit.onSessionStart()
        Assert.assertFalse(taplyticsKit.startCalled)
        Assert.assertTrue(TaplyticsKit.started)

        //als0 make sure there is no duplicate start in onKitCreate (situation shouldn't ever happen anyway)
        taplyticsKit.startCalled = false
        taplyticsKit.onKitCreate(
            emptyMap(), Mockito.mock(
                Context::class.java
            )
        )
        Assert.assertFalse(taplyticsKit.startCalled)
        Assert.assertTrue(TaplyticsKit.started)
        reset()
    }

    private fun reset() {
        TaplyticsKit.started = false
        TaplyticsKit.delayInitializationUntilSessionStart = false
    }

    internal inner class MockTaplyticsKit : TaplyticsKit() {
        var startCalled = false
        override fun startTaplytics(settings: Map<String, String>, context: Context?) {
            startCalled = true
        }

        init {
            val mockConfiguration = Mockito.mock(
                KitConfiguration::class.java
            )
            val mockKitManager = Mockito.mock(KitManagerImpl::class.java)
            Mockito.`when`(mockKitManager.context).thenReturn(
                Mockito.mock(
                    Context::class.java
                )
            )
            Mockito.`when`(mockConfiguration.settings).thenReturn(emptyMap())
            kitManager = mockKitManager
            configuration = mockConfiguration
        }
    }
}
