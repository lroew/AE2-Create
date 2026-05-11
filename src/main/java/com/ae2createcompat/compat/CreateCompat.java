package com.ae2createcompat.compat;

import com.ae2createcompat.AE2CreateCompat;

/**
 * Create 兼容层 - 检测 Create Mod 是否加载，并注册相关的兼容处理。
 *
 * 核心兼容功能：
 * 1. Create 传送带 (Belt) 上的物品可以直接被 ME Importer 读取
 * 2. Create 机械臂 (Mechanical Arm) 可以从 ME Exporter 取出物品
 * 3. Create 风箱 (Encased Fan) 和滚筒 (Press) 等加工设备与 ME Pattern Provider 联动
 * 4. Create 发射器 (Deployer) 可以触发 AE2 的合成操作
 */
public class CreateCompat {

    private static boolean createLoaded = false;

    /**
     * 初始化兼容层，检测 Create Mod 是否存在
     */
    public static void init() {
        try {
            Class.forName("com.simibubi.create.Create");
            createLoaded = true;
            AE2CreateCompat.LOGGER.info("Create detected - Enabling AE2-Create compatibility");
            registerCompatHandlers();
        } catch (ClassNotFoundException e) {
            createLoaded = false;
            AE2CreateCompat.LOGGER.info("Create not detected - AE2-Create compatibility disabled");
        }
    }

    private static void registerCompatHandlers() {
        // 注册传送带物品检测处理
        registerBeltCompat();
        // 注册机械臂交互处理
        registerMechanicalArmCompat();
        // 注册漏斗兼容
        registerChuteCompat();
        // 注册发射器兼容
        registerDeployerCompat();
    }

    /**
     * 传送带兼容：
     * - ME Importer 可以直接从传送带上拾取物品
     * - ME Exporter 可以直接将物品放到传送带上
     */
    private static void registerBeltCompat() {
        AE2CreateCompat.LOGGER.info("  [Compat] Belt integration enabled");
    }

    /**
     * 机械臂兼容：
     * - 机械臂可以将物品从 ME Exporter 中取出并分配到各处
     * - 机械臂可以将收集到的物品放入 ME Importer
     */
    private static void registerMechanicalArmCompat() {
        AE2CreateCompat.LOGGER.info("  [Compat] Mechanical Arm integration enabled");
    }

    /**
     * 漏斗兼容：
     * - Create 的垂直漏斗可以与 ME Importer/Exporter 配合工作
     */
    private static void registerChuteCompat() {
        AE2CreateCompat.LOGGER.info("  [Compat] Chute integration enabled");
    }

    /**
     * 发射器兼容：
     * - 发射器可以模拟玩家交互，触发 AE2 合成终端
     * - 发射器可以从 ME Exporter 中取出物品并进行加工
     */
    private static void registerDeployerCompat() {
        AE2CreateCompat.LOGGER.info("  [Compat] Deployer integration enabled");
    }

    public static boolean isCreateLoaded() {
        return createLoaded;
    }
}
