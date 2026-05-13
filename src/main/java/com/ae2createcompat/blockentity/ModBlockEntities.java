package com.ae2createcompat.blockentity;

import com.ae2createcompat.AE2CreateCompat;
import com.ae2createcompat.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AE2CreateCompat.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MEExporterBlockEntity>> ME_EXPORTER =
            BLOCK_ENTITIES.register("me_exporter", () ->
                    BlockEntityType.Builder.of((pos, state) ->
                            new MEExporterBlockEntity(ModBlockEntities.ME_EXPORTER.get(), pos, state),
                            ModBlocks.ME_EXPORTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MEImporterBlockEntity>> ME_IMPORTER =
            BLOCK_ENTITIES.register("me_importer", () ->
                    BlockEntityType.Builder.of((pos, state) ->
                            new MEImporterBlockEntity(ModBlockEntities.ME_IMPORTER.get(), pos, state),
                            ModBlocks.ME_IMPORTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MEPatternProviderBlockEntity>> ME_PATTERN_PROVIDER =
            BLOCK_ENTITIES.register("me_pattern_provider", () ->
                    BlockEntityType.Builder.of((pos, state) ->
                            new MEPatternProviderBlockEntity(ModBlockEntities.ME_PATTERN_PROVIDER.get(), pos, state),
                            ModBlocks.ME_PATTERN_PROVIDER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MEStockKeeperBlockEntity>> ME_STOCK_KEEPER =
            BLOCK_ENTITIES.register("me_stock_keeper", () ->
                    BlockEntityType.Builder.of((pos, state) ->
                            new MEStockKeeperBlockEntity(ModBlockEntities.ME_STOCK_KEEPER.get(), pos, state),
                            ModBlocks.ME_STOCK_KEEPER.get()).build(null));
}
