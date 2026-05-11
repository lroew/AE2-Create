package com.ae2createcompat.blockentity;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
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
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/**
 * ME Importer - 从 Create 物品管道/传送带接收物品并存入 AE2 网络。
 *
 * 工作原理：
 * 1. 连接到 AE2 ME 网络
 * 2. 从相邻的 Create 物品管道中抽取物品
 * 3. 将物品注入 ME 存储网络
 */
public class MEImporterBlockEntity extends BlockEntity implements IGridConnectedBlockEntity {

    private IGridNode mainNode;
    private final InternalInventoryBuffer internalBuffer = new InternalInventoryBuffer();
    private int tickCounter = 0;
    private static final int IMPORT_INTERVAL = 5; // 每 5 tick 导入一次
    private boolean whitelistMode = true;
    private AEItemKey filterItem = null;

    public MEImporterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
        tag.putBoolean("whitelistMode", whitelistMode);
        if (filterItem != null) {
            tag.put("filterItem", filterItem.toTagGeneric());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        whitelistMode = tag.getBoolean("whitelistMode");
        if (tag.contains("filterItem")) {
            filterItem = AEItemKey.fromTagGeneric(tag.getCompound("filterItem"));
        }
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;

        tickCounter++;
        if (tickCounter < IMPORT_INTERVAL) return;
        tickCounter = 0;

        IGrid grid = null;
        IGridNode node = getMainNode();
        if (node != null) {
            grid = node.getGrid();
        }
        if (grid == null) return;

        IStorageService storageService = grid.getCraftingService().getGrid();
        MEStorage meStorage = storageService.getInventory();

        // 从相邻的 Create 管道/传送带中抽取物品
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            var be = level.getBlockEntity(neighborPos);
            if (be == null) continue;

            IItemHandler handler = be.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, dir.getOpposite()).orElse(null);
            if (handler == null) continue;

            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack extracted = handler.extractItem(slot, 64, false);
                if (extracted.isEmpty()) continue;

                AEItemKey aeKey = AEItemKey.of(extracted);
                if (aeKey == null) continue;

                // 检查过滤器
                if (filterItem != null) {
                    if (whitelistMode && !aeKey.equals(filterItem)) continue;
                    if (!whitelistMode && aeKey.equals(filterItem)) continue;
                }

                // 注入到 ME 网络
                long inserted = meStorage.insert(aeKey, extracted.getCount(), null, null);
                if (inserted > 0) {
                    // 如果没有全部注入，把剩余的放回去
                    long remaining = extracted.getCount() - inserted;
                    if (remaining > 0) {
                        ItemStack remainingStack = extracted.copy();
                        remainingStack.setCount((int) remaining);
                        // 尝试放回原处
                        ItemStack notReturned = ItemHandlerHelper.insertItemStacked(handler, remainingStack, false);
                        if (!notReturned.isEmpty()) {
                            // 丢弃溢出的物品（或者放入内部缓存）
                            AE2CreateCompat.LOGGER.warn("Items overflow in ME Importer at {}", worldPosition);
                        }
                    }
                }
            }
        }
    }

    public void setFilter(AEItemKey item) {
        this.filterItem = item;
        setChanged();
    }

    public void toggleWhitelistMode() {
        this.whitelistMode = !this.whitelistMode;
        setChanged();
    }

    public boolean isWhitelistMode() {
        return whitelistMode;
    }

    public AEItemKey getFilter() {
        return filterItem;
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

    /**
     * 内部物品缓冲区 - 用于临时存储待处理的物品
     */
    private static class InternalInventoryBuffer {
        private final ItemStack[] items = new ItemStack[1];

        InternalInventoryBuffer() {
            items[0] = ItemStack.EMPTY;
        }
    }
}
