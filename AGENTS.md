# AGENTS.md

本文件由稳定的个人核心和项目专属信息组成。规则等级如下：

- `[必须]`：不得违反；确需放宽时，在“项目例外”中说明原因和范围。
- `[默认]`：没有充分理由时遵守；偏离时在任务结果中简要说明。
- `[参考]`：提供背景、入口或建议，不作为完成条件。

## 1. 使用与优先级

规则冲突时依次服从：

1. 用户在当前任务中的明确要求；
2. 更靠近目标文件的嵌套 `AGENTS.md`；
3. 本文件的“项目例外”；
4. 本文件的 `[必须]` 规则；
5. 项目已有约定和本文件的 `[默认]` 规则；
6. `[参考]` 内容。

项目已有明确技术约定时，不应仅为满足个人风格而大规模改写现有代码。

## 2. 个人核心原则

### Agent 执行纪律 `[必须]`

- 修改前先阅读与任务直接相关的代码、配置和文档，理解现有约定后再行动。
- 修改范围必须与当前任务直接相关；发现无关问题时记录并说明，不擅自扩大任务范围。
- 不得覆盖、删除或回退来源不明的已有改动。
- 不得静默新增依赖、替换技术方案或修改构建、部署与基础设施配置；确有必要时先说明原因和影响。
- 不得在代码、文档、日志或提交中写入密钥、令牌、密码及其他敏感信息。
- 完成前必须运行与改动相匹配的测试、检查或构建命令；无法运行时明确说明未验证内容和原因。
- 修改了可验证的行为时，应同步新增或更新相应测试；项目缺少适用测试基础时，说明替代验证方式。

### 简单性与设计 `[默认]`

- 优先采用能够完整解决当前需求的最简单方案。
- 简单不等于草率：不得以减少代码量为由牺牲正确性、边界处理、安全性或可维护性。
- 不为未经确认的未来需求预先增加抽象层、扩展点、配置项或通用框架。
- 优先沿用项目已有的结构、模式和工具；不要仅因个人偏好引入新的实现范式。
- 仅当抽象能够消除已经存在的实质重复，或能够明确隔离职责和变化边界时，才引入抽象。
- 函数、类和模块应具有清晰、单一的主要职责；按职责拆分，不按机械行数拆分。
- 大型重构与功能修改原则上分开进行；与当前修改直接相关的小型清理可以一并完成。

### 可读性与代码健康 `[默认]`

- 新代码应让熟悉该技术栈的维护者能够快速理解。
- 命名应准确表达业务含义、职责和数据单位，避免模糊缩写及无意义名称。
- 控制流和数据流应尽量直接；避免不必要的嵌套、隐式副作用和跨层耦合。
- 错误应在合适的边界被处理或向上传递，不得静默吞掉异常或伪造成功结果。
- 注释主要解释“为什么这样做”、外部约束和非显而易见的决策，不重复描述代码本身。
- 每次修改至少保持现有代码健康，不能为了快速完成需求引入明显的临时堆叠。

## 3. Git 提交规范

### 提交行为 `[必须]`

- 只有用户明确要求时才执行 `git commit`。
- 提交前检查暂存区差异，只包含当前提交主题相关的文件。
- 提交前运行项目规定的适用检查；检查未通过时不得将提交描述为已验证。
- 不得提交密钥、令牌、本地环境文件、调试产物或无关改动。

### 提交格式 `[必须]`

使用 `<type>: <中文摘要>`，例如 `feat: 增加用户登录功能`。

- `type` 使用固定英文小写前缀，摘要使用简体中文且末尾不加句号。
- 摘要应描述实际结果，避免“更新代码”“修改问题”等模糊表达。
- 一次提交只表达一个逻辑主题；多类内容使用最能代表主要目的的前缀。
- 可用类型：`feat`、`fix`、`refactor`、`perf`、`docs`、`test`、`style`、`build`、`ci`、`chore`、`revert`。
- 默认不使用 scope；多模块项目可在项目专属信息中启用 `<type>(<scope>): <中文摘要>`。

## 4. 文档规范

### 写作与放置 `[必须]`

- 项目文档默认使用简体中文；代码标识符、命令、路径、配置键和无合适译法的专有名词保留原文。
- 根 `README.md` 提供项目简介、前置条件、快速启动、常用命令和详细文档入口。
- `docs/README.md` 是完整文档索引；根 `README.md` 必须包含指向它的相对链接。
- 仓库级标准文件保留在根目录，其他长期项目文档统一放在 `docs/`。
- 模块专属文档可以放在模块目录，但必须从 `docs/README.md` 建立入口。
- 同一事实只维护一个权威来源，其他位置通过链接引用。
- 新增、移动、重命名或删除文档时，必须在同一次修改中更新 `docs/README.md`。
- 启动、构建、测试、部署、配置、公开接口、数据格式或关键路径变化时，必须检查并同步相关文档。
- 文档不得记录真实敏感值；命令示例应注明运行目录和必要前置条件。

### 命名与内容 `[默认]`

- 文档文件名默认使用小写英文 `kebab-case`，不使用临时性名称。
- 不为满足模板提前创建空目录；按实际内容组织 `getting-started/`、`development/`、`architecture/`、`operations/` 或 `reference/`。
- 文档开头说明用途、适用范围和目标读者。
- 部署文档说明目标环境、配置来源、验证方式和回滚方法。
- 只保留当前有效事实；历史讨论仅在解释重要决策时保留。

## 5. 项目专属信息

### 项目概述

- 项目用途：个人记账 Android App，本地优先，专注快速记账与大件消费的日均/周均使用成本核算。
- 主要使用者：开发者本人及有相似需求的开源用户。
- 当前阶段：早期自用版本（versionName 0.1.0，versionCode 1）。
- 非项目范围：无云端同步、无账号体系、无网络上报；不上架应用商店。

### 技术基线

- 主要语言及版本：Kotlin 2.0.21（权威文件 `android/gradle/libs.versions.toml`）。
- 运行时及版本：Android minSdk 26 / targetSdk 36 / compileSdk 36（权威文件 `android/app/build.gradle`）；构建需 JDK 17+。
- 核心框架：Jetpack Compose（Bom 2024.12.01）、Material3；Navigation Compose 2.8.5；Coroutines 1.9.0 + Flow。
- 包管理器：Gradle 8.10.2（wrapper），版本目录 `android/gradle/libs.versions.toml`。
- 数据存储：Room 2.6.1（本地 SQLite）+ DataStore Preferences 1.1.1；数据全部存手机本地。
- 关键外部服务：无（无网络权限，纯本地）。

### 关键目录

| 路径 | 用途 |
|---|---|
| `android/app/src/main/java/com/ledgerlite/app/` | 应用源码根 |
| `data/local/` | Room 数据库、DAO、实体、关系 |
| `data/repository/` | 仓库层（含 `SettingsRepository` 偏好设置） |
| `di/` | `AppContainer` 进程级组合根（零注解 DI） |
| `domain/model/` | 领域模型（如 `BigItemStatus`） |
| `ui/record/` | 首页记账页 |
| `ui/ledger/` | 流水页与流水编辑面板 |
| `ui/bigitem/` | 大件资产页 |
| `ui/stats/` | 统计页 |
| `ui/settings/` | 设置与分类管理 |
| `ui/components/` | 通用组件（含 `AmountText`、`AmountKeypad`、`DecimalConfig`、图表） |
| `ui/components/charts/` | 折线、对比柱、热力图、环形图 |
| `util/` | `MoneyUtil`、`AmortizationUtil`、`DateUtil` |
| `android/app/src/test/` | JVM 单元测试 |
| `tools/` | 辅助脚本（如 `gen_icon.py` 图标生成） |

### 标准命令

| 操作 | 命令 | 运行目录 | 说明 |
|---|---|---|---|
| 安装依赖 | 无需手动安装 | `android/` | Gradle wrapper 自动拉取；首次需 JDK 17+ |
| 构建 Debug | `./gradlew assembleDebug` | `android/` | 输出 `app/build/outputs/apk/debug/app-debug.apk` |
| 构建 Release | `./gradlew assembleRelease` | `android/` | 输出 `app/build/outputs/apk/release/LedgerLite.apk`；需 `keystore.properties` |
| 运行测试 | `./gradlew :app:testDebugUnitTest` | `android/` | JVM 单测（Robolectric 4.14.1） |
| 代码检查 | `./gradlew lint` | `android/` | Release 构建含 `lintVitalRelease` |
| 类型检查 | 无独立命令 | `android/` | Kotlin 编译即类型检查 |
| 安装到设备 | `adb -s <device> install -r <apk>` | 任意 | 需 Android SDK platform-tools |

- 最小完成检查：`./gradlew assembleDebug` 编译通过；涉及工具类改动时额外运行 `:app:testDebugUnitTest`。

### 配置与环境

- 配置示例文件：无。
- 必需环境变量：无。
- 本地配置方式：`android/local.properties` 指定 SDK 路径；`android/keystore.properties` 指定 release 签名。
- 配置或密钥的安全来源：签名 keystore 与 `keystore.properties` 由本地维护，已加入 `.gitignore`，不入库。
- 本地依赖服务：无。
- 默认端口：无（移动 App）。

### 部署信息

| 环境 | 部署位置 | 触发方式 | 配置来源 | 验证方式 |
|---|---|---|---|---|
| 无 | 无应用商店或 CI/CD | 本地 `assembleRelease` 生成 APK 后手动安装 | `keystore.properties` | 设备上启动 App |

- 部署文档：无。
- 回滚方式：仓库中未确认。
- 发布权限或审批要求：仓库中未确认。

### 特殊约束与高风险区域

- 金额内部一律以 `Long` 分存储，UI 展示才转元字符串；转换统一走 `MoneyUtil`，不得在业务代码里手算分/元。
- 小数显示由全局 `LocalDecimalConfig`（`ui/components/DecimalConfig.kt`）控制，读取 `SettingsRepository.showDecimals` + `decimalPlaces`；金额展示组件应读该 CompositionLocal，不要硬编码小数位数。
- 不引入 Hilt 等注解 DI 框架；依赖注入通过 `AppContainer` + ViewModel 内嵌 `Factory` 手工组装。
- `android/app/build/`、`local.properties`、`keystore.properties`、`*.keystore` 不得入库（见 `.gitignore`）。
- 数据库 schema 变更需同步 `android/app/schemas/`（Room 导出路径，由 `ksp { room.schemaLocation }` 配置）。

### 文档入口

- 项目说明：[`README.md`](README.md)
- 文档索引：无
- 架构文档：无
- 部署与运维文档：无
- API 或配置参考：无

## 6. 项目例外

无
