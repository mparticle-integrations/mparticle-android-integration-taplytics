package com.mparticle.kits

import android.content.Context
import com.mparticle.MPEvent
import com.mparticle.MParticle.IdentityType
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.identity.MParticleUser
import com.mparticle.kits.KitIntegration.AttributeListener
import com.mparticle.kits.KitIntegration.CommerceListener
import com.mparticle.kits.KitIntegration.IdentityListener
import com.mparticle.kits.KitIntegration.SessionListener
import com.taplytics.sdk.Taplytics
import org.json.JSONException
import org.json.JSONObject
import java.lang.Exception
import java.math.BigDecimal
import java.util.HashMap

open class TaplyticsKit :
    KitIntegration(),
    AttributeListener,
    KitIntegration.EventListener,
    CommerceListener,
    IdentityListener,
    SessionListener {
    private fun mergeOptions(
        tlOptions: Map<String?, Any?>,
        configuration: MutableMap<String, Any>?,
    ): HashMap<String?, Any?> {
        var tlOptions: Map<String?, Any?>? = tlOptions
        var configuration = configuration
        if (tlOptions == null) {
            tlOptions = HashMap()
        }
        if (configuration == null) {
            configuration = HashMap()
        }
        val merged = HashMap(configuration)
        for ((key, value) in tlOptions) {
            merged[key] = value
        }
        return merged
    }

    public override fun onKitCreate(
        settings: Map<String, String>,
        context: Context,
    ): List<ReportingMessage> {
        if (!started && !delayInitializationUntilSessionStart) {
            started = true
            startTaplytics(settings, context)
        }
        return emptyList()
    }

    private fun createReportingMessages(message: String): List<ReportingMessage> {
        val reportingMessage =
            ReportingMessage(
                this,
                message,
                System.currentTimeMillis(),
                null,
            )
        return listOf(reportingMessage)
    }

    protected open fun startTaplytics(
        settings: Map<String, String>,
        context: Context?,
    ) {
        val apiKey = getAPIKey(settings)
        val options = mergeOptions(tlOptions, getOptionsFromConfiguration(settings))
        options[DELAYED_START] = true
        Taplytics.startTaplytics(context, apiKey, options)
    }

    private fun getAPIKey(settings: Map<String, String>): String? {
        val apiKey = settings[API_KEY]
        require(!KitUtils.isEmpty(apiKey)) { FAILED_TO_INITIALIZE_KIT_MESSAGE }
        return apiKey
    }

    private fun getOptionsFromConfiguration(settings: Map<String, String>): MutableMap<String, Any>? {
        val options = HashMap<String, Any>()
        addAggressiveOption(options, settings)
        return if (options.isEmpty()) null else options
    }

    private fun addAggressiveOption(
        options: MutableMap<String, Any>,
        settings: Map<String, String>,
    ) {
        val agg = settings[AGGRESSIVE].toBoolean()
        options[TAPLYTICS_AGGRESSIVE] = agg
    }

    override fun getName(): String = KIT_NAME

    /**
     * AttributeListener Interface
     */
    override fun setUserAttribute(
        attributeKey: String,
        attributeValue: String?,
    ) {
        try {
            val attr = JSONObject()
            if (attributeValue != null) {
                attr.put(attributeKey, attributeValue)
            } else {
                attr.put(attributeKey, "")
            }
            Taplytics.setUserAttributes(attr)
        } catch (e: JSONException) {
        }
    }

    override fun supportsAttributeLists(): Boolean = false

    override fun setAllUserAttributes(
        attributes: Map<String, String>,
        attributeLists: Map<String, List<String>>,
    ) {
        for ((key, value) in attributes) {
            setUserAttribute(key, value)
        }
    }

    override fun setUserIdentity(
        identityType: IdentityType,
        s: String?,
    ) {
        // no-op
    }

    override fun removeUserIdentity(identityType: IdentityType) {
        setUserIdentity(identityType, null)
    }

    override fun removeUserAttribute(attribute: String) {
        setUserAttribute(attribute, null)
    }

    override fun onIdentifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        setUserAttributeFromRequest(filteredIdentityApiRequest)
    }

    /**
     * Identity Listener
     */
    private fun setUserAttributeFromRequest(request: FilteredIdentityApiRequest) {
        val identities = request.userIdentities
        try {
            val attr = JSONObject()
            if (identities[IdentityType.CustomerId] != null) {
                attr.put(USER_ID, identities[IdentityType.CustomerId])
            }
            if (identities[IdentityType.Email] != null) {
                attr.put(EMAIL, identities[IdentityType.Email])
            }
            Taplytics.setUserAttributes(attr)
        } catch (e: JSONException) {
        }
    }

    override fun onLoginCompleted(
        user: MParticleUser,
        request: FilteredIdentityApiRequest,
    ) {
        setUserAttributeFromRequest(request)
    }

    override fun onLogoutCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        Taplytics.resetAppUser {
            // no-op
        }
    }

    override fun onModifyCompleted(
        mParticleUser: MParticleUser,
        request: FilteredIdentityApiRequest,
    ) {
        setUserAttributeFromRequest(request)
    }

    override fun onUserIdentified(mParticleUser: MParticleUser) {
        // no-op
    }

    /**
     * Unsupported methods
     */
    override fun logout(): List<ReportingMessage> = emptyList()

    override fun setUserAttributeList(
        attribute: String,
        attributeValueList: List<String>,
    ) {}

    /**
     * CommerceListener Interface
     */
    override fun logEvent(event: CommerceEvent): List<ReportingMessage> {
        if (!KitUtils.isEmpty(event.productAction) &&
            event.productAction.equals(Product.PURCHASE, true)
        ) {
            val transactionAttributes = event.transactionAttributes ?: return emptyList()
            val id = transactionAttributes.id
            val revenue = transactionAttributes.revenue ?: return emptyList()
            Taplytics.logRevenue(id, revenue)
            return listOf(ReportingMessage.fromEvent(this, event))
        }
        return emptyList()
    }

    /**
     * EventListener Interface
     */
    override fun logEvent(event: MPEvent): List<ReportingMessage>? {
        val eventName = event.eventName
        val metaDataMap = event.customAttributeStrings
        var metaData: JSONObject? = null
        if (metaDataMap != null) {
            metaData = (metaDataMap as Map<*, *>?)?.let { JSONObject(it) }
        }
        Taplytics.logEvent(eventName, null, metaData)
        return listOf(ReportingMessage.fromEvent(this, event))
    }

    override fun logScreen(
        screenName: String,
        screenAttributes: Map<String, String>,
    ): List<ReportingMessage> {
        Taplytics.logEvent(screenName)
        return createReportingMessages(ReportingMessage.MessageType.SCREEN_VIEW)
    }

    /**
     * Unsupported Methods
     */
    override fun logException(
        exception: Exception,
        exceptionAttributes: Map<String, String>,
        message: String,
    ): List<ReportingMessage> = emptyList()

    override fun logError(
        message: String,
        errorAttributes: Map<String, String>,
    ): List<ReportingMessage> = emptyList()

    override fun leaveBreadcrumb(breadcrumb: String): List<ReportingMessage> = emptyList()

    // put all these together
    override fun logLtvIncrease(
        valueIncreased: BigDecimal,
        valueTotal: BigDecimal,
        eventName: String,
        contextInfo: Map<String, String>,
    ): List<ReportingMessage> = emptyList()

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> {
        Taplytics.hasUserOptedOutTracking(null) { hasOptedOut ->
            if (!hasOptedOut && optedOut) {
                Taplytics.optOutUserTracking(null)
            } else if (hasOptedOut && !optedOut) {
                Taplytics.optInUserTracking(null)
            }
        }
        return createReportingMessages(ReportingMessage.MessageType.OPT_OUT)
    }

    override fun onSessionStart(): List<ReportingMessage> {
        if (!started && delayInitializationUntilSessionStart) {
            started = true
            startTaplytics(configuration.settings, context)
        }
        return emptyList()
    }

    override fun onSessionEnd(): List<ReportingMessage> = emptyList()

    companion object {
        /**
         * Option Keys
         */
        private const val API_KEY = "apiKey"
        private const val AGGRESSIVE = "TaplyticsOptionAggressive"
        private const val TAPLYTICS_AGGRESSIVE = "aggressive"
        private const val USER_ID = "user_id"
        private const val EMAIL = "email"
        private const val DELAYED_START = "delayedStartTaplytics"
        private const val FAILED_TO_INITIALIZE_KIT_MESSAGE = "Failed to initialize Taplytics SDK - an API key is required"
        private const val KIT_NAME = "Taplytics"

        @JvmField
        var delayInitializationUntilSessionStart = false

        @JvmField
        var started = false

        /**
         * tlOptions get and set methods
         */
        var tlOptions = HashMap<String?, Any?>()
    }
}
