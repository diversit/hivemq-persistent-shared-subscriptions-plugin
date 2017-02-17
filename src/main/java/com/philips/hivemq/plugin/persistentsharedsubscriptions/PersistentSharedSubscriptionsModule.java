package com.philips.hivemq.plugin.persistentsharedsubscriptions;

import com.google.inject.Provides;
import com.hivemq.spi.HiveMQPluginModule;
import com.hivemq.spi.PluginEntryPoint;
import com.philips.hivemq.plugin.persistentsharedsubscriptions.service.InMemorySharedSubscriptionRegistry;
import com.philips.hivemq.plugin.persistentsharedsubscriptions.service.SharedSubscriptionRegistry;
import com.philips.hivemq.plugin.persistentsharedsubscriptions.service.SharedSubscriptionService;

/**
 * Module configuration for {@link PersistentSharedSubscriptionsPlugin}.
 */
public class PersistentSharedSubscriptionsModule extends HiveMQPluginModule {

    @Override
    protected void configurePlugin() {
    }

    @Provides
    public SharedSubscriptionRegistry sharedSubscriptionRegistry() {
        return new InMemorySharedSubscriptionRegistry();
    }

    @Provides
    public SharedSubscriptionService sharedSubscriptionService(SharedSubscriptionRegistry registry) {
        return new SharedSubscriptionService(registry);
    }

    @Override
    protected Class<? extends PluginEntryPoint> entryPointClass() {
        return PersistentSharedSubscriptionsPlugin.class;
    }
}
