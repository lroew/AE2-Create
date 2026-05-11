# AE2 Create Compat

**Applied Energistics 2 × Create (机械动力) 自动化兼容 Mod**

为 Minecraft 1.21.1 (NeoForge) 开发的 Mod，实现 AE2 和 Create 之间的自动化物品传输与合成桥接。

## 功能

### 📦 ME Exporter (ME 导出器)
从 AE2 ME 网络中提取物品，通过相邻的 Create 管道/传送带/机械臂输出。
- 连接到 ME 网络后自动工作
- 可配置导出的物品和数量
- 兼容所有 Create 物品传输设备

### 📥 ME Importer (ME 导入器)
从 Create 管道/传送带接收物品并存入 AE2 ME 网络。
- 自动从相邻设备抽取物品
- 支持白名单/黑名单过滤
- 支持指定物品类型过滤

### ⚙️ ME Pattern Provider (ME 合成模式提供器)
通过 Create 机械动力设备驱动 AE2 的自动化合成。
- 从 AE2 合成系统获取合成模式
- 将合成材料推送给 Create 加工设备
- 收集加工产物并回收至 AE2 网络
- 支持 Create 切割、压榨、组装等加工流程

## 兼容的 Create 设备

| Create 设备 | 支持的交互 |
|---|---|
| 传送带 (Belt) | 物品输入/输出 |
| 机械臂 (Mechanical Arm) | 物品分配/收集 |
| 漏斗 (Chute) | 垂直物品传输 |
| 发射器 (Deployer) | 模拟玩家交互 |

## 安装

1. 安装 Minecraft 1.21.1
2. 安装 NeoForge 21.1.x
3. 安装 Applied Energistics 2
4. 安装 Create (机械动力)
5. 将本 Mod JAR 放入 `mods/` 文件夹

## 构建

```bash
./gradlew build
```

构建产物位于 `build/libs/` 目录。

## 使用示例

### 基础物流
```
[ME 网络存储] → [ME Exporter] → [Create 传送带] → [ME Importer] → [ME 网络存储]
```

### 自动化加工
```
[AE2 合成系统] → [ME Pattern Provider] → [Create 滚筒/切割机] → [产物回收至 ME 网络]
```

### 物品分配
```
[ME Exporter] → [Create 机械臂] → [多个目标容器]
```

## 许可证

MIT License
