# Third-party assets

## `home_crystal_content_shell_redesign.png`

- Purpose: 首页无业务文字的柔光晶体底图；期号、开奖号、指标、云南公告、试机号和状态均由 Compose 实时绘制。
- Creator: 项目设计流程中的 OpenAI 图像生成服务；PNG 保留 C2PA 内容凭证。
- Source: `docs/prototypes/2026-08-12-yunnan-announcement/v2/home-crystal-redesign-raw.png`，与 APK 资源逐字节一致。
- Approved use: 仅作为 Lucky3D 首页 UI 壳；不包含第三方彩报、固定开奖数据或用户内容。
- SHA-256: `06F4927697A68B4C61E0936F5726B44699FA6EAB088EFA16B61DBF2F2D3139F8`。

## `trial/caiba-55125-trial-seed.json`

- Purpose: APK 内置试机号历史基线，首次前台启动后幂等导入 Room，供走势图离线按期读取。
- Source: [55125.cn 2025 年福彩 3D 试机号](https://www.55125.cn/3d/3dsjhcx-2025.htm) 与 [55125.cn 2026 年福彩 3D 试机号](https://www.55125.cn/3d/3dsjhcx-2026.htm)。
- Range: 2025 全年 `2025001—2025351`；2026 截至 2026-08-03 打包快照 `2026001—2026204`。
- Count: 555 条。
- Generated: 2026-08-03，使用 `tools/fetch-caiba-trial-seed.py`；相同期号的完全相同重复行折叠，冲突重复、断号、错误年份或非法三位号码会使构建校验失败。
- SHA-256: `FCEEEE05265703BAFB32EAC24882256C781F7A2FBA7B360443BC26FB10243190`。
- Boundary: 该 JSON 包含文本期号、日期和试机号，不包含第三方图片；数据不写入官方 `draws` 表，也不参与筛选、回测或复盘。

## V1.1 实时彩报边界

- V1.1 彩报来源为牛彩网 A11“彩吧彩报第三版”；图片在用户设备下载后保存在应用私有目录，不提交到 Git，也不打包进 APK。
- 完整图片在公开发布前必须确认来源展示授权；未确认时不得作为公开发行版默认开启能力。
- V1 的 `caibao_placeholder.jpg` 仅保留为已完成历史实现的来源记录，不是 V1.1 彩报内容。

## `caibao_placeholder.jpg`

- Purpose: static, read-only placeholder on the V1 “彩报” page.
- Description: newspapers on a table; it is not a real 福彩彩报 and contains no recommendation data.
- Creator: [Swello](https://unsplash.com/@getswello)
- Source: [A newspaper sitting on top of a white table](https://unsplash.com/photos/Us9-XfhoVjA)
- License: [Unsplash License](https://unsplash.com/license)
- Download URL: `https://images.unsplash.com/photo-1643967254338-475e748b6488?auto=format&fit=max&fm=jpg&q=80&w=1200`
- Downloaded: 2026-07-28
- SHA-256: `2C8C41D8AB72E85FD1904300CD79AC814D805E4141BB9535FDF8748922755FCE`
