package com.ae2createcompat.blockentity;

import com.ae2createcompat.AE2CreateCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AE2CreateCompat.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MEExporterBlockEntity>> ME_EXPORTER_BE =
            BLOCK_ENTITIES.register("me_exporter", () ->
                    BlockEntityType.Builder.of(MEExporterBlockEntity::new,
                            ModBlocks.ME_EXPORTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MEImporterBlockEntity>> ME_IMPORTER_BE =
            BLOCK_ENTITIES.register("me_importer", () ->
                    BlockEntityType.Builder.of(MEImporterBlockEntity::new,
                            ModBlocks.ME_IMPORTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MEPatternProviderBlockEntity>> ME_PATTERN_PROVIDER_BE =
            BLOCK_ENTITIES.register("me_pattern_provider", () ->
                    BlockEntityType.Builder.of(MEPatternProviderBlockEntity::new,
                            ModBlocks.ME_PATTERN_PROVIDER.get()).build(null));
}
