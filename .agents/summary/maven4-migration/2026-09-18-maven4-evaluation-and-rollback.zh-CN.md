# Maven 4.0.0-rc-6 评估与回退记录

- 日期：2026-09-18
- **结论：已回退。本仓库继续使用 Maven 3.9.16（`/opt/homebrew/bin/mvn`），不再使用 Maven 4 RC。**
- 回退决定由用户作出；回退后工作区与迁移前一致（`.mvn/`、`mvnw`、`mvnw.cmd` 均已移除，`.gitignore` 与 memory-bank ENV-002 已还原）。

## 评估期实测数据（供将来复议）

| 项 | 结果 |
|---|---|
| 兼容性体检 | `mvnup check` 6 类策略全绿，`pom.xml` 无需改动 |
| 构建（rc-6 实测） | `-N validate` 3.4s；`test-compile` 19.7s；`-DskipTests package` 31.6s（spring-boot repackage 正常）；全量 `test` 3445 用例 / 0 失败 / 0 错误 / 2 跳过 / 3m31s |
| 启动开销 | `mvn -v`：3.9.16 **0.37s** vs 4.0.0-rc-6 0.63s（经 wrapper 0.87s）→ 无速度收益 |
| 告警差异 | Maven 4 报 `Version not locked for default bindings plugins [maven-resources-plugin]`；当日 3.3.1 与 3.4.0 都被解析过，暴露 POM 版本漂移隐患 |
| 模型差异 | `maven.config` 中 `${maven.multiModuleProjectDirectory}`：3.9.16 可插值，rc-6 **不可**（生成字面量目录并报 `Not fully interpolated local repository`） |
| 版本状态 | rc-6 属 RC 未 GA，Apache 官方推荐版本仍为 3.9.16 |

## 回退动作（2026-09-18 21:43）

以下内容已移入 `~/.Trash/maven4-rollback-20260918/`，可随时恢复：
`project-.mvn`（wrapper 配置 + `maven.config` + 84MB 项目内依赖仓库）、`project-mvnw`、`project-mvnw.cmd`、`workspace-maven-4.0.0-rc-6`（发行版 17MB）、`m2-wrapper-dists-apache-maven-4.0.0-rc-6`（wrapper 缓存 17MB）。

仓库侧：`.gitignore` 与 `.agents/memory-bank/patterns/build-and-env.md` 已用 HEAD 版本覆盖还原；依赖仓库回到 `~/.m2/repository`；`mvn -v` 仍为 3.9.16。

## 若将来要重新启用

1. 下载 `https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/4.0.0-rc-6/apache-maven-4.0.0-rc-6-bin.tar.gz`（sha512 见同目录的 `.sha512` 文件）
2. 解压到 `/Users/mc/Workspace/maven/4.0.0-rc-6`
3. 或直接从上述 Trash 目录恢复；上表评估结论可直接复用
