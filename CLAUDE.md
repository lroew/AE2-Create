# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个Minecraft 1.21.1 NeoForge模组，名为"AE2 Create Compat"，用于桥接Applied Energistics 2 (AE2)和Create(机械动力)两个模组之间的物品传输和自动化合成功能。

## 构建与开发

### 构建命令
- `./gradlew build` - 构建项目，生成JAR文件到`build/libs/`
- `./gradlew runClient` - 运行Minecraft客户端进行测试
- `./gradlew runServer` - 运行Minecraft服务器进行测试
- `./gradlew runData` - 生成数据文件到`src/generated/resources/`

### 开发环境要求
- Java 21
- Gradle (项目包含gradle wrapper)
- Minecraft 1.21.1
- NeoForge 21.1.228

### 依赖版本
- AE2: 19.2.17 (编译时依赖)
- Create: 6.0.10-280 (运行时依赖)
- 项目使用Gradle版本管理，依赖定义在`build.gradle`和`gradle.properties`中

## 代码架构

### 主要模块
1. **核心入口** - `AE2CreateCompat.java`
   - 模组主类，处理初始化、注册和事件总线

2. **方块系统** - `com.ae2createcompat.block`
   - `ModBlocks.java` - 方块注册器
   - 定义了三个核心方块：ME导出器、ME导入器、ME合成模式提供器

3. **方块实体系统** - `com.ae2createcompat.blockentity`
   - `ModBlockEntities.java` - 方块实体注册器
   - `MEExporterBlockEntity.java` - 从AE2网络导出物品到Create设备
   - `MEImporterBlockEntity.java` - 从Create设备导入物品到AE2网络
   - `MEPatternProviderBlockEntity.java` - 处理AE2合成模式与Create加工设备交互
   - `BlockEntityTickHandler.java` - 处理方块实体逻辑

4. **兼容性层** - `com.ae2createcompat.compat`
   - `AE2Compat.java` - AE2 API交互封装
   - `CreateCompat.java` - Create API交互封装和初始化

5. **能力系统** - `com.ae2createcompat.capability`
   - `CapabilityRegistrar.java` - 注册AE2和Create之间的能力桥接
   - `CapabilityEventHandler.java` - 处理能力相关事件

6. **物品系统** - `com.ae2createcompat.item`
   - `ModItems.java` - 物品注册器

7. **配方系统** - `com.ae2createcompat.recipe`
   - `ModRecipes.java` - 配方类型和序列化器注册

### 关键技术特点
1. **AE2网络集成**：通过AE2的内部API直接访问ME网络存储和合成系统
2. **Create设备兼容**：支持传送带、机械臂、漏斗、发射器等Create设备
3. **能力系统**：使用NeoForge的能力系统在不同模组间传递物品和流体
4. **事件驱动**：基于NeoForge事件总线处理游戏逻辑

### 设计模式
- 注册器模式：每个子系统有自己的注册类(ModBlocks, ModItems等)
- 桥接模式：通过兼容性层连接AE2和Create的API
- 观察者模式：通过事件处理器响应游戏事件

## 开发注意事项

### 代码约定
- 包结构按功能模块划分
- 方块实体命名遵循`ME[功能]BlockEntity`模式
- 使用SLF4J进行日志记录，通过`AE2CreateCompat.LOGGER`访问
- 中文注释用于重要功能说明

### 调试与测试
- 客户端测试使用`runClient`任务
- 服务器测试使用`runServer`任务
- 日志级别可通过`forge.logging.console.level`系统属性配置

### 资源管理
- 主要资源路径：`src/main/resources/`
- 生成资源路径：`src/generated/resources/`
- 构建产物位置：`build/libs/ae2createcompat-1.0.0.jar`

### CI/CD流程
- 使用.cnb.yml配置构建流水线
- GitHub Actions工作流定义在`.github/workflows/build.yml`
- 自动构建和发布到GitHub Releases

## 常见任务

### 添加新方块
1. 在`ModBlocks.java`中注册方块
2. 在`ModBlockEntities.java`中注册对应的方块实体
3. 创建方块实体类实现功能逻辑
4. 在`ModItems.java`中注册对应的物品形式
5. 添加方块模型和材质到资源目录

### 调试方块实体逻辑
1. 在方块实体类中添加日志输出
2. 使用`runClient`启动游戏测试
3. 查看控制台日志输出
4. 使用调试器连接到运行的Minecraft实例

### 修改依赖版本
1. 更新`gradle.properties`中的版本号
2. 运行`./gradlew build`验证构建
3. 测试兼容性功能