package com.ae2createcompat;

import com.ae2createcompat.block.ModBlocks;
import com.ae2createcompat.blockentity.ModBlockEntities;
import com.ae2createcompat.capability.CapabilityRegistrar;
import com.ae2createcompat.compat.CreateCompat;
import com.ae2createcompat.item.ModItems;
import com.ae2createcompat.recipe.ModRecipes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(AE2CreateCompat.MODID)
public class AE2CreateCompat {
    public static final String MODID = "ae2createcompat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public AE2CreateCompat(IEventBus modEventBus) {
        LOGGER.info("AE2 Create Compat - Initializing...");

        // Register deferred registries
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);

        // Register setup event
        modEventBus.addListener(this::commonSetup);

        // Register game events
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("AE2 Create Compat - Common Setup");

        event.enqueueWork(() -> {
            // Register capabilities that bridge AE2 and Create
            CapabilityRegistrar.register();
        });

        // Initialize compat layer
        CreateCompat.init();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("AE2 Create Compat - Server Starting");
    }
}
