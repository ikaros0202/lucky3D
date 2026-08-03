# Lucky3D V1 验收报告

> 版本：V0.1.0<br>
> 日期：2026-07-28<br>
> 结论：V1 工程与本地验收 Release 达到 Checkpoint E；可进入“正式签名后内测”决策

## 1. 产品范围结论

实现保持在批准范围内：

- 只支持中国福利彩票 3D 的单选、组选3、组选6。
- 一级入口固定为首页、走势、选号、方案、彩报。
- Room 为运行时唯一事实来源；网络整批校验后才事务写库。
- 无云端、账号、跨设备同步、购彩、支付、兑奖、AI 荐号、社区、导出、分享或文件备份。
- 彩报仅为随包静态占位图。
- 文案保持历史数据分析与方案核对定位，不承诺未来中奖概率。

## 2. 验收结果

| 验收面 | 结果 | 证据 |
| --- | --- | --- |
| 构建 | 通过 | clean 状态 `test + lint + connectedDebugAndroidTest + assembleRelease` |
| 数据 | 通过 | 3334 期结构校验；6 个黄金样例；独立抽样 20/20 |
| 首页/历史 | 通过 | 无网本地优先；最近 5 期 UI 与 seed 5/5 |
| 走势 | 通过 | 30/100 期坐标和统计测试；滚动、缩放、点选、非颜色编码 |
| 选号 | 通过 | 全部 V1 条件、胆拖、冲突/撤销、前导零、组选排列、金额 |
| 方案/模板 | 通过 | 保存、复制、备注、强停重开、迁移、同版本覆盖安装保留 |
| 回测/复盘 | 通过 | 防未来数据泄漏、可复现、样本不足、逐期候选/金额、修正重算 |
| 设置/提醒 | 通过 | 三类独立开关、DataStore、事件去重和遗漏阈值 |
| UI/无障碍 | 通过 | 320dp、411dp、深浅色、1.3× 字体、semantics 与非颜色编码 |
| 安全 | 通过 | INTERNET-only 源权限、禁备份、禁明文、无敏感日志和签名材料 |
| Release 安装 | 通过 | API 26 离线、API 37 主流程、同版本覆盖安装 |

## 3. 性能观察

- 1000 个候选使用 LazyColumn，不一次构建全部节点。
- 全量回测不再按每一期重复计算相同静态候选；候选快照复用有回归测试。
- 回测计算运行在 `Dispatchers.Default`，不阻塞主线程。
- 3304 个有效期的回测结果改为 LazyColumn 逐项呈现。
- API 37 自动化从点击到检测到全量报告不超过 2.56 秒；该值包含一次 UIAutomator dump，属于端到端上界，不是微基准。

## 4. Release 产物

本地验收 APK：

`artifacts/lucky3d-v0.1.0-release.apk`（本地构建产物，不进入 Git）

- 包名：`com.lucky3d.app`
- 版本：`versionCode 1` / `versionName 0.1.0`
- APK SHA-256：`D7AE063A765881181F53C5536D07660938A78F9F9C236602FDA307171A9E0835`
- 本地验收证书 SHA-256：`DDF2E4D899BFCB30986F872409AEF599F3359A90E769F90AD56F12AEB548A6A0`
- v2/v3 签名校验通过。

该证书是一次性本地验收证书，密钥已经删除。APK 可安装，但不能作为未来升级链的正式发布身份。

## 5. 设备与视觉证据

验证设备：

- `XinYue_API26`：Android 8.0 / API 26；临时设为 720×1280、360dpi，即 320dp 宽；用于窄屏与严格无网首启。
- `XinYue_API37`：Android API 37、16 KB 页模拟器；411dp 宽；用于深色、大字体、五入口、保存重开、回测和 Release 流程。

截图目录：`docs/evidence/task17/`。关键文件包括：

- `release-offline-api26.png`
- `release-final-api26.png`
- `release-home-api37.png`
- `release-scheme-reopen-api37.png`
- `home-dark-large-font-api37.png`
- `trend-api37.png`
- `pick-filtered-api37.png`
- `backtest-optimized-api37.png`
- `caibao-dark-large-font-api37.png`

## 6. 已知风险与边界

1. 本轮只有模拟器证据，没有物理真机结论。
2. TalkBack 语义树和标签已验证，但未做真实盲人用户或完整朗读体验研究。
3. 第三方 20 期抽样只能发现问题，不能证明全部 3334 期绝对正确；发生冲突仍以官方公告为准。
4. 福利彩票公开接口没有 SLA；现有策略是保留本地数据、显示最后成功时间和可重试错误。
5. 本轮验证的是同版本覆盖安装和 Room schema 迁移；真实 APK 版本升级要在 `versionCode 2` 时再验收。
6. 当前签名只用于本地验收；正式内测必须使用长期保管的发布密钥重新签名。

## 7. 最终结论

Tasks 11—17、Checkpoint D 与 Checkpoint E 的实现、测试、数据、设备和文档证据已经闭环。下一步不是继续补 V1 待办，而是由产品负责人选择：

- 配置正式发布密钥并进入内测；或
- 基于本轮验收反馈规划 V1.1。
