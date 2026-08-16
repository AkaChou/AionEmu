---
alwaysApply: true
---

# 通用协作规则

## 语言与工作区

1. 默认使用中文沟通、注释和交付说明；现有英文术语、类名、协议名和日志 key 保持不变。
2. 当前仓库以 `git rev-parse --show-toplevel` 得到的 checkout 根目录为准，不依赖机器绝对路径。
3. 不要混用不同 checkout 的 `target`、运行进程、配置或日志。
4. 开始、测试和交付前检查 `git status --short`，保留用户已有的 dirty 改动。

## 命令与编辑

1. 项目说明和命令示例使用标准系统命令，不依赖个人命令包装器。
2. 精确文本搜索优先使用 `rg`；已知文件或符号时直接读取。
3. 代码位置未知时先使用一次聚焦的 `jbcontext search`，找到目录后改用精确搜索和局部读取。
4. 手工编辑使用 `apply_patch`。不要用脚本覆盖整文件，也不要用 Python 代替简单文件补丁。
5. Maven/Javac 是写入 `target` 的操作，必须串行执行；不要让多个 agent 同时构建或清理。

## 修改边界

1. 只修改完成当前请求所需的文件，不顺手重排 XML、升级依赖、格式化无关代码或修改生成文件。
2. 禁止 `git reset --hard`、`git checkout --`、`git restore` 或其他会丢弃用户改动的命令。
3. 发现重叠改动时，在现有内容上工作；只有无法安全合并时才询问用户。
4. 代码和文档不得包含个人机器绝对路径。使用仓库相对路径、明确参数或环境变量。

## 验证

1. 按改动范围先跑 focused tests，再扩大到共享或生产门禁。
2. 修改任务 catalog 或任务 XML 时，至少运行：

   ```bash
   mvn -q -Dtest=QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest test
   ```

3. 交付前运行 `git diff --check`，检查状态和 diff 文件清单。
4. 需要全量证明时，确认没有其他 Maven/Javac writer，再串行执行 `mvn clean verify`。
5. 不要把并发构建污染的 `target/classes` 或不明进程产生的结果当作可靠证明。

## Git 交付

1. 用户明确要求“提交”时，只暂存本次修改的明确路径，检查 cached diff 后本地 commit。
2. 除非用户明确要求，否则不 push、不创建 PR、不改写远程分支或历史。
3. 提交说明使用简洁的 Conventional Commits 风格。
4. 最终说明包含改动、测试命令与结果、残余风险、commit hash，以及是否 push。

## 构建与运行

1. 常用构建入口为 `mvn test`、`./package.sh`。
2. 不要从可变的 `target/classes` 长时间运行服务。
3. 运行时验证使用打包后的 `target/AionEmu.jar` 或 `aion/` 部署产物，并确认进程、类时间戳和日志属于当前 checkout。
