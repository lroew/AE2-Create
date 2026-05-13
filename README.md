# AE2 Create Compat v2.0

**Applied Energistics 2 × Create (机械动力) 自动化兼容 Mod**

为 Minecraft 1.21.1 (NeoForge) 开发的 Mod，实现 AE2 和 Create 6.0+ 之间的全面自动化物品传输与合成桥接。

## 功能

### 📦 ME Exporter (ME 导出器)
从 AE2 ME 网络中提取物品，通过相邻的 Create 设备输出。
- 支持定向导出（仅朝向面）
- 支持过滤器配置
- 实时显示工作状态和总导出量
- 兼容所有 Create 物品传输和存储设备

### 📥 ME Importer (ME 导入器)
从 Create 设备接收物品并存入 AE2 ME 网络。
- 支持白名单/黑名单过滤
- 支持定向导入
- 实时显示工作状态和总导入量
- 兼容所有 Create 物品传输和加工设备

### ⚙️ ME Pattern Provider (ME 合成模式提供器)
通过 Create 机械动力设备驱动 AE2 的自动化合成。
- 状态机驱动：空闲→推送材料→加工→收集→回收
- 支持所有 Create 加工设备
- 自动从 AE2 合成系统获取待处理模式
- 实时显示工作状态和总合成量

### 📊 ME Stock Keeper (ME 库存监控器) ✨NEW
监控 ME 网络中指定物品的数量，输出红石信号控制 Create 设备。
- 最多同时监控 9 种物品
- 可配置高低阈值
- 根据库存水平输出不同强度的红石信号
- 与 Create 智能漏斗、显示器、火车调度联动

## 兼容的 Create 6.0+ 设备

### 物流传输设备
| Create 设备 | 支持的交互 |
|---|---|
| 传送带 (Belt) | 物品输入/输出 |
| 漏斗 (Chute) | 垂直物品传输 |
| 智能漏斗 (Smart Chute) | 过滤传输，红石控制 |
| 机械臂 (Mechanical Arm) | 物品分配/收集 |
| 物品保险库 (Item Vault) | 大容量中转存储 |
| 转运车 (Contraption) | 移动装置存取 |
| 发射器 (Deployer) | 模拟玩家交互 |

### 加工设备
| Create 设备 | 支持的交互 |
|---|---|
| 滚筒 (Mechanical Press) | 压制/冲压加工 |
| 混合器 (Mechanical Mixer) | 混合搅拌加工 |
| 切割机 (Mechanical Saw) | 切割加工 |
| 烘干架 (Basin + Heater) | 加热加工 |
| 密封风扇 (Encased Fan) | 洗涤/烘干/喷射 |
| 装配器 (Mechanical Crafter) | 高级合成 |
| 翻滚漏斗 (Spout) | 液体注入 |

### 轨道设备 (Create 6.0+)
| Create 设备 | 支持的交互 |
|---|---|
| 火车 (Train) | 车厢物品装卸 |
| 火车站 (Train Station) | 调度控制 |

### 显示与控制设备
| Create 设备 | 支持的交互 |
|---|---|
| 显示板 (Display Board) | 库存信息显示 |
| 红石链接 (Redstone Link) | 无线控制 |

## Create 附属 Mod 支持

| 附属 Mod | 兼容功能 |
|---|---|
| Create: Electric Trains | 电动火车车厢装卸、调度控制 |
| Create: Copycats+ | 模拟方块内部设备识别 |

## 使用示例

### 基础物流
```
[ME 网络] → [ME Exporter] → [传送带] → [ME Importer] → [ME 网络]
```

### 自动化加工
```
[AE2 合成] → [ME Pattern Provider] → [滚筒/切割机/混合器] → [产物回收]
```

### 库存控制自动化
```
[ME Stock Keeper] →(红石信号)→ [智能漏斗/显示板/火车调度]
```

### 火车自动化
```
[ME Exporter] → [火车车厢] → [火车站] → [ME Importer]
         ↑                              ↓
   [ME Stock Keeper] ←(红石信号)→ [火车调度控制]
```

## 安装

1. 安装 Minecraft 1.21.1
2. 安装 NeoForge 21.1.x
3. 安装 Applied Energistics 2 (19.x)
4. 安装 Create 机械动力 6.0+
5. 将本 Mod JAR 放入 `mods/` 文件夹
6. （可选）安装 Create: Electric Trains / Copycats+ 以获得额外兼容功能

## 构建

```bash
./gradlew build
```

构建产物位于 `build/libs/` 目录。

## 语言支持 / Language Support

- 🇨🇳 中文 (zh_cn)
- 🇬🇧 English (en_us)

## 技术架构

- NeoForge Capabilities 桥接 AE2 和 Create 的物品系统
- AE2 IGridConnectedBlockEntity 实现 ME 网络连接
- 状态机模式管理合成流程
- 可选的 Create 附属 mod 检测，缺失时优雅降级
- 中英资源包

## 许可证

MIT License
