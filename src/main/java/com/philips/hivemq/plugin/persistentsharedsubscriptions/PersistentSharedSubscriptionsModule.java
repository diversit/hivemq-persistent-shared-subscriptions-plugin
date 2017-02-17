package com.philips.hivemq.plugin.persistentsharedsubscriptions;

import com.hivemq.spi.HiveMQPluginModule;
import com.hivemq.spi.PluginEntryPoint;

/**
 * Module configuration for {@link PersistentSharedSubscriptionsPlugin}.
 */
public class PersistentSharedSubscriptionsModule extends HiveMQPluginModule {

    @Override
    protected void configurePlugin() {

    }

    @Override
    protected Class<? extends PluginEntryPoint> entryPointClass() {
        return PersistentSharedSubscriptionsPlugin.class;
    }
}
