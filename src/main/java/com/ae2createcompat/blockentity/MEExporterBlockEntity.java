package com.ae2createcompat.blockentity;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.me.helpers.BlockEntityNodeListener;
import appeng.me.helpers.IGridConnectedBlockEntity;
import appeng.me.ManagedGridNode;
import com.ae2createcompat.AE2CreateCompat;
import com.ae2createcompat.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * ME Exporter - 从 AE2 网络中提取物品并通过 Create 的物品管道/传送带输出。
 *
 * 工作原理：
 * 1. 连接到 AE2 ME 网络
 * 2. 从 ME 存储中请求指定物品
 * 3. 通过 facing 方向的 Create 物品管道/漏斗/传送带等输出物品
 */
public class MEExporterBlockEntity extends BlockEntity implements IGridConnectedBlockEntity {

    private final Runnable tickCallback = this::serverTick;
    private final IManagedGridNode mainNode = new ManagedGridNode(this, BlockEntityNodeListener.INSTANCE);
    private AEItemKey requestedItem = null;
    private long requestAmount = 64;
    private int tickCounter = 0;
    private static final int EXPORT_INTERVAL = 10; // 每 10 tick 导出一次

    public MEExporterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        mainNode.setInWorldNode(true);
    }

    @Override
    public IGridNode getGridNode(@Nullable Direction dir) {
        return mainNode.getNode();
    }

    @Override
    public IManagedGridNode getMainNode() {
        return mainNode;
    }

    @Override
    public void saveChanges() {
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        mainNode.saveToNBT(tag);
        if (requestedItem != null) {
            tag.put("requestedItem", requestedItem.toTag(provider));
        }
        tag.putLong("requestAmount", requestAmount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        mainNode.loadFromNBT(tag);
        if (tag.contains("requestedItem")) {
            requestedItem = AEItemKey.fromTag(provider, tag.getCompound("requestedItem"));
        }
        requestAmount = tag.getLong("requestAmount");
        if (requestAmount <= 0) requestAmount = 64;
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;

        tickCounter++;
        if (tickCounter < EXPORT_INTERVAL) return;
        tickCounter = 0;

        if (!mainNode.isReady()) return;

        IGrid grid = mainNode.getGrid();
        if (grid == null) return;

        IStorageService storageService = grid.getService(IStorageService.class);
        MEStorage meStorage = storageService.getInventory();

        // 从 ME 网络中提取请求的物品
        if (requestedItem != null) {
            var extracted = meStorage.extract(requestedItem, requestAmount, appeng.api.config.Actionable.MODULATE, new appeng.me.helpers.BaseActionSource());
            if (extracted > 0) {
                ItemStack stack = requestedItem.toStack((int) Math.min(extracted, Integer.MAX_VALUE));
                // 尝试推送到相邻的 Create 物品管道或其他容器
                pushToAdjacent(stack);
            }
        }
    }

    /**
     * 将物品推送到相邻的面（通常连接 Create 物品管道或传送带）
     */
    private void pushToAdjacent(ItemStack stack) {
        if (stack.isEmpty()) return;

        for (Direction dir : Direction.values()) {
            if (level == null) continue;
            BlockPos neighborPos = worldPosition.relative(dir);
            var be = level.getBlockEntity(neighborPos);
            if (be == null) continue;

            IItemHandler handler = net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK.getCapability(
                    level, neighborPos, level.getBlockState(neighborPos), be, dir.getOpposite());
            if (handler != null) {
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, stack, false);
                if (remainder.isEmpty()) return; // 全部插入成功
                stack = remainder;
            }
        }
    }

    /**
     * 设置要导出的物品和数量
     */
    public void setRequestedItem(AEItemKey item, long amount) {
        this.requestedItem = item;
        this.requestAmount = amount;
        setChanged();
    }

    public AEItemKey getRequestedItem() {
        return requestedItem;
    }

    public long getRequestAmount() {
        return requestAmount;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (!level.isClientSide) {
            mainNode.create(level, worldPosition);
            ModBlocks.registerTickCallback(tickCallback);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        mainNode.destroy();
        ModBlocks.unregisterTickCallback(tickCallback);
    }
}