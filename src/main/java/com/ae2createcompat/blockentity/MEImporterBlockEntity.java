package com.ae2createcompat.blockentity;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
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
 * ME Importer - 从 Create 物品管道/传送带接收物品并存入 AE2 网络。
 *
 * 工作原理：
 * 1. 连接到 AE2 ME 网络
 * 2. 从相邻的 Create 物品管道中抽取物品
 * 3. 将物品注入 ME 存储网络
 */
public class MEImporterBlockEntity extends BlockEntity implements IGridConnectedBlockEntity {

    private final Runnable tickCallback = this::serverTick;
    private final IManagedGridNode mainNode = new ManagedGridNode(this, BlockEntityNodeListener.INSTANCE);
    private int tickCounter = 0;
    private static final int IMPORT_INTERVAL = 5; // 每 5 tick 导入一次
    private boolean whitelistMode = true;
    private AEItemKey filterItem = null;

    public MEImporterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
        tag.putBoolean("whitelistMode", whitelistMode);
        if (filterItem != null) {
            tag.put("filterItem", filterItem.toTag(provider));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        mainNode.loadFromNBT(tag);
        whitelistMode = tag.getBoolean("whitelistMode");
        if (tag.contains("filterItem")) {
            filterItem = AEItemKey.fromTag(provider, tag.getCompound("filterItem"));
        }
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;

        tickCounter++;
        if (tickCounter < IMPORT_INTERVAL) return;
        tickCounter = 0;

        if (!mainNode.isReady()) return;

        IGrid grid = mainNode.getGrid();
        if (grid == null) return;

        IStorageService storageService = grid.getService(IStorageService.class);
        MEStorage meStorage = storageService.getInventory();

        // 从相邻的 Create 管道/传送带中抽取物品
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            var be = level.getBlockEntity(neighborPos);
            if (be == null) continue;

            IItemHandler handler = net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK.getCapability(
                    level, neighborPos, level.getBlockState(neighborPos), be, dir.getOpposite());
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