# AionEmu 管理控制台前端

AionEmu 管理控制台的前端工程：React 19 + TypeScript + Vite 8 + Tailwind CSS v4。

AionEmu admin console frontend: React 19 + TypeScript + Vite 8 + Tailwind CSS v4.

## 命令 / Commands

| 命令 / Command | 说明 / Description |
|---|---|
| `pnpm install` | 安装依赖 / Install the dependencies |
| `pnpm dev` | 开发服务器，API 代理到 `127.0.0.1:8080` / Dev server, proxying the API to `127.0.0.1:8080` |
| `pnpm build` | 构建产物到 `dist/` / Build the output into `dist/` |
| `pnpm lint` | oxlint 检查 / Run the oxlint checks |

## 与后端的集成 / Integration with the backend

构建产物位于 `web/dist`，由 `pom.xml` 的 `<resources>` 映射到 `target/classes/static`，
最终随 `AionEmu.jar` 一起分发。`<resources>` 声明让 IDE 的增量构建也会同步产物，
而 IDE 增量构建尚未拷贝时，后端会回退到磁盘上的 `web/dist`（页面不会 404）。

The build output lives in `web/dist` and the `<resources>` declaration in `pom.xml` maps it into
`target/classes/static`, so it ships inside `AionEmu.jar`. The declaration also makes the IDE sync the assets
during an incremental build; when an incremental build has not copied them yet, the backend falls back to the
on-disk `web/dist` so the page does not 404.

`scripts/package.sh` 会在 `mvn package` 之前先跑 `pnpm install && pnpm build`。

`scripts/package.sh` runs `pnpm install && pnpm build` before `mvn package`.

开发时用 `pnpm dev` 起 Vite 开发服务器（**http://127.0.0.1:5173**），`/api` 代理到 `127.0.0.1:8080`。

During development run the Vite dev server with `pnpm dev` (**http://127.0.0.1:5173**); it proxies `/api` to
`127.0.0.1:8080`.

## 菜单结构 / Menu structure

控制台菜单是两级：一级是配置分组（管理配置 / 主服配置 / 商城管理），二级是该分组下的配置文件。
分组与配置目录解耦，一个文件可以同时归属多个分组。

The console menu has two levels: the configuration groups at the top and the configuration files of a group
below. The groups are decoupled from the configuration directories, so a file may belong to several groups.

## 国际化 / Internationalization

界面文案走 `src/i18n/dictionaries.ts`，**默认中文**，右上角可切换英文，选择持久化在 `localStorage`。

The UI copy lives in `src/i18n/dictionaries.ts`. **Chinese is the default**; the switch in the top right
changes to English and the choice is persisted in the `localStorage`.

键的备注不另建字典：配置文件里的注释块本身就是双语的（`# GM 面板 / GM Panel`），后端解析后随每个键返回，
界面按当前语言选取对应的一侧。

The key descriptions need no separate dictionary: the comment blocks in the configuration files are already
bilingual (`# GM 面板 / GM Panel`), the backend parses them and returns them with every key, and the UI picks
the side matching the current language.

## 设计约定 / Design conventions

- 暗色单主题贯穿全页，一个强调色（琥珀）锁定全局。
  One dark theme across the whole page, with a single accent color (amber).
- 数字、权限等级与键名一律等宽，保证列对齐。
  Numbers, access levels and keys always use the monospace face so the columns stay aligned.
- 圆角统一 6px 刻度（`--radius-console`）。
  A single 6px radius scale (`--radius-console`).
- 每个键都展示「文件值 / 生效值 / 覆盖来源」，被覆盖的键显式标注，避免「改了没反应」无从排查。
  Every key shows its file value, effective value and override source, so an overridden key is visible and
  a change that would not take effect is never silent.
