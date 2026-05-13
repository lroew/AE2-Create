package com.ae2createcompat.compat;

import com.ae2createcompat.AE2CreateCompat;

/**
 * AE2 兼容层 v2.0 - 检测 AE2 Mod 是否加载，并注册相关的兼容处理。
 *
 * 核心兼容功能：
 * 1. ME 网络与 Create 管道系统的桥接
 * 2. AE2 合成系统与 Create 加工设备的联动
 * 3. AE2 存储系统与 Create 物流系统的互通
 * 4. AE2 P2P 通道与 Create 设备网络
 * 5. AE2 终端与 Create 显示器
 *
 * 兼容的 AE2 功能：
 * - ME 网络 (ME Network)
 * - ME 接口 (ME Interface)
 * - ME 合成终端 (Crafting Terminal)
 * - ME 存储总线 (Storage Bus)
 * - P2P 通道 (P2P Tunnels)
 * - 合成监视器 (Crafting Monitor)
 * - 自动合成 (Autocrafting)
 */
public class AE2Compat {

    private static boolean ae2Loaded = false;
    private static String ae2Version = "unknown";

    public static void init() {
        try {
            Class<?> clazz = Class.forName("appeng.core.AEConfig");
            ae2Loaded = true;
            AE2CreateCompat.LOGGER.info("AE2 detected - Enabling AE2-Create compatibility");
            detectAE2Version();
            AE2CreateCompat.LOGGER.info("  AE2 version: {}", ae2Version);
        } catch (ClassNotFoundException e) {
            ae2Loaded = false;
            AE2CreateCompat.LOGGER.warn("AE2 not detected - AE2-Create compatibility disabled");
        }
    }

    private static void detectAE2Version() {
        try {
            // 尝试获取 AE2 版本信息
            Class<?> versionClass = Class.forName("appeng.core.AELog");
            ae2Version = "detected";
        } catch (Exception e) {
            ae2Version = "unknown (class detection failed)";
        }
    }

    public static boolean isAE2Loaded() {
        return ae2Loaded;
    }

    public static String getAE2Version() {
        return ae2Version;
    }
}
