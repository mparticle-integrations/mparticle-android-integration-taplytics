package com.mparticle.kits;


import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.mparticle.internal.KitManager;

public class KitTests {

    private KitIntegration getKit() {
        return new TaplyticsKit();
    }

    @Before
    public void testDefaultStaticFields() {
        //make sure we are not carrying over any static settings in between tests
        assertFalse(TaplyticsKit.started);
        assertFalse(TaplyticsKit.delayInitializationUntilSessionStart);
    }

    @Test
    public void testGetName() throws Exception {
        String name = getKit().getName();
        assertTrue(name != null && name.length() > 0);
    }

    /**
     * Kit *should* throw an exception when they're initialized with the wrong settings.
     *
     */
    @Test
    public void testOnKitCreate() throws Exception{
        Exception e = null;
        try {
            KitIntegration kit = getKit();
            Map settings = new HashMap<>();
            settings.put("fake setting", "fake");
            kit.onKitCreate(settings, Mockito.mock(Context.class));
        }catch (Exception ex) {
            e = ex;
        }
        assertNotNull(e);
        reset();
    }

    @Test
    public void testClassName() throws Exception {
        KitIntegrationFactory factory = new KitIntegrationFactory();
        Map<Integer, String> integrations = factory.getKnownIntegrations();
        String className = getKit().getClass().getName();
        for (Map.Entry<Integer, String> entry : integrations.entrySet()) {
            if (entry.getValue().equals(className)) {
                return;
            }
        }
        fail(className + " not found as a known integration.");
        reset();
    }

    @Test
    public void testInitializationNoSessionStartDelayFlag() {
        MockTaplyticsKit taplyticsKit = new MockTaplyticsKit();

        //starts without SessionStart delay flag
        taplyticsKit.onKitCreate(Collections.emptyMap(), Mockito.mock(Context.class));
        assertTrue(taplyticsKit.startCalled);
        assertFalse(TaplyticsKit.delayInitializationUntilSessionStart);
        assertTrue(TaplyticsKit.started);

        //make sure there are no duplicate starts
        taplyticsKit.startCalled = false;
        taplyticsKit.onKitCreate(Collections.emptyMap(), Mockito.mock(Context.class));
        assertFalse(taplyticsKit.startCalled);
        assertTrue(TaplyticsKit.started);

        //alse make sure there is no duplicate start in onSessionStart
        taplyticsKit.startCalled = false;
        taplyticsKit.onSessionStart();
        assertFalse(taplyticsKit.startCalled);
        assertTrue(TaplyticsKit.started);

        reset();
    }

    @Test
    public void testInitializationWithSessionStartDelayFlag() {
        MockTaplyticsKit taplyticsKit = new MockTaplyticsKit();
        TaplyticsKit.started = false;
        TaplyticsKit.delayInitializationUntilSessionStart = false;

        //doesn't start in onCreate with SessionStart delay flag
        taplyticsKit = new MockTaplyticsKit();
        MockTaplyticsKit.delayInitializationUntilSessionStart = true;
        taplyticsKit.onKitCreate(Collections.emptyMap(), Mockito.mock(Context.class));
        assertFalse(taplyticsKit.startCalled);
        assertTrue(TaplyticsKit.delayInitializationUntilSessionStart);
        assertFalse(TaplyticsKit.started);

        //does start in onSessionStart with SessionStart delay flag
        taplyticsKit.onSessionStart();

        assertTrue(taplyticsKit.startCalled);
        assertTrue(TaplyticsKit.delayInitializationUntilSessionStart);
        assertTrue(TaplyticsKit.started);

        //make sure there are no duplicate starts
        taplyticsKit.startCalled = false;
        taplyticsKit.onSessionStart();
        assertFalse(taplyticsKit.startCalled);
        assertTrue(TaplyticsKit.started);

        //als0 make sure there is no duplicate start in onKitCreate (situation shouldn't ever happen anyway)
        taplyticsKit.startCalled = false;
        taplyticsKit.onKitCreate(Collections.emptyMap(), Mockito.mock(Context.class));
        assertFalse(taplyticsKit.startCalled);
        assertTrue(TaplyticsKit.started);

        reset();
    }

    private void reset() {
        TaplyticsKit.started = false;
        TaplyticsKit.delayInitializationUntilSessionStart = false;
    }

    class MockTaplyticsKit extends TaplyticsKit {
        boolean startCalled = false;

        public MockTaplyticsKit() {
            KitConfiguration mockConfiguration = Mockito.mock(KitConfiguration.class);
            KitManagerImpl mockKitManager = Mockito.mock(KitManagerImpl.class);
            Mockito.when(mockKitManager.getContext()).thenReturn(Mockito.mock(Context.class));
            Mockito.when(mockConfiguration.getSettings()).thenReturn(Collections.emptyMap());
            setKitManager(mockKitManager);
            setConfiguration(mockConfiguration);
        }

        @Override
        protected void startTaplytics(Map<String, String> settings, Context context) {
            startCalled = true;
        }
    }
}