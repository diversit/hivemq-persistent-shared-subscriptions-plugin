package com.philips.hivemq.plugin.persistentsharedsubscriptions;

import com.hivemq.spi.PluginEntryPoint;
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

    @Inject
    public PersistentSharedSubscriptionsPlugin() {
    }

    @PostConstruct
    public void registerCallbacks() {
        new SharedSubscriptionService(getCallbackRegistry());
    }

}