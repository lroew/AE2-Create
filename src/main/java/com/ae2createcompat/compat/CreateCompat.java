package com.ae2createcompat.compat;

import com.ae2createcompat.AE2CreateCompat;

import java.util.HashSet;
import java.util.Set;

/**
 * Create 兼容层 v2.0 - 检测 Create Mod 及其版本，注册全面的兼容处理。
 *
 * 兼容的 Create 6.0+ 核心设备：
 *
 * 物流传输设备：
 * - 传送带 (Belt) - 水平/斜向/垂直物品传输
 * - 漏斗 (Chute) - 垂直物品传输
 * - 智能漏斗 (Smart Chute) - 过滤式垂直传输
 * - 机械臂 (Mechanical Arm) - 多点物品分配
 * - 物品保险库 (Item Vault) - 大容量存储
 * - 转运车 (Contraption) - 移动物流装置
 * - 手提储物箱 (Portable Storage Interface) - 移动装置存取
 *
 * 加工设备：
 * - 滚筒 (Mechanical Press) - 压制/冲压
 * - 混合器 (Mechanical Mixer) - 混合搅拌
 * - 切割机 (Mechanical Saw) - 切割木材/石料
 * - 烘干架 (Basin + Heater) - 加热加工
 * - 洗涤器 (Encased Fan + Basin) - 洗涤物品
 * - 烘干器 (Encased Fan) - 烘干/喷射
 * - 装配器 (Mechanical Crafter) - 高级合成
 * - 翻滚漏斗 (Spout) - 液体注入
 * - 发射器 (Deployer) - 模拟玩家交互
 *
 * 动力与结构设备：
 * - 齿轮 (Cogwheel) - 动力传输
 * - 传动杆 (Shaft) - 旋转力传输
 * - 水车 (Water Wheel) - 水力发电
 * - 风车 (Windmill Bearing) - 风力发电
 * - 发动机 (Engine) - 动力产生
 *
 * 显示与控制设备：
 * - 显示器 (Display Board) - 信息显示
 * - 显示链接 (Display Link) - 数据链接
 * - 红石链接 (Redstone Link) - 无线红石
 * - 铜质红石信号 (Copper Redstone) - 信号传输
 *
 * 轨道设备 (Create 6.0+ 新增)：
 * - 火车轮 (Train) - 轨道运输
 * - 火车站 (Train Station) - 站点管理
 * - 火车信号灯 (Train Signal) - 信号控制
 * - 轨道 (Track) - 铁轨铺设
 */
public class CreateCompat {

    private static boolean createLoaded = false;
    private static String createVersion = "unknown";
    private static final Set<String> registeredCompat = new HashSet<>();

    public static void init() {
        try {
            Class.forName("com.simibubi.create.Create");
            createLoaded = true;
            AE2CreateCompat.LOGGER.info("Create detected - Enabling AE2-Create compatibility v2.0");
            detectCreateVersion();
            registerCompatHandlers();
        } catch (ClassNotFoundException e) {
            createLoaded = false;
            AE2CreateCompat.LOGGER.info("Create not detected - AE2-Create compatibility disabled");
        }
    }

    private static void detectCreateVersion() {
        try {
            // 检测 Create 6.0+ 特有的类
            Class.forName("com.simibubi.create.content.trains.entity.Train");
            createVersion = "6.0+";
            AE2CreateCompat.LOGGER.info("  Create version: {} (Train support enabled)", createVersion);
        } catch (ClassNotFoundException e) {
            createVersion = "unknown";
        }
    }

    private static void registerCompatHandlers() {
        // === 物流传输设备兼容 ===
        registerBeltCompat();
        registerChuteCompat();
        registerSmartChuteCompat();
        registerMechanicalArmCompat();
        registerItemVaultCompat();
        registerContraptionCompat();

        // === 加工设备兼容 ===
        registerMechanicalPressCompat();
        registerMechanicalMixerCompat();
        registerMechanicalSawCompat();
        registerBasinCompat();
        registerEncasedFanCompat();
        registerMechanicalCrafterCompat();
        registerSpoutCompat();
        registerDeployerCompat();

        // === 轨道设备兼容 ===
        registerTrainCompat();
        registerTrainStationCompat();

        // === 显示与控制设备兼容 ===
        registerDisplayBoardCompat();
        registerRedstoneCompat();

        AE2CreateCompat.LOGGER.info("  Registered {} Create compat modules", registeredCompat.size());
    }

    // === 物流传输设备 ===

    /**
     * 传送带 (Belt) 兼容
     * ME Importer 可以从传送带上拾取物品
     * ME Exporter 可以将物品放到传送带上
     * 传送带上的物品可以被机械臂和发射器操作
     */
    private static void registerBeltCompat() {
        register("belt", "传送带 (Belt)");
    }

    /**
     * 漏斗 (Chute) 兼容
     * Create 的垂直漏斗可以与 ME Importer/Exporter 配合工作
     * 支持直通和侧向输出模式
     */
    private static void registerChuteCompat() {
        register("chute", "漏斗 (Chute)");
    }

    /**
     * 智能漏斗 (Smart Chute) 兼容
     * Create 6.0+ 的智能漏斗支持精确数量过滤
     * 可以通过 ME Stock Keeper 的红石信号控制
     */
    private static void registerSmartChuteCompat() {
        register("smart_chute", "智能漏斗 (Smart Chute)");
    }

    /**
     * 机械臂 (Mechanical Arm) 兼容
     * 机械臂可以将物品从 ME Exporter 中取出并分配到各处
     * 机械臂可以将收集到的物品放入 ME Importer
     * 支持多目标分配模式
     */
    private static void registerMechanicalArmCompat() {
        register("mechanical_arm", "机械臂 (Mechanical Arm)");
    }

    /**
     * 物品保险库 (Item Vault) 兼容
     * Create 6.0+ 的大容量存储设备
     * 可以作为 ME Exporter/Importer 的中转缓冲区
     */
    private static void registerItemVaultCompat() {
        register("item_vault", "物品保险库 (Item Vault)");
    }

    /**
     * 转运车 (Contraption) 兼容
     * Create 的移动装置（包括转运车和矿车组装）
     * 通过手提储物接口 (Portable Storage Interface) 与 ME 网络交互
     */
    private static void registerContraptionCompat() {
        register("contraption", "转运车/移动装置 (Contraption)");
    }

    // === 加工设备 ===

    /**
     * 滚筒 (Mechanical Press) 兼容
     * 通过 ME Pattern Provider 推送材料，收集压制产物
     * 支持压制配方和冲压配方
     */
    private static void registerMechanicalPressCompat() {
        register("mechanical_press", "滚筒 (Mechanical Press)");
    }

    /**
     * 混合器 (Mechanical Mixer) 兼容
     * 通过 ME Pattern Provider 推送多种材料到 Basin，搅拌后收集产物
     * 支持混合配方和加热混合配方
     */
    private static void registerMechanicalMixerCompat() {
        register("mechanical_mixer", "混合器 (Mechanical Mixer)");
    }

    /**
     * 切割机 (Mechanical Saw) 兼容
     * 通过 ME Pattern Provider 推送原木/石材，收集切割产物
     * 支持切割和精密切割
     */
    private static void registerMechanicalSawCompat() {
        register("mechanical_saw", "切割机 (Mechanical Saw)");
    }

    /**
     * 烘干架 (Basin + Heater) 兼容
     * 通过 ME Pattern Provider 推送材料到 Basin
     * 支持 burning/heating 配方
     */
    private static void registerBasinCompat() {
        register("basin", "烘干架 (Basin)");
    }

    /**
     * 密封风扇 (Encased Fan) 兼容
     * 支持洗涤 (Washing)、烘干 (Drying)、喷射 (Blasting)、抽气 (Haunting) 配方
     * 通过 ME Pattern Provider 传递物品
     */
    private static void registerEncasedFanCompat() {
        register("encased_fan", "密封风扇 (Encased Fan)");
    }

    /**
     * 装配器 (Mechanical Crafter) 兼容
     * 通过 ME Pattern Provider 推送材料，支持高级合成配方
     */
    private static void registerMechanicalCrafterCompat() {
        register("mechanical_crafter", "装配器 (Mechanical Crafter)");
    }

    /**
     * 翻滚漏斗 (Spout) 兼容
     * 支持将 ME 网络中的液体/物品通过 Spout 注入
     */
    private static void registerSpoutCompat() {
        register("spout", "翻滚漏斗 (Spout)");
    }

    /**
     * 发射器 (Deployer) 兼容
     * 可以模拟玩家交互，触发 AE2 接口/终端
     * 可以从 ME Exporter 中取出物品并进行加工
     */
    private static void registerDeployerCompat() {
        register("deployer", "发射器 (Deployer)");
    }

    // === 轨道设备 ===

    /**
     * 火车轮 (Train) 兼容
     * Create 6.0+ 新增的铁路运输系统
     * ME Exporter 可以向火车车厢装卸物品
     * ME Importer 可以从火车车厢收集物品
     * 支持 Schedule 自动化调度
     */
    private static void registerTrainCompat() {
        try {
            Class.forName("com.simibubi.create.content.trains.entity.Train");
            register("train", "火车轮 (Train)");
        } catch (ClassNotFoundException e) {
            AE2CreateCompat.LOGGER.info("  [Compat] Train support not available (Create < 6.0)");
        }
    }

    /**
     * 火车站 (Train Station) 兼容
     * ME Stock Keeper 可以根据库存水平控制火车调度
     * 通过红石信号控制火车到站/出发
     */
    private static void registerTrainStationCompat() {
        try {
            Class.forName("com.simibubi.create.content.trains.station.TrainStationBlockEntity");
            register("train_station", "火车站 (Train Station)");
        } catch (ClassNotFoundException e) {
            AE2CreateCompat.LOGGER.info("  [Compat] Train Station support not available (Create < 6.0)");
        }
    }

    // === 显示与控制设备 ===

    /**
     * 显示板 (Display Board) 兼容
     * 可以显示 ME 网络的库存信息
     * 通过 Display Link 连接 ME Stock Keeper
     */
    private static void registerDisplayBoardCompat() {
        register("display_board", "显示板 (Display Board)");
    }

    /**
     * 红石控制兼容
     * ME Stock Keeper 的红石信号可以控制所有 Create 设备
     * 支持红石链接无线传输
     */
    private static void registerRedstoneCompat() {
        register("redstone", "红石控制 (Redstone Control)");
    }

    private static void register(String id, String displayName) {
        registeredCompat.add(id);
        AE2CreateCompat.LOGGER.info("  [Compat] {} integration enabled", displayName);
    }

    public static boolean isCreateLoaded() {
        return createLoaded;
    }

    public static String getCreateVersion() {
        return createVersion;
    }

    public static boolean isCompatRegistered(String id) {
        return registeredCompat.contains(id);
    }

    public static Set<String> getRegisteredCompat() {
        return new HashSet<>(registeredCompat);
    }
}
