package com.ae2createcompat.compat;

import com.ae2createcompat.AE2CreateCompat;

/**
 * AE2 兼容层 - 检测 AE2 Mod 是否加载，并注册相关的兼容处理。
 *
 * 核心兼容功能：
 * 1. ME 网络与 Create 管道系统的桥接
 * 2. AE2 合成系统与 Create 加工设备的联动
 * 3. AE2 存储系统与 Create 物流系统的互通
 */
public class AE2Compat {

    private static boolean ae2Loaded = false;

    public static void init() {
        try {
            Class.forName("appeng.core.AEConfig");
            ae2Loaded = true;
            AE2CreateCompat.LOGGER.info("AE2 detected - Enabling AE2-Create compatibility");
        } catch (ClassNotFoundException e) {
            ae2Loaded = false;
            AE2CreateCompat.LOGGER.warn("AE2 not detected - AE2-Create compatibility disabled");
        }
    }

    public static boolean isAE2Loaded() {
        return ae2Loaded;
    }
}
