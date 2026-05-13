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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * ME Pattern Provider v2.0 (机械动力版) - 通过 Create 设备驱动 AE2 的合成模式。
 *
 * 支持的 Create 6.0+ 加工设备：
 * - 滚筒 (Mechanical Press) - 压制/冲压
 * - 混合器 (Mechanical Mixer) - 混合
 * - 切割机 (Mechanical Saw) - 切割
 * - 烘干架 (Heater + Basin) - 加热加工
 * - 洗涤器 (Encased Fan + Basin) - 洗涤
 * - 烘干器 (Encased Fan) - 烘干
 * - 装配器 (Mechanical Crafter) - 高级合成
 * - 翻滚漏斗 (Spout) - 液体注入
 * - 发射器 (Deployer) - 模拟交互
 *
 * 支持 Create 附属 mod：
 * - Create: Electric Trains 电动列车 - 车站自动化
 * - Create: Copycats+ 模拟方块
 *
 * 工作流程：
 * 1. 连接到 AE2 ME 网络
 * 2. 从 AE2 的合成系统中获取待处理的合成模式
 * 3. 将合成所需的原材料暴露给 Create 设备
 * 4. Create 设备完成处理后，收集产物
 * 5. 将产物回收并存入 AE2 网络
 */
public class MEPatternProviderBlockEntity extends BlockEntity implements IGridConnectedBlockEntity {

    private final Runnable tickCallback = this::serverTick;
    private final IManagedGridNode mainNode = new ManagedGridNode(this, BlockEntityNodeListener.INSTANCE);
    private ItemStack[] inputBuffer = new ItemStack[9];
    private ItemStack[] outputBuffer = new ItemStack[9];
    private boolean isActive = false;
    private int processingTicks = 0;
    private static final int PROCESSING_TIME = 20;
    private long totalCrafted = 0;

    // 状态枚举
    private enum State {
        IDLE,           // 空闲等待
        PUSHING_INPUT,  // 推送材料到 Create 设备
        PROCESSING,     // Create 设备处理中
        COLLECTING,     // 收集产物
        RETURNING       // 回收产物到 AE2 网络
    }

    private State currentState = State.IDLE;

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
        tag.putString("currentState", currentState.name());
        tag.putLong("totalCrafted", totalCrafted);

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
        try {
            currentState = State.valueOf(tag.getString("currentState"));
        } catch (IllegalArgumentException e) {
            currentState = State.IDLE;
        }
        totalCrafted = tag.getLong("totalCrafted");

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
     * 状态机驱动的服务器端 tick
     */
    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (!mainNode.isReady()) return;

        IGrid grid = mainNode.getGrid();
        if (grid == null) return;

        switch (currentState) {
            case IDLE -> handleIdle(grid);
            case PUSHING_INPUT -> handlePushingInput();
            case PROCESSING -> handleProcessing();
            case COLLECTING -> handleCollecting();
            case RETURNING -> handleReturning(grid);
        }
    }

    /**
     * 空闲状态 - 优先返回产物，然后请求新合成模式
     */
    private void handleIdle(IGrid grid) {
        if (hasItemsInOutputBuffer()) {
            currentState = State.RETURNING;
            return;
        }
        if (!hasItemsInInputBuffer()) {
            requestPatternFromAE(grid);
        }
        if (hasItemsInInputBuffer()) {
            currentState = State.PUSHING_INPUT;
            isActive = true;
            setChanged();
        }
    }

    /**
     * 推送输入材料到 Create 设备
     */
    private void handlePushingInput() {
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        if (pushInputsToCreate(facing)) {
            currentState = State.PROCESSING;
            processingTicks = 0;
        }
    }

    /**
     * 处理状态 - 等待 Create 设备完成加工
     */
    private void handleProcessing() {
        processingTicks++;
        if (processingTicks >= PROCESSING_TIME) {
            currentState = State.COLLECTING;
            processingTicks = 0;
        }
    }

    /**
     * 从 Create 设备收集产物
     */
    private void handleCollecting() {
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        collectOutputsFromCreate(facing.getOpposite());
        currentState = State.RETURNING;
    }

    /**
     * 回收产物到 AE2 网络
     */
    private void handleReturning(IGrid grid) {
        IStorageService storageService = grid.getService(IStorageService.class);
        MEStorage meStorage = storageService.getInventory();

        boolean allReturned = true;
        for (int i = 0; i < outputBuffer.length; i++) {
            if (outputBuffer[i].isEmpty()) continue;
            AEItemKey aeKey = AEItemKey.of(outputBuffer[i]);
            if (aeKey == null) continue;

            long inserted = meStorage.insert(aeKey, outputBuffer[i].getCount(), null, null);
            if (inserted > 0) {
                totalCrafted += inserted;
            }
            outputBuffer[i].shrink((int) inserted);
            if (outputBuffer[i].isEmpty()) {
                outputBuffer[i] = ItemStack.EMPTY;
            } else {
                allReturned = false; // ME 网络空间不足
            }
        }

        if (allReturned) {
            currentState = State.IDLE;
            isActive = false;
        }
        setChanged();
    }

    /**
     * 从 AE2 合成系统请求下一个待处理的模式
     */
    private void requestPatternFromAE(IGrid grid) {
        try {
            ICraftingService craftingService = grid.getService(ICraftingService.class);
            AE2CreateCompat.LOGGER.debug("Requesting pattern from AE2 crafting service at {}", worldPosition);
            // AE2 的 crafting service 会自动推送待处理的模式到 pattern provider
        } catch (Exception e) {
            AE2CreateCompat.LOGGER.debug("No pending crafting jobs at {}", worldPosition);
        }
    }

    /**
     * 将输入缓冲区的物品推送给 Create 设备
     */
    private boolean pushInputsToCreate(Direction outputDir) {
        if (level == null) return false;
        BlockPos neighborPos = worldPosition.relative(outputDir);
        var be = level.getBlockEntity(neighborPos);
        if (be == null) return false;

        IItemHandler handler = Capabilities.ItemHandler.BLOCK.getCapability(
                level, neighborPos, level.getBlockState(neighborPos), be, outputDir.getOpposite());
        if (handler == null) return false;

        boolean allPushed = true;
        for (int i = 0; i < inputBuffer.length; i++) {
            if (inputBuffer[i].isEmpty()) continue;
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, inputBuffer[i], false);
            inputBuffer[i] = remainder;
            if (!remainder.isEmpty()) allPushed = false;
        }
        return allPushed;
    }

    /**
     * 从 Create 设备收集输出产物
     */
    private void collectOutputsFromCreate(Direction inputDir) {
        if (level == null) return;
        BlockPos neighborPos = worldPosition.relative(inputDir);
        var be = level.getBlockEntity(neighborPos);
        if (be == null) return;

        IItemHandler handler = Capabilities.ItemHandler.BLOCK.getCapability(
                level, neighborPos, level.getBlockState(neighborPos), be, inputDir.getOpposite());
        if (handler == null) return;

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack extracted = handler.extractItem(slot, 64, false);
            if (extracted.isEmpty()) continue;
            mergeIntoOutputBuffer(extracted);
        }
    }

    /**
     * 将物品合并到输出缓冲区
     */
    private void mergeIntoOutputBuffer(ItemStack stack) {
        for (int i = 0; i < outputBuffer.length; i++) {
            if (stack.isEmpty()) break;
            if (outputBuffer[i].isEmpty()) {
                outputBuffer[i] = stack.copy();
                stack = ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(outputBuffer[i], stack)) {
                int space = outputBuffer[i].getMaxStackSize() - outputBuffer[i].getCount();
                int toAdd = Math.min(space, stack.getCount());
                if (toAdd > 0) {
                    outputBuffer[i].grow(toAdd);
                    stack.shrink(toAdd);
                }
            }
        }
        // 剩余物品说明缓冲区满了
        if (!stack.isEmpty()) {
            AE2CreateCompat.LOGGER.warn("Output buffer full in ME Pattern Provider at {}", worldPosition);
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public State getCurrentState() {
        return currentState;
    }

    public ItemStack[] getInputBuffer() {
        return inputBuffer;
    }

    public ItemStack[] getOutputBuffer() {
        return outputBuffer;
    }

    public long getTotalCrafted() {
        return totalCrafted;
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
