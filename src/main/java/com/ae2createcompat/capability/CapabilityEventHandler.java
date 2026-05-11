package com.ae2createcompat.capability;

import com.ae2createcompat.AE2CreateCompat;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Capability 事件订阅器 - 注册 AE2-Create 桥接能力
 */
@EventBusSubscriber(modid = AE2CreateCompat.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CapabilityEventHandler {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        CapabilityRegistrar.registerCapabilities(event);
    }
}
