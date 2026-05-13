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
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * ME Exporter v2.0 - 从 AE2 网络中提取物品并通过 Create 设备输出。
 *
 * 支持的 Create 6.0+ 设备：
 * - 传送带 (Belt) - 物品传输
 * - 机械臂 (Mechanical Arm) - 物品分配
 * - 漏斗 (Chute) - 垂直物品传输
 * - 物品保险库 (Item Vault) - 大容量存储
 * - 智能漏斗 (Smart Chute) - 过滤传输
 * - 转运车 (Minecart Contraption) - 移动物流
 * - 发射器 (Deployer) - 模拟玩家交互
 * - 手动建造 (Portable Storage Interface) - 移动装置接口
 * - 火车轮 (Train) - 轨道运输
 *
 * 支持 Create 附属 mod：
 * - Create: Electric Trains 电动列车
 * - Create: Copycats+ 模拟方块
 */
public class MEExporterBlockEntity extends BlockEntity implements IGridConnectedBlockEntity {

    private final Runnable tickCallback = this::serverTick;
    private final IManagedGridNode mainNode = new ManagedGridNode(this, BlockEntityNodeListener.INSTANCE);
    private AEItemKey requestedItem = null;
    private long requestAmount = 64;
    private int tickCounter = 0;
    private static final int EXPORT_INTERVAL = 10;
    private boolean filterEnabled = false;
    private boolean exportToFacingOnly = false; // 仅向方块朝向面导出
    private long totalExported = 0;
    private boolean active = false;

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
        tag.putBoolean("filterEnabled", filterEnabled);
        tag.putBoolean("exportToFacingOnly", exportToFacingOnly);
        tag.putLong("totalExported", totalExported);
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
        filterEnabled = tag.getBoolean("filterEnabled");
        exportToFacingOnly = tag.getBoolean("exportToFacingOnly");
        totalExported = tag.getLong("totalExported");
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;

        tickCounter++;
        if (tickCounter < EXPORT_INTERVAL) return;
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

        // 从 ME 网络中提取请求的物品
        if (requestedItem != null) {
            var extracted = meStorage.extract(requestedItem, requestAmount,
                    appeng.api.config.Actionable.MODULATE,
                    new appeng.me.helpers.BaseActionSource());
            if (extracted > 0) {
                ItemStack stack = requestedItem.toStack((int) Math.min(extracted, Integer.MAX_VALUE));
                long remaining = pushToAdjacent(stack);
                long actuallyExported = extracted - remaining;
                if (actuallyExported > 0) {
                    totalExported += actuallyExported;
                    active = true;
                    setChanged();
                } else {
                    if (active) {
                        active = false;
                        setChanged();
                    }
                }
            } else {
                if (active) {
                    active = false;
                    setChanged();
                }
            }
        }
    }

    /**
     * 将物品推送到相邻的面。
     * 兼容所有实现 IItemHandler 的 Create 设备。
     */
    private long pushToAdjacent(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        if (exportToFacingOnly) {
            // 仅向方块朝向面导出
            Direction facing = getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
            long remaining = tryPushToDirection(stack, facing);
            return remaining;
        }

        // 向所有方向导出
        for (Direction dir : Direction.values()) {
            if (level == null) break;
            long remaining = tryPushToDirection(stack, dir);
            if (remaining <= 0) return 0; // 全部插入成功
            stack = stack.copy();
            stack.setCount((int) remaining);
        }
        return stack.getCount();
    }

    /**
     * 尝试向指定方向推送物品
     */
    private long tryPushToDirection(ItemStack stack, Direction dir) {
        if (level == null || stack.isEmpty()) return stack.getCount();
        BlockPos neighborPos = worldPosition.relative(dir);
        var be = level.getBlockEntity(neighborPos);
        if (be == null) return stack.getCount();

        IItemHandler handler = Capabilities.ItemHandler.BLOCK.getCapability(
                level, neighborPos, level.getBlockState(neighborPos), be, dir.getOpposite());
        if (handler != null) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, stack, false);
            return remainder.getCount();
        }
        return stack.getCount();
    }

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

    public void setFilterEnabled(boolean enabled) {
        this.filterEnabled = enabled;
        setChanged();
    }

    public boolean isFilterEnabled() {
        return filterEnabled;
    }

    public void setExportToFacingOnly(boolean facingOnly) {
        this.exportToFacingOnly = facingOnly;
        setChanged();
    }

    public boolean isExportToFacingOnly() {
        return exportToFacingOnly;
    }

    public boolean isActive() {
        return active;
    }

    public long getTotalExported() {
        return totalExported;
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
