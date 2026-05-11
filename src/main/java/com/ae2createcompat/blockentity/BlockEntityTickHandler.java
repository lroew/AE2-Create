package com.ae2createcompat.blockentity;

import com.ae2createcompat.AE2CreateCompat;
import com.ae2createcompat.block.ModBlocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 方块实体 Tick 处理器 - 驱动 ME 方块实体的逻辑 tick
 */
@EventBusSubscriber(modid = AE2CreateCompat.MODID)
public class BlockEntityTickHandler {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ModBlocks.tickAll();
    }
}
