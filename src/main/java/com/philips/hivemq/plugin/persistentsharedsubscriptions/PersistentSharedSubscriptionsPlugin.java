package com.philips.hivemq.plugin.persistentsharedsubscriptions;

import com.hivemq.spi.PluginEntryPoint;
import com.hivemq.spi.callback.events.OnPublishReceivedCallback;
import com.hivemq.spi.callback.exception.OnPublishReceivedException;
import com.hivemq.spi.message.PUBLISH;
import com.hivemq.spi.security.ClientData;
import com.philips.hivemq.plugin.persistentsharedsubscriptions.service.PersistSharedSubscriptionMessageService;
import com.philips.hivemq.plugin.persistentsharedsubscriptions.service.SharedSubscriptionRegistry;
import com.philips.hivemq.plugin.persistentsharedsubscriptions.service.SharedSubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * {@link PluginEntryPoint} of the persistent shared subscriptions plugin.
 *
 * Registers the callbacks needed to implement the feature.
 *
 * Goal:
 * Stores all messages on a topic on which shared subscriptions are registered
 * when currently no clients are connected.
 * When first client connects to a shared subscription topic, that client will
 * receive all missed messages.
 */
public class PersistentSharedSubscriptionsPlugin extends PluginEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(PersistentSharedSubscriptionsPlugin.class);

    private final SharedSubscriptionRegistry registry;
    private final SharedSubscriptionService sharedSubscriptionService;
    private PersistSharedSubscriptionMessageService persistSharedSubscriptionMessageService;

    @Inject
    public PersistentSharedSubscriptionsPlugin(SharedSubscriptionRegistry registry,
                                               SharedSubscriptionService sharedSubscriptionService,
                                               PersistSharedSubscriptionMessageService persistSharedSubscriptionMessageService) {
        this.registry = registry;
        this.sharedSubscriptionService = sharedSubscriptionService;
        this.persistSharedSubscriptionMessageService = persistSharedSubscriptionMessageService;
    }

    @PostConstruct
    public void registerCallbacks() {
        getCallbackRegistry().addCallback(sharedSubscriptionService);
        getCallbackRegistry().addCallback(persistSharedSubscriptionMessageService);
    }

}
