package com.mparticle.kits;

import android.content.Context;

import org.json.JSONException;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.math.BigDecimal;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.TransactionAttributes;

import com.mparticle.identity.MParticleUser;
import com.taplytics.sdk.Taplytics;
import com.taplytics.sdk.TaplyticsHasUserOptedOutListener;
import com.taplytics.sdk.TaplyticsResetUserListener;

import org.json.JSONObject;

import static com.mparticle.MParticle.IdentityType;

public class TaplyticsKit extends KitIntegration
        implements
        KitIntegration.AttributeListener,
        KitIntegration.EventListener,
        KitIntegration.CommerceListener,
        KitIntegration.IdentityListener {

    /**
     * Option Keys
     */
    private static final String API_KEY = "apiKey";
    private static final String AGGRESSIVE = "TaplyticsOptionAggressive";
    private static final String TAPLYTICS_AGGRESSIVE = "aggressive";
    private static final String USER_ID = "user_id";
    private static final String EMAIL = "email";
    private static final String DELAYED_START = "delayedStartTaplytics";

    /**
     * tlOptions get and set methods
     */

    private static Map<String, Object> tlOptions = new HashMap<>();

    public static Map<String, Object> getTlOptions() {
        return tlOptions;
    }

    public static void setTlOptions(Map<String, Object> options) {
        tlOptions = options;
    }

    private HashMap<String, Object> mergeOptions(Map<String, Object> tlOptions, Map<String, Object> configuration) {
        if (tlOptions == null) {
            tlOptions = new HashMap<>();
        }
        if (configuration == null) {
            configuration = new HashMap<>();
        }
        HashMap<String, Object> merged = new HashMap<>(configuration);
        for (Map.Entry<String, Object> entry : tlOptions.entrySet()) {
            merged.put(entry.getKey(), entry.getValue());
        }
        return merged;
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        startTaplytics(settings, context);
        return null;
    }

    private List<ReportingMessage> createReportingMessages(String message) {
        ReportingMessage reportingMessage = new ReportingMessage(this,
                message,
                System.currentTimeMillis(),
                null);
        return Collections.singletonList(reportingMessage);
    }

    private void startTaplytics(Map<String, String> settings, Context context) {
        String apiKey = getAPIKey(settings);
        HashMap<String, Object> options = mergeOptions(getTlOptions(), getOptionsFromConfiguration(settings));
        options.put(DELAYED_START, true);
        Taplytics.startTaplytics(context, apiKey, options);
    }

    private String getAPIKey(Map<String, String> settings) {
        final String apiKey = settings.get(API_KEY);
        if (KitUtils.isEmpty(apiKey)) {
            throw new IllegalArgumentException("Failed to initialize Taplytics SDK - an API key is required");
        }
        return apiKey;
    }


    private Map<String, Object> getOptionsFromConfiguration(Map<String, String> settings) {
        Map<String, Object> options = new HashMap<>();
        addAggressiveOption(options, settings);

        return options.isEmpty() ? null : options;
    }

    private void addAggressiveOption(Map<String, Object> options, Map<String, String> settings) {
        Boolean agg = Boolean.parseBoolean(settings.get(AGGRESSIVE));
        options.put(TAPLYTICS_AGGRESSIVE, agg.booleanValue());
    }

    @Override
    public String getName() {
        return "Taplytics";
    }

    /**
     * AttributeListener Interface
     */

    @Override
    public void setUserAttribute(String attributeKey, String attributeValue) {
        try {
            JSONObject attr = new JSONObject();
            if (attributeValue != null) {
                attr.put(attributeKey, attributeValue);
            } else {
                attr.put(attributeKey, "");
            }
            Taplytics.setUserAttributes(attr);
        } catch (JSONException e) {

        }
    }

    @Override
    public boolean supportsAttributeLists() { return false; }

    @Override
    public void setAllUserAttributes(Map<String, String> attributes, Map<String, List<String>> attributeLists) {
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            setUserAttribute(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void setUserIdentity(MParticle.IdentityType identityType, String identity) {
        switch (identityType) {
            case CustomerId: {
                setUserAttribute(USER_ID, identity);
                break;
            }
            case Email: {
                setUserAttribute(EMAIL, identity);
            }
        }
    }

    @Override
    public void removeUserIdentity(MParticle.IdentityType identityType) {
        setUserIdentity(identityType, null);
    }


    @Override
    public void removeUserAttribute(String attribute) {
        setUserAttribute(attribute, null);
    }

    @Override
    public void onIdentifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        setUserAttributeFromRequest(filteredIdentityApiRequest);
    }

    /**
     * Identity Listener
     */

    public void setUserAttributeFromRequest(FilteredIdentityApiRequest request) {
        Map<MParticle.IdentityType, String> identities = request.userIdentities;
        try {
            JSONObject attr = new JSONObject();
            if (identities.get(IdentityType.CustomerId) != null) {
                attr.put(USER_ID, identities.get(IdentityType.CustomerId));
            }
            if (identities.get(IdentityType.Email) != null) {
                attr.put(EMAIL, identities.get(IdentityType.Email));
            }
            Taplytics.setUserAttributes(attr);
        } catch (JSONException e) {

        }
    }

    @Override
    public void onLoginCompleted(MParticleUser user, FilteredIdentityApiRequest request) {
        setUserAttributeFromRequest(request);
    }

    @Override
    public void onLogoutCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        Taplytics.resetAppUser(new TaplyticsResetUserListener() {
            @Override
            public void finishedResettingUser() {
                // no-op
            }
        });
    }

    @Override
    public void onModifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        // no-op
    }

    @Override
    public void onUserIdentified(MParticleUser mParticleUser) {
        // no-op
    }


    /**
     * Unsupported methods
     */
    @Override
    public List<ReportingMessage> logout() { return null; }

    @Override
    public void setUserAttributeList(String attribute, List<String> attributeValueList) { }

    /**
     * CommerceListener Interface
     */

    @Override
    public List<ReportingMessage> logEvent(CommerceEvent event) {
        if (!KitUtils.isEmpty(event.getProductAction()) &&
                event.getProductAction().equalsIgnoreCase(Product.PURCHASE)) {

            TransactionAttributes transactionAttributes = event.getTransactionAttributes();

            if (transactionAttributes == null) {
                return null;
            }

            String id = transactionAttributes.getId();
            Double revenue = transactionAttributes.getRevenue();

            if (id == null || revenue == null) {
                return null;
            }

            Taplytics.logRevenue(id, revenue);
            return Collections.singletonList(ReportingMessage.fromEvent(this, event));
        }
        return null;
    }

    /**
     * EventListener Interface
     */

    @Override
    public List<ReportingMessage> logEvent(MPEvent event) {
        String eventName = event.getEventName();
        Taplytics.logEvent(eventName);
        return Collections.singletonList(ReportingMessage.fromEvent(this, event));
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> screenAttributes) {
        Taplytics.logEvent(screenName);
        return createReportingMessages(ReportingMessage.MessageType.SCREEN_VIEW);
    }

    /**
     * Unsupported Methods
     */

    @Override
    public List<ReportingMessage> logException(Exception exception, Map<String, String> exceptionAttributes, String message) { return null; }

    @Override
    public List<ReportingMessage> logError(String message, Map<String, String> errorAttributes) { return null; }

    @Override
    public List<ReportingMessage> leaveBreadcrumb(String breadcrumb) { return null; }

    //put all these together
    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, BigDecimal valueTotal, String eventName, Map<String, String> contextInfo) { return null; }

    @Override
    public List<ReportingMessage> setOptOut(final boolean optedOut) {
        Taplytics.hasUserOptedOutTracking(null, new TaplyticsHasUserOptedOutListener() {
            @Override
            public void hasUserOptedOutTracking(boolean hasOptedOut) {
                if (!hasOptedOut && optedOut) {
                    Taplytics.optOutUserTracking(null);
                } else if (hasOptedOut && !optedOut) {
                    Taplytics.optInUserTracking(null);
                }
            }
        });

        return createReportingMessages(ReportingMessage.MessageType.OPT_OUT);
    }
}