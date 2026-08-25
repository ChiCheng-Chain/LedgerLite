# LedgerLite

> 业余时间开发的个人记账 App，按自己的使用习惯打磨。

市面上的记账软件要么收费、要么功能臃肿，索性自己写一个够用就好。本地优先、快速记录、界面克制，专注解决两件事：**日常花销记得够快**，**大件消费算得清日均使用成本**。

## 特性

- **快速记账** — 底部弹出计算器面板，输金额、选分类、完成，3 秒一笔
- **流水管理** — 按日分组、每日合计，左滑删除，时间筛选（今天/本周/本月/自定义）
- **资产摊销** — 记录大件消费，自动算日均/周均使用成本，判断一笔大额消费是否值得
- **基础统计** — 本月总支出、分类占比环形图、近 30 天趋势折线、支出热力图
- **分类自管** — 自定义分类的增删改，8 色调色板
- **本地优先** — 数据全部存在手机本地，不上传任何服务器
- **Material You** — 跟随系统深色模式，大圆角卡片，120Hz 适配

## 技术栈

Kotlin · Jetpack Compose · Room · DataStore · Compose Navigation · Coroutines + Flow

零注解 DI（Application 持容器 + ViewModel 内嵌 Factory），单 module 工程，不引入 Hilt。

## 截图

（后续补充）

## 构建

需要 Android Studio + JDK 17+。克隆后直接用 Android Studio 打开 `android/` 目录即可运行。

```bash
cd android
./gradlew :app:assembleDebug     # Debug 构建
./gradlew :app:assembleRelease   # Release 构建（需自行配置 keystore）
```

## 参与贡献

这个项目最初只是给自己用的，但如果你也有类似的需求，欢迎一起完善：

- 发现 bug 或有功能建议 → [提个 Issue](../../issues)
- 想加功能或修 bug → 直接提 PR，简单改动随手就来

## License

MIT
