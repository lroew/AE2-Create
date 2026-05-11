package com.ae2createcompat.blockentity;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.crafting.IPatternDetails;
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

import java.util.List;

/**
 * ME Pattern Provider (机械动力版) - 通过 Create 机械臂等设备驱动 AE2 的合成模式。
 *
 * 工作原理：
 * 1. 连接到 AE2 ME 网络
 * 2. 从 AE2 的合成系统中获取待处理的合成模式
 * 3. 将合成所需的原材料暴露给 Create 的机械臂/传送带来投入
 * 4. Create 设备完成处理后，将产物回收并存入 AE2 网络
 *
 * 这实现了 AE2 自动合成与 Create 自动化生产线的桥接：
 * - AE2 负责合成规划和存储
 * - Create 负责实际的生产加工（如切割、压榨、组装等）
 */
public class MEPatternProviderBlockEntity extends BlockEntity implements IGridConnectedBlockEntity {

    private final Runnable tickCallback = this::serverTick;
    private final IManagedGridNode mainNode = new ManagedGridNode(this, BlockEntityNodeListener.INSTANCE);
    private ItemStack[] inputBuffer = new ItemStack[9]; // 合成输入缓冲区
    private ItemStack[] outputBuffer = new ItemStack[9]; // 合成输出缓冲区
    private boolean isActive = false;
    private int processingTicks = 0;
    private static final int PROCESSING_TIME = 20; // 模拟处理时间

    public MEPatternProviderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        mainNode.setInWorldNode(true);
        clearBuffers();
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

    private void clearBuffers() {
        for (int i = 0; i < inputBuffer.length; i++) {
            inputBuffer[i] = ItemStack.EMPTY;
            outputBuffer[i] = ItemStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        mainNode.saveToNBT(tag);
        tag.putBoolean("isActive", isActive);
        tag.putInt("processingTicks", processingTicks);
        // Save input/output buffers
        CompoundTag inputTag = new CompoundTag();
        for (int i = 0; i < inputBuffer.length; i++) {
            if (!inputBuffer[i].isEmpty()) {
                inputTag.put("slot" + i, inputBuffer[i].save(provider));
            }
        }
        tag.put("inputBuffer", inputTag);

        CompoundTag outputTag = new CompoundTag();
        for (int i = 0; i < outputBuffer.length; i++) {
            if (!outputBuffer[i].isEmpty()) {
                outputTag.put("slot" + i, outputBuffer[i].save(provider));
            }
        }
        tag.put("outputBuffer", outputTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        mainNode.loadFromNBT(tag);
        isActive = tag.getBoolean("isActive");
        processingTicks = tag.getInt("processingTicks");

        CompoundTag inputTag = tag.getCompound("inputBuffer");
        for (int i = 0; i < inputBuffer.length; i++) {
            inputBuffer[i] = inputTag.contains("slot" + i) ?
                    ItemStack.parseOptional(provider, inputTag.getCompound("slot" + i)) : ItemStack.EMPTY;
        }

        CompoundTag outputTag = tag.getCompound("outputBuffer");
        for (int i = 0; i < outputBuffer.length; i++) {
            outputBuffer[i] = outputTag.contains("slot" + i) ?
                    ItemStack.parseOptional(provider, outputTag.getCompound("slot" + i)) : ItemStack.EMPTY;
        }
    }

    /**
     * 服务器端 tick - 管理合成模式的生命周期
     */
    public void serverTick() {
        if (level == null || level.isClientSide) return;

        if (!mainNode.isReady()) return;

        IGrid grid = mainNode.getGrid();
        if (grid == null) return;

        // 阶段1: 尝试将输出缓冲区中的产物注入 AE2 网络
        if (hasItemsInOutputBuffer()) {
            injectOutputsToME(grid);
            return;
        }

        // 阶段2: 处理中的状态 - 等待 Create 设备完成加工
        if (isActive) {
            processingTicks++;
            // 在处理期间，从相邻 Create 设备收集输出
            collectOutputsFromCreate();
            if (processingTicks >= PROCESSING_TIME && isOutputComplete()) {
                isActive = false;
                processingTicks = 0;
            }
            return;
        }

        // 阶段3: 请求新的合成模式并准备输入
        if (!hasItemsInInputBuffer()) {
            requestPatternFromAE(grid);
        }

        // 阶段4: 将输入缓冲区的物品推送给 Create 设备
        if (hasItemsInInputBuffer()) {
            if (pushInputsToCreate()) {
                isActive = true;
                processingTicks = 0;
            }
        }
    }

    /**
     * 从 AE2 合成系统请求下一个待处理的模式
     */
    private void requestPatternFromAE(IGrid grid) {
        try {
            ICraftingService craftingService = grid.getService(ICraftingService.class);
            AE2CreateCompat.LOGGER.debug("Requesting pattern from AE2 crafting service");
            // 实际实现需要通过 AE2 的 IPatternDetails 接口获取模式详情
        } catch (Exception e) {
            AE2CreateCompat.LOGGER.debug("No pending crafting jobs");
        }
    }

    /**
     * 将输入缓冲区的物品推送给相邻的 Create 设备（如机械臂输入端、传送带等）
     */
    private boolean pushInputsToCreate() {
        boolean allPushed = true;
        for (Direction dir : Direction.values()) {
            if (level == null) continue;
            BlockPos neighborPos = worldPosition.relative(dir);
            var be = level.getBlockEntity(neighborPos);
            if (be == null) continue;

            IItemHandler handler = net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK.getCapability(
                    level, neighborPos, level.getBlockState(neighborPos), be, dir.getOpposite());
            if (handler == null) continue;

            for (int i = 0; i < inputBuffer.length; i++) {
                if (inputBuffer[i].isEmpty()) continue;
                ItemStack remainder = ItemHandlerHelper
                        .insertItemStacked(handler, inputBuffer[i], false);
                inputBuffer[i] = remainder;
                if (!remainder.isEmpty()) allPushed = false;
            }
        }
        return allPushed;
    }

    /**
     * 从相邻的 Create 设备收集输出产物
     */
    private void collectOutputsFromCreate() {
        for (Direction dir : Direction.values()) {
            if (level == null) continue;
            BlockPos neighborPos = worldPosition.relative(dir);
            var be = level.getBlockEntity(neighborPos);
            if (be == null) continue;

            IItemHandler handler = net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK.getCapability(
                    level, neighborPos, level.getBlockState(neighborPos), be, dir.getOpposite());
            if (handler == null) continue;

            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack extracted = handler.extractItem(slot, 64, false);
                if (extracted.isEmpty()) continue;
                // 放入输出缓冲区
                for (int i = 0; i < outputBuffer.length; i++) {
                    if (outputBuffer[i].isEmpty() ||
                            ItemStack.isSameItemSameComponents(outputBuffer[i], extracted)) {
                        int space = extracted.getMaxStackSize() - outputBuffer[i].getCount();
                        int toAdd = Math.min(space, extracted.getCount());
                        if (toAdd > 0) {
                            if (outputBuffer[i].isEmpty()) {
                                outputBuffer[i] = extracted.copy();
                                outputBuffer[i].setCount(toAdd);
                            } else {
                                outputBuffer[i].grow(toAdd);
                            }
                            extracted.shrink(toAdd);
                            if (extracted.isEmpty()) break;
                        }
                    }
                }
                // 放不下的放回
                if (!extracted.isEmpty()) {
                    ItemHandlerHelper.insertItemStacked(handler, extracted, false);
                }
            }
        }
    }

    /**
     * 将输出缓冲区的产物注入回 AE2 ME 网络
     */
    private void injectOutputsToME(IGrid grid) {
        IStorageService storageService = grid.getService(IStorageService.class);
        MEStorage meStorage = storageService.getInventory();

        for (int i = 0; i < outputBuffer.length; i++) {
            if (outputBuffer[i].isEmpty()) continue;
            AEItemKey aeKey = AEItemKey.of(outputBuffer[i]);
            if (aeKey == null) continue;

            long inserted = meStorage.insert(aeKey, outputBuffer[i].getCount(), null, null);
            outputBuffer[i].shrink((int) inserted);
            if (outputBuffer[i].isEmpty()) {
                outputBuffer[i] = ItemStack.EMPTY;
            }
        }
    }

    private boolean hasItemsInInputBuffer() {
        for (ItemStack stack : inputBuffer) {
            if (!stack.isEmpty()) return true;
        }
        return false;
    }

    private boolean hasItemsInOutputBuffer() {
        for (ItemStack stack : outputBuffer) {
            if (!stack.isEmpty()) return true;
        }
        return false;
    }

    private boolean isOutputComplete() {
        return hasItemsInOutputBuffer();
    }

    public boolean isActive() {
        return isActive;
    }

    public ItemStack[] getInputBuffer() {
        return inputBuffer;
    }

    public ItemStack[] getOutputBuffer() {
        return outputBuffer;
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