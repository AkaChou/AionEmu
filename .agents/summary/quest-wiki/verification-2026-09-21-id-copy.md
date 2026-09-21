# QuestWiki ID/GM 复制按钮验证记录（2026-09-21）

仓库：/Users/mc/IdeaProjects/AionEmu-QuestWiki（未提交）
页面：http://localhost:5190/quest/10525（preview，dist 构建 15:08:40 与源码 15:08:23 对齐）

## 验证项与证据

1. 步骤总览 NPC ID 复制
   - 点击第 1 步 `ID` 芯片 → 展开 `（NPC 806075）` + 出现「复制」按钮
   - 点击「复制」→ 按钮文案变「已复制」，约 1.5s 后复原为「复制」
   - 页面内 spy 记录 `navigator.clipboard.writeText("806075")`
   - 系统剪贴板：`pbpaste` → `806075`
2. 步骤总览物品 ID 复制
   - 第 5 步展开后共 7 个复制按钮（主行 4 个 + 采集进度 3 个）
   - 点击「复制 182216069」→「已复制」，写入 `182216069`
3. GM 命令复制
   - 第 1 步展开 GM → `//quest set 10525 START 0`，复制按钮 →「已复制」，写入该命令
   - 系统剪贴板：`pbpaste` → `//quest set 10525 START 0`
4. dev 服务器（5173）同样通过：`idChip=（NPC 806075）`、`copyLabel=已复制`、写入 `806075`
5. 窄视口（390x844, mobile, touch）全部展开：18 个复制按钮，`docScrollWidth == innerWidth`，行/列表/GM 行均无横向溢出
6. `pnpm lint` 通过；`pnpm test` 7 文件 / 46 用例通过；`pnpm check:data`、`pnpm build` 同轮已通过

## 实现位置

- `src/components/QuestStepsPanel.tsx`：`writeClipboard`（Clipboard API → execCommand 回退）、`EntityIdChip`（ID 芯片 + 复制按钮）、步骤 GM 行复制按钮
- `src/styles.css`：`.quest-steps-id-copy`、`.quest-steps-gm` 样式

## 未覆盖范围（如用户追加再扩展）

「任务阶段 / 服务端状态图 / 客户端对话页面」等 Markdown 章节里的 `NPC 806075（维达）`、`道具 182216072（奥德增强魔力石）` 仍为纯文本，无复制按钮（全库此类 NPC 引用约 22.2 万处，逐处加按钮噪声较大，需先确认交互形式）。
