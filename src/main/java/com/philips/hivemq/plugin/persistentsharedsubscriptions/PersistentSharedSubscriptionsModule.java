package com.philips.hivemq.plugin.persistentsharedsubscriptions;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.hivemq.spi.HiveMQPluginModule;
import com.hivemq.spi.PluginEntryPoint;
import com.hivemq.spi.services.PluginExecutorService;
import com.hivemq.spi.services.PublishService;
import com.philips.hivemq.plugin.persistentsharedsubscriptions.service.*;

/**
 * Module configuration for {@link PersistentSharedSubscriptionsPlugin}.
 */
public class PersistentSharedSubscriptionsModule extends HiveMQPluginModule {

    @Override
    protected void configurePlugin() {
    }

    @Provides
    @Singleton
    public SharedSubscriptionRegistry sharedSubscriptionRegistry() {
        return new InMemorySharedSubscriptionRegistry();
    }

    @Provides
    @Singleton
    public SharedSubscriptionService sharedSubscriptionService(SharedSubscriptionRegistry registry,
                                                               PersistSharedSubscriptionMessageService persistSharedSubscriptionMessageService) {
        return new SharedSubscriptionService(registry, persistSharedSubscriptionMessageService);
    }

    @Provides
    @Singleton
    public PersistSharedSubscriptionMessageService persistSharedSubscriptionMessageService(PluginExecutorService pluginExecutorService,
                                                                                           SharedSubscriptionRegistry registry,
                                                                                           MessageStore messageStore,
                                                                                           PublishService publishService) {
        return new PersistSharedSubscriptionMessageService(pluginExecutorService, registry, messageStore, publishService);
    }

    @Provides
    @Singleton
    public MessageStore messageStore() {
        return new InMemoryMessageStore();
    }

    @Override
    protected Class<? extends PluginEntryPoint> entryPointClass() {
        return PersistentSharedSubscriptionsPlugin.class;
    }
}
