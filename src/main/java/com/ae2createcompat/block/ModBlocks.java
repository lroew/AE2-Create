package com.ae2createcompat.block;

import com.ae2createcompat.blockentity.MEExporterBlockEntity;
import com.ae2createcompat.blockentity.MEImporterBlockEntity;
import com.ae2createcompat.blockentity.MEPatternProviderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(com.ae2createcompat.AE2CreateCompat.MODID);

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    /**
     * Track all active mod block entities for ticking purposes.
     */
    private static final List<Runnable> TICK_CALLBACKS = new CopyOnWriteArrayList<>();

    /**
     * ME Exporter - 从 AE2 网络导出物品到 Create 物品管道/传送带
     * 支持所有 Create 6.0+ 设备
     */
    public static final DeferredBlock<Block> ME_EXPORTER = BLOCKS.registerBlock(
            "me_exporter",
            properties -> new MEExporterBlock(properties)
    );

    /**
     * ME Importer - 从 Create 物品管道/传送带导入物品到 AE2 网络
     * 支持所有 Create 6.0+ 设备
     */
    public static final DeferredBlock<Block> ME_IMPORTER = BLOCKS.registerBlock(
            "me_importer",
            properties -> new MEImporterBlock(properties)
    );

    /**
     * ME Pattern Provider - 通过 Create 机械动力驱动 AE2 的合成模式
     * 支持切割、压榨、组装、清洗、烘干等 Create 加工流程
     */
    public static final DeferredBlock<Block> ME_PATTERN_PROVIDER = BLOCKS.registerBlock(
            "me_pattern_provider",
            properties -> new MEPatternProviderBlock(properties)
    );

    /**
     * ME Stock Keeper - ME 网络库存监控器，与 Create 智能漏斗和显示器联动
     * 监控 ME 网络中指定物品的数量，输出红石信号控制 Create 设备
     */
    public static final DeferredBlock<Block> ME_STOCK_KEEPER = BLOCKS.registerBlock(
            "me_stock_keeper",
            properties -> new MEStockKeeperBlock(properties)
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
            try {
                callback.run();
            } catch (Exception e) {
                com.ae2createcompat.AE2CreateCompat.LOGGER.error("Error ticking block entity callback", e);
            }
        }
    }

    /**
     * Base block class for ME Exporter
     */
    public static class MEExporterBlock extends HorizontalDirectionalBlock implements EntityBlock {
        public MEExporterBlock(Properties properties) {
            super(properties.strength(3.0f, 6.0f));
            registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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

        @Override
        public void appendHoverText(ItemStack stack, ItemTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("block.ae2createcompat.me_exporter.tooltip"));
            tooltip.add(Component.translatable("block.ae2createcompat.me_exporter.tooltip.compat"));
        }
    }

    /**
     * Base block class for ME Importer
     */
    public static class MEImporterBlock extends HorizontalDirectionalBlock implements EntityBlock {
        public MEImporterBlock(Properties properties) {
            super(properties.strength(3.0f, 6.0f));
            registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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

        @Override
        public void appendHoverText(ItemStack stack, ItemTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("block.ae2createcompat.me_importer.tooltip"));
            tooltip.add(Component.translatable("block.ae2createcompat.me_importer.tooltip.compat"));
        }
    }

    /**
     * Base block class for ME Pattern Provider
     */
    public static class MEPatternProviderBlock extends HorizontalDirectionalBlock implements EntityBlock {
        public MEPatternProviderBlock(Properties properties) {
            super(properties.strength(3.0f, 6.0f));
            registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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

        @Override
        public void appendHoverText(ItemStack stack, ItemTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("block.ae2createcompat.me_pattern_provider.tooltip"));
            tooltip.add(Component.translatable("block.ae2createcompat.me_pattern_provider.tooltip.compat"));
        }
    }

    /**
     * Base block class for ME Stock Keeper
     */
    public static class MEStockKeeperBlock extends HorizontalDirectionalBlock implements EntityBlock {
        public MEStockKeeperBlock(Properties properties) {
            super(properties.strength(3.0f, 6.0f));
            registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new com.ae2createcompat.blockentity.MEStockKeeperBlockEntity(
                    com.ae2createcompat.blockentity.ModBlockEntities.ME_STOCK_KEEPER.get(), pos, state);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public void appendHoverText(ItemStack stack, ItemTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("block.ae2createcompat.me_stock_keeper.tooltip"));
            tooltip.add(Component.translatable("block.ae2createcompat.me_stock_keeper.tooltip.compat"));
        }
    }
}
