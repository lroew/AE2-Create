package com.ae2createcompat.item;

import com.ae2createcompat.AE2CreateCompat;
import com.ae2createcompat.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AE2CreateCompat.MODID);

    public static final DeferredItem<BlockItem> ME_EXPORTER_ITEM = ITEMS.registerSimpleBlockItem(
            "me_exporter", ModBlocks.ME_EXPORTER);

    public static final DeferredItem<BlockItem> ME_IMPORTER_ITEM = ITEMS.registerSimpleBlockItem(
            "me_importer", ModBlocks.ME_IMPORTER);

    public static final DeferredItem<BlockItem> ME_PATTERN_PROVIDER_ITEM = ITEMS.registerSimpleBlockItem(
            "me_pattern_provider", ModBlocks.ME_PATTERN_PROVIDER);

    public static final DeferredItem<BlockItem> ME_STOCK_KEEPER_ITEM = ITEMS.registerSimpleBlockItem(
            "me_stock_keeper", ModBlocks.ME_STOCK_KEEPER);
}
