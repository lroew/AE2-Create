package com.ae2createcompat.blockentity;

import com.ae2createcompat.AE2CreateCompat;
import com.ae2createcompat.block.ModBlocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * 方块实体 Tick 处理器 - 驱动 ME 方块实体的逻辑 tick
 */
@EventBusSubscriber(modid = AE2CreateCompat.MODID)
public class BlockEntityTickHandler {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server == null) return;

        server.getAllLevels().forEach(level -> {
            level.blockEntityTickers.forEach(ticker -> {
                var be = level.getBlockEntity(ticker.pos());
                if (be instanceof MEExporterBlockEntity exporter) {
                    exporter.serverTick();
                } else if (be instanceof MEImporterBlockEntity importer) {
                    importer.serverTick();
                } else if (be instanceof MEPatternProviderBlockEntity provider) {
                    provider.serverTick();
                }
            });
        });
    }
}
