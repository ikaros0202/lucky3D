# Third-party assets

## 随 APK 保留的界面素材

- `app/src/main/res/drawable-nodpi/home_crystal_navigation_shell.png` 在 `AppNavigation.kt` 的旧导航组件中仍有静态引用，本轮按用户确认的范围保留；当前五入口实际使用主题化 Compose 导航。
- SHA-256: `81530EB1188F39D55AA44234D888656512B1C0B6ED270FA4F565FFA93737A469`。
- 旧首页整页底图已无代码引用，2026-10-02 经用户确认随未使用资源清理；其来源记录仍可从 Git 历史追溯。

## `trial/caiba-55125-trial-seed.json`

- Purpose: APK 内置试机号历史基线，首次前台启动后幂等导入 Room，供走势图离线按期读取。
- Source: [55125.cn 2025 年福彩 3D 试机号](https://www.55125.cn/3d/3dsjhcx-2025.htm) 与 [55125.cn 2026 年福彩 3D 试机号](https://www.55125.cn/3d/3dsjhcx-2026.htm)。
- Range: 2025 全年 `2025001—2025351`；2026 截至 2026-08-03 打包快照 `2026001—2026204`。
- Count: 555 条。
- Generated: 2026-08-03，使用 `tools/fetch-caiba-trial-seed.py`；相同期号的完全相同重复行折叠，冲突重复、断号、错误年份或非法三位号码会使构建校验失败。
- SHA-256: `FCEEEE05265703BAFB32EAC24882256C781F7A2FBA7B360443BC26FB10243190`。
- Boundary: 该 JSON 包含文本期号、日期和试机号，不包含第三方图片；数据不写入官方 `draws` 表，也不参与筛选、回测或复盘。

## 实时彩报边界

- 彩报来源为牛彩网 A11“彩吧彩报第三版”；图片在用户设备下载后保存在应用私有目录，不提交到 Git，也不打包进 APK。
- 完整图片在公开发布前必须确认来源展示授权；未确认时不得作为公开发行版默认开启能力。
