package com.ae2createcompat.block;

import com.ae2createcompat.blockentity.MEExporterBlockEntity;
import com.ae2createcompat.blockentity.MEImporterBlockEntity;
import com.ae2createcompat.blockentity.MEPatternProviderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(com.ae2createcompat.AE2CreateCompat.MODID);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    /**
     * Track all active mod block entities for ticking purposes.
     */
    private static final List<Runnable> TICK_CALLBACKS = new CopyOnWriteArrayList<>();

    /**
     * ME Exporter - 从 AE2 网络导出物品到 Create 物品管道/传送带
     */
    public static final DeferredBlock<Block> ME_EXPORTER = BLOCKS.registerBlock(
            "me_exporter",
            properties -> new MEExporterBlock(properties)
    );

    /**
     * ME Importer - 从 Create 物品管道/传送带导入物品到 AE2 网络
     */
    public static final DeferredBlock<Block> ME_IMPORTER = BLOCKS.registerBlock(
            "me_importer",
            properties -> new MEImporterBlock(properties)
    );

    /**
     * ME Pattern Provider - 通过 Create 机械动力驱动 AE2 的合成模式
     */
    public static final DeferredBlock<Block> ME_PATTERN_PROVIDER = BLOCKS.registerBlock(
            "me_pattern_provider",
            properties -> new MEPatternProviderBlock(properties)
    );

    /**
     * Register a tick callback for a block entity.
     */
    public static void registerTickCallback(Runnable callback) {
        TICK_CALLBACKS.add(callback);
    }

    /**
     * Unregister a tick callback for a block entity.
     */
    public static void unregisterTickCallback(Runnable callback) {
        TICK_CALLBACKS.remove(callback);
    }

    /**
     * Tick all registered mod block entities.
     */
    public static void tickAll() {
        for (Runnable callback : TICK_CALLBACKS) {
            callback.run();
        }
    }

    /**
     * Base block class for mod blocks that have block entities
     */
    public static class MEExporterBlock extends Block implements EntityBlock {
        public MEExporterBlock(Properties properties) {
            super(properties);
        }

        @Override
        public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new MEExporterBlockEntity(
                    com.ae2createcompat.blockentity.ModBlockEntities.ME_EXPORTER.get(), pos, state);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }
    }

    public static class MEImporterBlock extends Block implements EntityBlock {
        public MEImporterBlock(Properties properties) {
            super(properties);
        }

        @Override
        public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new MEImporterBlockEntity(
                    com.ae2createcompat.blockentity.ModBlockEntities.ME_IMPORTER.get(), pos, state);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }
    }

    public static class MEPatternProviderBlock extends Block implements EntityBlock {
        public MEPatternProviderBlock(Properties properties) {
            super(properties);
        }

        @Override
        public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new MEPatternProviderBlockEntity(
                    com.ae2createcompat.blockentity.ModBlockEntities.ME_PATTERN_PROVIDER.get(), pos, state);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }
    }
}
