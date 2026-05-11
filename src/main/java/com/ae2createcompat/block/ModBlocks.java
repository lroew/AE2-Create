package com.ae2createcompat.block;

import com.ae2createcompat.blockentity.MEExporterBlockEntity;
import com.ae2createcompat.blockentity.MEImporterBlockEntity;
import com.ae2createcompat.blockentity.MEPatternProviderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(com.ae2createcompat.AE2CreateCompat.MODID);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    /**
     * ME Exporter - 从 AE2 网络导出物品到 Create 物品管道/传送带
     */
    public static final DeferredBlock<Block> ME_EXPORTER = registerBlock(
            "me_exporter",
            () -> new Block(Block.Properties.of().strength(3.0f, 10.0f).noOcclusion()),
            MEExporterBlockEntity::new
    );

    /**
     * ME Importer - 从 Create 物品管道/传送带导入物品到 AE2 网络
     */
    public static final DeferredBlock<Block> ME_IMPORTER = registerBlock(
            "me_importer",
            () -> new Block(Block.Properties.of().strength(3.0f, 10.0f).noOcclusion()),
            MEImporterBlockEntity::new
    );

    /**
     * ME Pattern Provider - 通过 Create 机械动力驱动 AE2 的合成模式
     */
    public static final DeferredBlock<Block> ME_PATTERN_PROVIDER = registerBlock(
            "me_pattern_provider",
            () -> new Block(Block.Properties.of().strength(3.0f, 10.0f).noOcclusion()),
            MEPatternProviderBlockEntity::new
    );

    @SuppressWarnings("unchecked")
    private static <T extends Block & EntityBlock> DeferredBlock<T> registerBlock(
            String name, Supplier<T> block) {
        return (DeferredBlock<T>) BLOCKS.register(name, block);
    }

    private static DeferredBlock<Block> registerBlock(
            String name, Supplier<Block> blockSupplier, Object... params) {
        return BLOCKS.registerSimpleBlock(name, blockSupplier);
    }

    public static VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
