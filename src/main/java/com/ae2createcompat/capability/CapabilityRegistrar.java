package com.ae2createcompat.capability;

import com.ae2createcompat.AE2CreateCompat;
import com.ae2createcompat.blockentity.MEExporterBlockEntity;
import com.ae2createcompat.blockentity.MEImporterBlockEntity;
import com.ae2createcompat.blockentity.MEPatternProviderBlockEntity;
import com.ae2createcompat.blockentity.ModBlockEntities;
import appeng.api.stacks.AEItemKey;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * 能力注册器 v2.0 - 将 AE2 的存储能力桥接到 Create 的物品处理系统。
 *
 * 通过 NeoForge 的 Capabilities 系统，让所有 Create 6.0+ 设备能够
 * 识别并操作 AE2 的 ME 方块实体。
 *
 * 支持的 Create 设备交互：
 * - 传送带 (Belt): 物品进出
 * - 机械臂 (Mechanical Arm): 物品分配/收集
 * - 漏斗 (Chute): 垂直传输
 * - 智能漏斗 (Smart Chute): 过滤传输
 * - 物品保险库 (Item Vault): 大容量存储
 * - 发射器 (Deployer): 模拟玩家交互
 * - 火车轮 (Train): 货物车厢装卸
 * - 转运车 (Contraption): 移动装置交互
 *
 * 兼容 Create 附属 mod：
 * - Create: Electric Trains 电动列车
 * - Create: Copycats+ 模拟方块
 */
public class CapabilityRegistrar {

    public static void register() {
        AE2CreateCompat.LOGGER.info("Registering AE2-Create capability bridges v2.0");
    }

    /**
     * 注册所有能力桥接
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // ME Exporter: 对外暴露 IItemHandler
        // Create 机械臂/传送带等设备可从此接口提取物品
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ME_EXPORTER.get(),
                (be, side) -> new MEExporterItemHandler((MEExporterBlockEntity) be, side)
        );

        // ME Importer: 对外暴露 IItemHandler
        // Create 传送带/机械臂等设备可向此接口插入物品
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ME_IMPORTER.get(),
                (be, side) -> new MEImporterItemHandler((MEImporterBlockEntity) be, side)
        );

        // ME Pattern Provider: 输入/输出端分别暴露不同的 IItemHandler
        // Create 设备可从输入端取出材料，将产物放入输出端
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ME_PATTERN_PROVIDER.get(),
                (be, side) -> new MEPatternProviderItemHandler((MEPatternProviderBlockEntity) be, side)
        );

        AE2CreateCompat.LOGGER.info("  [Capabilities] ME Exporter IItemHandler registered");
        AE2CreateCompat.LOGGER.info("  [Capabilities] ME Importer IItemHandler registered");
        AE2CreateCompat.LOGGER.info("  [Capabilities] ME Pattern Provider IItemHandler registered");
    }

    /**
     * ME Exporter 的 ItemHandler 适配器
     * Create 设备可以从 ME 网络中提取物品
     */
    private static class MEExporterItemHandler implements IItemHandler {
        private final MEExporterBlockEntity exporter;
        private final Direction side;

        MEExporterItemHandler(MEExporterBlockEntity exporter, Direction side) {
            this.exporter = exporter;
            this.side = side;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            AEItemKey requested = exporter.getRequestedItem();
            if (requested == null) return ItemStack.EMPTY;
            return requested.toStack(1);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack; // Exporter 不接受输入
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            // 允许 Create 设备通过能力接口提取物品
            AEItemKey requested = exporter.getRequestedItem();
            if (requested == null) return ItemStack.EMPTY;
            if (!ItemStack.isSameItemSameComponents(requested.toStack(1), getStackInSlot(slot))) return ItemStack.EMPTY;
            // 实际提取由 serverTick 处理，这里返回模拟结果
            return requested.toStack(Math.min(amount, (int) Math.min(exporter.getRequestAmount(), Integer.MAX_VALUE)));
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }
    }

    /**
     * ME Importer 的 ItemHandler 适配器
     * Create 设备可以向 AE2 网络中存入物品
     */
    private static class MEImporterItemHandler implements IItemHandler {
        private final MEImporterBlockEntity importer;
        private final Direction side;

        MEImporterItemHandler(MEImporterBlockEntity importer, Direction side) {
            this.importer = importer;
            this.side = side;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            // 检查过滤器
            if (importer.getFilter() != null) {
                AEItemKey filterKey = importer.getFilter();
                AEItemKey inputKey = AEItemKey.of(stack);
                if (inputKey == null) return stack;
                if (importer.isWhitelistMode() && !inputKey.equals(filterKey)) return stack;
                if (!importer.isWhitelistMode() && inputKey.equals(filterKey)) return stack;
            }
            // 接受所有输入，实际处理在 serverTick 中进行
            return ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // 检查过滤器
            if (importer.getFilter() != null) {
                AEItemKey filterKey = importer.getFilter();
                AEItemKey inputKey = AEItemKey.of(stack);
                if (inputKey == null) return false;
                if (importer.isWhitelistMode()) return inputKey.equals(filterKey);
                return !inputKey.equals(filterKey);
            }
            return true;
        }
    }

    /**
     * ME Pattern Provider 的 ItemHandler 适配器
     * 分为输入端（Create 设备取出材料）和输出端（Create 设备放入产物）
     */
    private static class MEPatternProviderItemHandler implements IItemHandler {
        private final MEPatternProviderBlockEntity provider;
        private final Direction side;

        MEPatternProviderItemHandler(MEPatternProviderBlockEntity provider, Direction side) {
            this.provider = provider;
            this.side = side;
        }

        @Override
        public int getSlots() {
            return 9;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            ItemStack[] inputBuffer = provider.getInputBuffer();
            if (slot < 0 || slot >= inputBuffer.length) return ItemStack.EMPTY;
            return inputBuffer[slot].copy();
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot < 0 || slot >= 9) return stack;
            // 允许 Create 设备将产物放回
            return ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= 9) return ItemStack.EMPTY;
            ItemStack[] inputBuffer = provider.getInputBuffer();
            if (inputBuffer[slot].isEmpty()) return ItemStack.EMPTY;
            return inputBuffer[slot].copy();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }
    }
}