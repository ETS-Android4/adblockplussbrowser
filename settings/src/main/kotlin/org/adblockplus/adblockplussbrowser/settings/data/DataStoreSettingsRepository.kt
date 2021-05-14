package org.adblockplus.adblockplussbrowser.settings.data

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.adblockplus.adblockplussbrowser.base.data.model.Subscription
import org.adblockplus.adblockplussbrowser.settings.data.local.SubscriptionsDataSource
import org.adblockplus.adblockplussbrowser.settings.data.model.Settings
import org.adblockplus.adblockplussbrowser.settings.data.model.UpdateConfig
import org.adblockplus.adblockplussbrowser.settings.data.proto.ProtoSettings
import org.adblockplus.adblockplussbrowser.settings.data.proto.toProtoSubscription
import org.adblockplus.adblockplussbrowser.settings.data.proto.toProtoUpdateConfig
import org.adblockplus.adblockplussbrowser.settings.data.proto.toSettings

internal class DataStoreSettingsRepository(
    private val dataStore: DataStore<ProtoSettings>,
    private val subscriptionsDataSource: SubscriptionsDataSource,
) : SettingsRepository {

    override val settings: Flow<Settings> = dataStore.data
        .map { it.toSettings() }

    override suspend fun getEasylistSubscription(): Subscription =
        subscriptionsDataSource.getEasylistSubscription()

    override suspend fun getAcceptableAdsSubscription(): Subscription =
        subscriptionsDataSource.getAcceptableAdsSubscription()

    override suspend fun getDefaultPrimarySubscriptions(): List<Subscription> =
        subscriptionsDataSource.getDefaultPrimarySubscriptions()

    override suspend fun getDefaultOtherSubscriptions(): List<Subscription> =
        subscriptionsDataSource.getDefaultOtherSubscriptions()

    override suspend fun setAdblockEnabled(enabled: Boolean) {
        dataStore.updateData { settings ->
            settings.toBuilder().setAdblockEnabled(enabled).build()
        }
    }

    override suspend fun setAcceptableAdsEnabled(enabled: Boolean) {
        dataStore.updateData { settings ->
            settings.toBuilder().setAcceptableAdsEnabled(enabled).build()
        }
    }

    override suspend fun setUpdateConfig(updateConfig: UpdateConfig) {
        dataStore.updateData { settings ->
            settings.toBuilder().setUpdateConfig(updateConfig.toProtoUpdateConfig()).build()
        }
    }

    override suspend fun addAllowedDomain(domain: String) {
        dataStore.updateData { settings ->
            if (settings.allowedDomainsList.contains(domain)) {
                settings
            } else {
                settings.toBuilder().addAllowedDomains(domain).build()
            }
        }
    }

    override suspend fun removeAllowedDomain(domain: String) {
        dataStore.updateData { settings ->
            if (settings.allowedDomainsList.contains(domain)) {
                val allowedDomains = settings.allowedDomainsList.filter { it != domain }
                settings.toBuilder().clearAllowedDomains().addAllAllowedDomains(allowedDomains).build()
            } else {
                settings
            }
        }
    }

    override suspend fun setAllowedDomains(domains: List<String>) {
        dataStore.updateData { settings ->
            settings.toBuilder().clearAllowedDomains().addAllAllowedDomains(domains).build()
        }
    }

    override suspend fun addBlockedDomain(domain: String) {
        dataStore.updateData { settings ->
            if (settings.blockedDomainsList.contains(domain)) {
                settings
            } else {
                settings.toBuilder().addBlockedDomains(domain).build()
            }
        }
    }

    override suspend fun removeBlockedDomain(domain: String) {
        dataStore.updateData { settings ->
            if (settings.blockedDomainsList.contains(domain)) {
                val blockedDomains = settings.blockedDomainsList.filter { it != domain }
                settings.toBuilder().clearBlockedDomains().addAllBlockedDomains(blockedDomains).build()
            } else {
                settings
            }
        }
    }

    override suspend fun setBlockedDomains(domains: List<String>) {
        dataStore.updateData { settings ->
            settings.toBuilder().clearBlockedDomains().addAllBlockedDomains(domains).build()
        }
    }

    override suspend fun addActivePrimarySubscription(subscription: Subscription) {
        dataStore.updateData { settings ->
            if (settings.activePrimarySubscriptionsList.find { it.url == subscription.url } != null) {
                settings
            } else {
                settings.toBuilder().addActivePrimarySubscriptions(subscription.toProtoSubscription()).build()
            }
        }
    }

    override suspend fun removeActivePrimarySubscription(subscription: Subscription) {
        dataStore.updateData { settings ->
            if (settings.activePrimarySubscriptionsList.find { it.url == subscription.url } != null) {
                val activePrimarySubscriptions =
                    settings.activePrimarySubscriptionsList.filter { it.url != subscription.url }
                settings.toBuilder().clearActivePrimarySubscriptions()
                    .addAllActivePrimarySubscriptions(activePrimarySubscriptions).build()
            } else {
                settings
            }
        }
    }

    override suspend fun setActivePrimarySubscriptions(subscriptions: List<Subscription>) {
        dataStore.updateData { settings ->
            settings.toBuilder().clearActivePrimarySubscriptions()
                .addAllActivePrimarySubscriptions(subscriptions.map { it.toProtoSubscription() })
                .build()
        }
    }

    override suspend fun addActiveOtherSubscription(subscription: Subscription) {
        dataStore.updateData { settings ->
            if (settings.activeOtherSubscriptionsList.find { it.url == subscription.url } != null) {
                settings
            } else {
                settings.toBuilder().addActiveOtherSubscriptions(subscription.toProtoSubscription()).build()
            }
        }
    }

    override suspend fun removeActiveOtherSubscription(subscription: Subscription) {
        dataStore.updateData { settings ->
            if (settings.activeOtherSubscriptionsList.find { it.url == subscription.url } != null) {
                val activeOtherSubscriptions =
                    settings.activeOtherSubscriptionsList.filter { it.url != subscription.url }
                settings.toBuilder().clearActiveOtherSubscriptions()
                    .addAllActiveOtherSubscriptions(activeOtherSubscriptions).build()
            } else {
                settings
            }
        }
    }

    override suspend fun setActiveOtherSubscriptions(subscriptions: List<Subscription>) {
        dataStore.updateData { settings ->
            settings.toBuilder().clearActiveOtherSubscriptions()
                .addAllActiveOtherSubscriptions(subscriptions.map { it.toProtoSubscription() })
                .build()
        }
    }
}