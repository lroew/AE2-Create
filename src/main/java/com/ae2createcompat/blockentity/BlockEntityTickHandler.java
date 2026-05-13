package com.ae2createcompat.blockentity;

import com.ae2createcompat.AE2CreateCompat;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 方块实体 Tick 处理器 - 驱动所有 ME 方块实体的逻辑 tick
 *
 * 统一管理所有已注册的方块实体 tick 回调，
 * 确保 Create 设备的兼容处理在同一 tick 内一致执行。
 */
@EventBusSubscriber(modid = AE2CreateCompat.MODID)
public class BlockEntityTickHandler {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        try {
            ModBlocks.tickAll();
        } catch (Exception e) {
            AE2CreateCompat.LOGGER.error("Error during block entity tick processing", e);
        }
    }
}
