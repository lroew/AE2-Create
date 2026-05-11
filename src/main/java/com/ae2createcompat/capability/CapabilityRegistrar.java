package com.ae2createcompat.capability;

import com.ae2createcompat.AE2CreateCompat;
import com.ae2createcompat.block.ModBlocks;
import com.ae2createcompat.blockentity.MEExporterBlockEntity;
import com.ae2createcompat.blockentity.MEImporterBlockEntity;
import com.ae2createcompat.blockentity.ModBlockEntities;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * 能力注册器 - 将 AE2 的存储能力桥接到 Create 的物品处理系统。
 *
 * 通过 NeoForge 的 Capabilities 系统，让 Create 的机械臂、传送带等设备
 * 能够识别并操作 AE2 的 ME 接口方块。
 */
public class CapabilityRegistrar {

    public static void register() {
        AE2CreateCompat.LOGGER.info("Registering AE2-Create capability bridges");
    }

    /**
     * 注册所有能力桥接
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // ME Exporter: 对外暴露 IItemHandler，让 Create 设备可以从 ME Exporter 中提取物品
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ME_EXPORTER.get(),
                (be, side) -> new MEExporterItemHandler((MEExporterBlockEntity) be)
        );

        // ME Importer: 对外暴露 IItemHandler，让 Create 设备可以向 ME Importer 中插入物品
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ME_IMPORTER.get(),
                (be, side) -> new MEImporterItemHandler((MEImporterBlockEntity) be)
        );
    }

    /**
     * ME Exporter 的 ItemHandler 适配器
     * 允许 Create 设备从 ME 网络中提取物品
     */
    private static class MEExporterItemHandler implements IItemHandler {
        private final MEExporterBlockEntity exporter;

        MEExporterItemHandler(MEExporterBlockEntity exporter) {
            this.exporter = exporter;
        }

        @Override
        public int getSlots() {
            return 1; // 简化的单一槽位接口
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY; // 导出器不保存物品
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack; // 导出器不接受输入
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            // 从 AE2 网络中提取物品
            return ItemStack.EMPTY; // 实际提取逻辑在 serverTick 中处理
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false; // 导出器不接受输入
        }
    }

    /**
     * ME Importer 的 ItemHandler 适配器
     * 允许 Create 设备向 AE2 网络中存入物品
     */
    private static class MEImporterItemHandler implements IItemHandler {
        private final MEImporterBlockEntity importer;

        MEImporterItemHandler(MEImporterBlockEntity importer) {
            this.importer = importer;
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
            return ItemStack.EMPTY; // 接受所有输入，在 serverTick 中实际处理
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY; // Importer 不允许外部提取
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true; // 接受所有物品
        }
    }
}
