# 独立仓库与共享资源决策

日期：2026-10-11。状态：用户批准。

Lucky3D 与叁序分别维护源码、任务、发布记录及 Git 历史；原 Lucky3D 远程身份保持不变，叁序使用独立私有 GitHub 仓库。共享资源使用第三个私有仓库 fc3d-resources。

本地三个仓库为相邻目录。共享仓库只检出一份，两个项目的 shared-resources.json 固定相同提交；运行 `powershell -ExecutionPolicy Bypass -File tools/setup-shared-resources.ps1` 初始化或核验。共享版本不匹配或存在未提交修改时停止，不自动覆盖。新电脑需有两个私有仓库的访问权限。

相同的福彩开奖采集、试机号采集、数据校验实现与黄金样例只在共享仓库维护；项目 tools 下保留参数明确的包装入口。黄金样例路径为相邻 fc3d-resources/testdata/fc3d-golden-cases.json，不再复制到应用仓库。

两个项目现有开奖种子和试机号种子字节不同，分别属于已批准版本快照；预置数据库的表结构也不同，不能合并。它们继续归各应用所有。业务代码、UI、数据库迁移、APK与签名独立，不跨项目写入。

历史 Git 提交保持原样，旧提交中已有的重复资源不会通过改写历史删除。独立提交当前工作树不代表新 UI 已通过编译或业务验收。本次不发布 Release、不更新历史指纹来制造验收通过。

SanXu tools/verify.py 默认独立验证自身数据、数据库与依赖方向；旧项目指纹审计通过 --audit-original 显式启用。Lucky3D 后续独立变化会使历史审计失败，历史差异及快照保持不变。两个项目的 validate-predevelopment-data.ps1 独立验证各自数据与共享黄金样例。
