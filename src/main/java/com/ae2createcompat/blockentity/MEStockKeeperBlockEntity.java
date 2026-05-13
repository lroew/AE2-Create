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
import org.jetbrains.annotations.Nullable;

/**
 * ME Stock Keeper v2.0 - ME 网络库存监控器。
 *
 * 功能：
 * - 监控 ME 网络中指定物品的数量
 * - 根据库存水平输出红石信号
 * - 与 Create 智能漏斗、显示器等红石控制设备联动
 * - 支持多物品监控（最多 9 个物品槽位）
 *
 * 使用场景：
 * - 当 ME 网络中某种物品数量低于阈值时，通过红石信号启动 Create 生产线
 * - 当数量达到上限时，通过红石信号停止生产线
 * - 与 Create 显示器联动显示库存信息
 */
public class MEStockKeeperBlockEntity extends BlockEntity implements IGridConnectedBlockEntity {

    private final Runnable tickCallback = this::serverTick;
    private final IManagedGridNode mainNode = new ManagedGridNode(this, BlockEntityNodeListener.INSTANCE);
    private int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20; // 每秒检查一次

    // 监控配置
    private AEItemKey[] monitoredItems = new AEItemKey[9];
    private long[] thresholds = new long[9]; // 低阈值
    private long[] upperThresholds = new long[9]; // 高阈值
    private int redstonePower = 0;

    // 当前库存状态
    private long[] currentStock = new long[9];

    public MEStockKeeperBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        mainNode.setInWorldNode(true);
        for (int i = 0; i < upperThresholds.length; i++) {
            upperThresholds[i] = Long.MAX_VALUE; // 默认无上限
        }
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
        tag.putInt("redstonePower", redstonePower);

        CompoundTag itemsTag = new CompoundTag();
        CompoundTag thresholdsTag = new CompoundTag();
        CompoundTag upperThresholdsTag = new CompoundTag();
        for (int i = 0; i < 9; i++) {
            if (monitoredItems[i] != null) {
                itemsTag.put("item" + i, monitoredItems[i].toTag(provider));
                thresholdsTag.putLong("threshold" + i, thresholds[i]);
                upperThresholdsTag.putLong("upper" + i, upperThresholds[i]);
            }
        }
        tag.put("monitoredItems", itemsTag);
        tag.put("thresholds", thresholdsTag);
        tag.put("upperThresholds", upperThresholdsTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        mainNode.loadFromNBT(tag);
        redstonePower = tag.getInt("redstonePower");

        CompoundTag itemsTag = tag.getCompound("monitoredItems");
        CompoundTag thresholdsTag = tag.getCompound("thresholds");
        CompoundTag upperThresholdsTag = tag.getCompound("upperThresholds");
        for (int i = 0; i < 9; i++) {
            if (itemsTag.contains("item" + i)) {
                monitoredItems[i] = AEItemKey.fromTag(provider, itemsTag.getCompound("item" + i));
                thresholds[i] = thresholdsTag.getLong("threshold" + i);
                upperThresholds[i] = upperThresholdsTag.getLong("upper" + i);
            }
        }
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        if (!mainNode.isReady()) {
            updateRedstone(0);
            return;
        }

        IGrid grid = mainNode.getGrid();
        if (grid == null) return;

        IStorageService storageService = grid.getService(IStorageService.class);
        MEStorage meStorage = storageService.getInventory();

        int maxPower = 0;
        for (int i = 0; i < 9; i++) {
            if (monitoredItems[i] == null) {
                currentStock[i] = 0;
                continue;
            }

            // 查询当前库存
            var available = meStorage.extract(monitoredItems[i], Long.MAX_VALUE,
                    appeng.api.config.Actionable.SIMULATE,
                    new appeng.me.helpers.BaseActionSource());
            currentStock[i] = available;

            // 计算红石信号强度
            if (available <= 0) {
                // 物品不存在，最大信号
                maxPower = 15;
            } else if (available < thresholds[i]) {
                // 低于低阈值
                int power = 15 - (int) Math.min(14, available * 14 / Math.max(1, thresholds[i]));
                maxPower = Math.max(maxPower, power);
            } else if (available >= upperThresholds[i]) {
                // 达到或超过高阈值，无信号
            } else {
                // 在阈值范围内，弱信号
                maxPower = Math.max(maxPower, 1);
            }
        }

        updateRedstone(maxPower);
    }

    private void updateRedstone(int power) {
        if (power != redstonePower) {
            redstonePower = power;
            if (level != null) {
                level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            }
            setChanged();
        }
    }

    /**
     * 获取当前红石信号强度（供外部查询）
     */
    public int getOutputSignal() {
        return redstonePower;
    }

    /**
     * 设置监控的物品
     */
    public void setMonitoredItem(int slot, AEItemKey item, long lowThreshold, long upperThreshold) {
        if (slot < 0 || slot >= 9) return;
        monitoredItems[slot] = item;
        thresholds[slot] = lowThreshold;
        upperThresholds[slot] = upperThreshold;
        setChanged();
    }

    public AEItemKey getMonitoredItem(int slot) {
        if (slot < 0 || slot >= 9) return null;
        return monitoredItems[slot];
    }

    public long getCurrentStock(int slot) {
        if (slot < 0 || slot >= 9) return 0;
        return currentStock[slot];
    }

    public long getThreshold(int slot) {
        if (slot < 0 || slot >= 9) return 0;
        return thresholds[slot];
    }

    public long getUpperThreshold(int slot) {
        if (slot < 0 || slot >= 9) return Long.MAX_VALUE;
        return upperThresholds[slot];
    }

    public int getRedstonePower() {
        return redstonePower;
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