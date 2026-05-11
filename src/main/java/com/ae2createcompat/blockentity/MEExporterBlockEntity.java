package com.ae2createcompat.blockentity;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import appeng.me.helpers.BlockEntityNodeListener;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.ae2createcompat.AE2CreateCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * ME Exporter - 从 AE2 网络中提取物品并通过 Create 的物品管道/传送带输出。
 *
 * 工作原理：
 * 1. 连接到 AE2 ME 网络
 * 2. 从 ME 存储中请求指定物品
 * 3. 通过 facing 方向的 Create 物品管道/漏斗/传送带等输出物品
 */
public class MEExporterBlockEntity extends BlockEntity implements IGridConnectedBlockEntity {

    private IGridNode mainNode;
    private final InternalInventory buffer = InternalInventory.empty();
    private AEItemKey requestedItem = null;
    private long requestAmount = 64;
    private int tickCounter = 0;
    private static final int EXPORT_INTERVAL = 10; // 每 10 tick 导出一次

    public MEExporterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public IGridNode getGridNode(Direction dir) {
        return getMainNode();
    }

    @Override
    public IGridNode getMainNode() {
        if (mainNode == null) {
            mainNode = BlockEntityNodeListener.createNode(this, null);
        }
        return mainNode;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (requestedItem != null) {
            tag.put("requestedItem", requestedItem.toTagGeneric());
        }
        tag.putLong("requestAmount", requestAmount);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("requestedItem")) {
            requestedItem = AEItemKey.fromTagGeneric(tag.getCompound("requestedItem"));
        }
        requestAmount = tag.getLong("requestAmount");
        if (requestAmount <= 0) requestAmount = 64;
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;

        tickCounter++;
        if (tickCounter < EXPORT_INTERVAL) return;
        tickCounter = 0;

        IGrid grid = null;
        IGridNode node = getMainNode();
        if (node != null) {
            grid = node.getGrid();
        }
        if (grid == null) return;

        IStorageService storageService = grid.getCraftingService().getGrid();
        MEStorage meStorage = storageService.getInventory();

        // 从 ME 网络中提取请求的物品
        if (requestedItem != null) {
            var extracted = meStorage.extract(requestedItem, requestAmount, net.minecraft.world.level.block.entity.BlockEntityWrapper.EMPTY, null);
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

            IItemHandler handler = be.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, dir.getOpposite()).orElse(null);
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

    public void clearNode() {
        if (mainNode != null) {
            mainNode.destroy();
            mainNode = null;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        clearNode();
    }
}
