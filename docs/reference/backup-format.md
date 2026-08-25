# 备份文件格式参考

本文档描述 LedgerLite 备份功能生成的 JSON 文件结构，供恢复功能开发与备份文件人工检查使用。适用版本：backupVersion 1。

## 概述

- 备份入口：设置页 →「备份与恢复」→ 备份，经系统文件选择器（SAF）保存为单文件。
- 文件名约定：`LedgerLite-backup-yyyyMMdd-HHmmss.json`（时间为备份时刻）。
- 内容：全部分类、流水（含回收站软删记录）、资产与偏好设置，即应用全部本地数据。
- 编码 UTF-8，`prettyPrint` 缩进格式化，可直接文本对比（diff）。

## JSON 结构

```json
{
  "backupVersion": 1,
  "createdAt": 1740000000000,
  "categories": [ { ...Category } ],
  "expenseRecords": [ { ...ExpenseRecord } ],
  "bigItems": [ { ...BigItem } ],
  "settings": { ...BackupSettings }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `backupVersion` | Int | 备份格式版本，与应用内 `BackupRepository.CURRENT_BACKUP_VERSION` 对应；大于应用支持版本时拒绝恢复 |
| `createdAt` | Long | 备份时刻 epoch 毫秒 |
| `categories` | List | 分类表全量，字段同 Room 实体 `Category`（金额无关） |
| `expenseRecords` | List | 流水表全量，含 `deletedAt != null` 的回收站记录；`amount` 为 Long 分 |
| `bigItems` | List | 资产表全量，`status` 为枚举名（`active`/`ended`） |
| `settings` | Object | 偏好设置五项，见下 |

`settings` 字段（缺省时按默认值恢复）：

| 键 | 类型 | 默认值 | 对应设置项 |
|---|---|---|---|
| `defaultHome` | String | `"record"` | 默认首页 |
| `currencySymbol` | String | `"¥"` | 货币符号 |
| `showDecimals` | Boolean | `true` | 金额显示小数 |
| `decimalPlaces` | Int | `2` | 小数位数（1-2，超出钳制） |
| `recentLimit` | Int | `5` | 首页最近记录数量（1-20，超出钳制） |

## 恢复语义

- 恢复为**全量覆盖**：事务内清空三表后按 categories → expense_records → big_items 顺序重插（保留原 id），成功后写偏好。
- 任一步失败（文件损坏、外键冲突等）整个事务回滚，当前数据不变。
- 恢复前做结构校验：JSON 可解析、`backupVersion` 不高于当前支持、流水/资产引用的分类存在。校验失败直接报错，不弹确认框。
- `backupVersion` 低于当前版本的备份按"未知字段忽略 + 缺失字段取默认值"策略向前兼容。

## 兼容性约定

- 备份格式演进时递增 `backupVersion`，新版本应用必须能读旧版本备份（字段默认值兜底）。
- Room schema 版本与 `backupVersion` 相互独立：实体增删字段不影响 JSON 兼容性判断。
- 序列化配置：`ignoreUnknownKeys = true`、`encodeDefaults = true`、`prettyPrint = true`（见 `BackupRepository`）。
