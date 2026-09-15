# H2 → MySQL 数据迁移（复现说明）

本目录归档 408真题训练系统从 H2 切换为 MySQL 时使用的脚本与产物，供论文附录或换机复现使用。

## 背景

默认数据库已切换为 MySQL（`backend/src/main/resources/application.yml`，`spring.profiles.active: mysql`）。
H2 profile 保留用于纯本地离线运行。

## 文件说明

| 文件 | 作用 |
| --- | --- |
| `h2_dump.cmd` | 从 H2 导出全量数据为 SQL（输出 `h2_dump.sql`，默认在临时目录，可改路径） |
| `transform.ps1` | 将 H2 dump 转换为 MySQL 可导入脚本（解 `U&'...'` 转义、`TIMESTAMP '...'` → 字符串、表名小写化），输出 `mysql_load.sql` |
| `schema-mysql.sql` | MySQL 8 建表脚本（`backend/src/main/resources/db/schema-mysql.sql` 的副本，应用启动时也会执行） |
| `mysql_load.sql` | 转换后的完整数据导入文件（约 22MB，**已加入 .gitignore，可随时用上面两个脚本重新生成**） |

## 复现步骤

前提：本机 MySQL 8 已启动，MySQL 客户端在 PATH 或使用绝对路径；H2 数据文件位于 `backend/data/exam408.mv.db`。

1. 建库（utf8mb4）：`CREATE DATABASE exam408 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
2. 建表：`mysql -uroot -p --default-character-set=utf8mb4 exam408 < schema-mysql.sql`
3. 导 H2：运行 `h2_dump.cmd`（内部调用 `h2-2.1.214.jar org.h2.tools.Shell`），得到 `h2_dump.sql`
4. 转换：`powershell -NoProfile -ExecutionPolicy Bypass -File transform.ps1`，得到 `mysql_load.sql`
5. 导入：`mysql -uroot -p --default-character-set=utf8mb4 exam408 < mysql_load.sql`
6. 核对：`SELECT COUNT(*) FROM question; -- 应为 846`，`SELECT COUNT(*) FROM sys_user; -- 应为 1`

## 注意

- H2 的 `U&'\xxxx'` 是 ASCII 转义字面量，MySQL 不识别，必须经 `transform.ps1` 解码后才能导入，切勿直接忽略报错。
- 导入会话使用 `SET NAMES utf8mb4; SET SESSION sql_mode='NO_BACKSLASH_ESCAPES';`，保证反斜杠内容原样入库。
- MySQL 驱动已离线可用（`~/.m2/repository/com/mysql/mysql-connector-j/`），无需联网下载。