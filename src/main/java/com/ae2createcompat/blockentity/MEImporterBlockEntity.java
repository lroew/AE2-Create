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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * ME Importer v2.0 - 从 Create 设备接收物品并存入 AE2 网络。
 *
 * 支持的 Create 6.0+ 设备：
 * - 传送带 (Belt)
 * - 机械臂 (Mechanical Arm)
 * - 漏斗 (Chute)
 * - 智能漏斗 (Smart Chute)
 * - 物品保险库 (Item Vault)
 * - 滚筒 (Press / Mechanical Press)
 * - 混合器 (Mixer)
 * - 切割机 (Mechanical Saw)
 * - 烘干架 (Basin)
 * - 发射器 (Deployer)
 * - 火车轮 (Train) - 货物车厢
 * - 转运车 (Contraption)
 *
 * 支持 Create 附属 mod：
 * - Create: Electric Trains 电动列车
 * - Create: Copycats+ 模拟方块
 */
public class MEImporterBlockEntity extends BlockEntity implements IGridConnectedBlockEntity {

    private final Runnable tickCallback = this::serverTick;
    private final IManagedGridNode mainNode = new ManagedGridNode(this, BlockEntityNodeListener.INSTANCE);
    private int tickCounter = 0;
    private static final int IMPORT_INTERVAL = 5;
    private boolean whitelistMode = true;
    private AEItemKey filterItem = null;
    private long totalImported = 0;
    private boolean active = false;
    private boolean importFromFacingOnly = false;

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
        tag.putLong("totalImported", totalImported);
        tag.putBoolean("importFromFacingOnly", importFromFacingOnly);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        mainNode.loadFromNBT(tag);
        whitelistMode = tag.getBoolean("whitelistMode");
        if (tag.contains("filterItem")) {
            filterItem = AEItemKey.fromTag(provider, tag.getCompound("filterItem"));
        }
        totalImported = tag.getLong("totalImported");
        importFromFacingOnly = tag.getBoolean("importFromFacingOnly");
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;

        tickCounter++;
        if (tickCounter < IMPORT_INTERVAL) return;
        tickCounter = 0;

        if (!mainNode.isReady()) {
            if (active) {
                active = false;
                setChanged();
            }
            return;
        }

        IGrid grid = mainNode.getGrid();
        if (grid == null) return;

        IStorageService storageService = grid.getService(IStorageService.class);
        MEStorage meStorage = storageService.getInventory();

        boolean didImport = false;

        if (importFromFacingOnly) {
            didImport = tryImportFromDirection(meStorage,
                    getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING));
        } else {
            for (Direction dir : Direction.values()) {
                if (tryImportFromDirection(meStorage, dir)) {
                    didImport = true;
                }
            }
        }

        if (didImport != active) {
            active = didImport;
            setChanged();
        }
    }

    /**
     * 尝试从指定方向的设备导入物品
     */
    private boolean tryImportFromDirection(MEStorage meStorage, Direction dir) {
        if (level == null) return false;
        BlockPos neighborPos = worldPosition.relative(dir);
        var be = level.getBlockEntity(neighborPos);
        if (be == null) return false;

        IItemHandler handler = Capabilities.ItemHandler.BLOCK.getCapability(
                level, neighborPos, level.getBlockState(neighborPos), be, dir.getOpposite());
        if (handler == null) return false;

        boolean imported = false;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack extracted = handler.extractItem(slot, 64, false);
            if (extracted.isEmpty()) continue;

            AEItemKey aeKey = AEItemKey.of(extracted);
            if (aeKey == null) continue;

            // 检查过滤器
            if (filterItem != null) {
                if (whitelistMode && !aeKey.equals(filterItem)) {
                    // 不匹配白名单，放回
                    ItemHandlerHelper.insertItemStacked(handler, extracted, false);
                    continue;
                }
                if (!whitelistMode && aeKey.equals(filterItem)) {
                    // 匹配黑名单，放回
                    ItemHandlerHelper.insertItemStacked(handler, extracted, false);
                    continue;
                }
            }

            // 注入到 ME 网络
            long inserted = meStorage.insert(aeKey, extracted.getCount(), null, null);
            if (inserted > 0) {
                totalImported += inserted;
                imported = true;

                // 如果没有全部注入，把剩余的放回去
                long remaining = extracted.getCount() - inserted;
                if (remaining > 0) {
                    ItemStack remainingStack = extracted.copy();
                    remainingStack.setCount((int) remaining);
                    ItemStack notReturned = ItemHandlerHelper.insertItemStacked(handler, remainingStack, false);
                    if (!notReturned.isEmpty()) {
                        AE2CreateCompat.LOGGER.warn("Items overflow in ME Importer at {}", worldPosition);
                    }
                }
            } else {
                // ME 网络空间不足，放回
                ItemHandlerHelper.insertItemStacked(handler, extracted, false);
            }
        }
        return imported;
    }

    public void setFilter(AEItemKey item) {
        this.filterItem = item;
        setChanged();
    }

    public void toggleWhitelistMode() {
        this.whitelistMode = !this.whitelistMode;
        setChanged();
    }

    public void toggleFacingOnlyMode() {
        this.importFromFacingOnly = !this.importFromFacingOnly;
        setChanged();
    }

    public boolean isWhitelistMode() {
        return whitelistMode;
    }

    public AEItemKey getFilter() {
        return filterItem;
    }

    public boolean isActive() {
        return active;
    }

    public long getTotalImported() {
        return totalImported;
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
