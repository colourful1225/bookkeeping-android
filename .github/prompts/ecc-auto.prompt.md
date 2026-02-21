---
agent: agent
---
请在 `ecc-migrated/` 范围内自动选择最合适的 1-2 个技能并执行。

## 执行规则
1. 只从 `ecc-migrated/skills/` 选择技能。
2. 若需求不明确，只问 1 个澄清问题；明确则直接执行。
3. 仅使用 canonical 技能名称进行选择与输出。
4. 回复开头必须输出：
   - **已选技能**: `ecc-migrated/skills/<a>`, `ecc-migrated/skills/<b>`
   - **引用规则**: `ecc-migrated/rules/<path>`
   - **引用命令**: `ecc-migrated/commands/<name>`（无则填“无”）
   - **选择原因**: <一句话>
5. 实施时默认引用：
   - `ecc-migrated/rules/common/coding-style.md`
   - `ecc-migrated/rules/common/security.md`
   - `ecc-migrated/rules/common/testing.md`

## 技能完整映射（ecc-migrated/skills）

### 1) 工程核心（优先）
| 意图 | 技能 |
|---|---|
| REST API / 接口规范 | `api-design` |
| 后端架构 / 服务层模式 | `backend-patterns` |
| 编码规范 / 代码风格 | `coding-standards` |
| ECC 环境配置 | `configure-ecc` |
| E2E 测试 / 端到端验证 | `e2e-testing` |
| 评测框架 / 质量评估 | `eval-harness` |
| 前端架构模式 | `frontend-patterns` |
| 检索增强 / RAG 迭代 | `iterative-retrieval` |
| PostgreSQL 设计优化 | `postgres-patterns` |
| 安全评审 | `security-review` |
| 安全扫描 / 依赖检查 | `security-scan` |
| TDD 测试驱动 | `tdd-workflow` |
| 结果验证闭环 | `verification-loop` |
| LLM 成本优化 | `cost-aware-llm-pipeline` |
| 持续学习机制 | `continuous-learning-v2` |

### 2) 开发与交付
| 意图 | 技能 |
|---|---|
| 前端页面/视觉实现 | `frontend-design` |
| 复杂 React 组件构建 | `web-artifacts-builder` |
| Web 流程测试 | `webapp-testing` |
| MCP 服务开发 | `mcp-builder` |
| 技能创建 | `skill-creator` |
| 技能分享与复用 | `skill-share` |
| 发布说明 / 变更日志 | `changelog-generator` |

### 3) 文档与办公处理
| 意图 | 技能 |
|---|---|
| 技术文档共创 | `doc-coauthoring` |
| Word 文档处理 | `docx` |
| PDF 处理 | `pdf` |
| PPT 处理 | `pptx` |
| Excel 处理 | `xlsx` |
| 企业文档流程处理 | `nutrient-document-processing` |

### 4) 业务运营
| 意图 | 技能 |
|---|---|
| 内容研究写作 | `content-research-writer` |
| 线索研究 | `lead-research-assistant` |
| 竞品广告分析 | `competitive-ads-extractor` |
| 开发者成长分析 | `developer-growth-analysis` |
| 会议洞察分析 | `meeting-insights-analyzer` |
| 定制简历优化 | `tailored-resume-generator` |

### 5) 创意品牌
| 意图 | 技能 |
|---|---|
| 算法艺术 | `algorithmic-art` |
| 品牌规范 | `brand-guidelines` |
| 海报/视觉设计 | `canvas-design` |
| 主题体系 | `theme-factory` |
| 图片增强 | `image-enhancer` |
| Slack GIF 生成 | `slack-gif-creator` |

### 6) 效率工具
| 意图 | 技能 |
|---|---|
| 文件整理 | `file-organizer` |
| 发票整理 | `invoice-organizer` |
| 域名脑暴 | `domain-name-brainstormer` |
| LangSmith 数据抓取 | `langsmith-fetch` |
| 抽奖器 | `raffle-winner-picker` |
| Twitter 算法优化 | `twitter-algorithm-optimizer` |
| 视频下载处理 | `video-downloader` |
| 外部应用自动化连接 | `connect-apps` |

## 可用命令（ecc-migrated/commands）
`e2e.md` | `eval.md` | `evolve.md` | `instinct-export.md` | `instinct-import.md` | `instinct-status.md` | `skill-create.md` | `tdd.md` | `test-coverage.md` | `verify.md`

## 命令映射参考（场景 -> 命令）
| 场景 | 命令 |
|---|---|
| E2E 执行 | `e2e.md` |
| 评测/打分 | `eval.md` |
| 方案迭代 | `evolve.md` |
| TDD 循环 | `tdd.md` |
| 覆盖率提升 | `test-coverage.md` |
| 结果核验 | `verify.md` |
| 新技能创建 | `skill-create.md` |
| 经验导出 | `instinct-export.md` |
| 经验导入 | `instinct-import.md` |
| 经验状态查询 | `instinct-status.md` |

## 可用 Agent（ecc-migrated/agents）
`build-error-resolver.md` | `e2e-runner.md` | `security-reviewer.md` | `tdd-guide.md`

## Agent 映射参考（场景 -> Agent）
| 场景 | Agent |
|---|---|
| 构建失败/编译错误 | `build-error-resolver.md` |
| E2E 测试自动化 | `e2e-runner.md` |
| 安全审查/漏洞分析 | `security-reviewer.md` |
| TDD 红绿重构 | `tdd-guide.md` |

## 规则映射参考（任务类型 -> 规则）
| 任务类型 | 必选规则 | 可选增强规则 |
|---|---|---|
| 通用开发任务 | `ecc-migrated/rules/common/coding-style.md` + `ecc-migrated/rules/common/security.md` + `ecc-migrated/rules/common/testing.md` | `ecc-migrated/rules/common/patterns.md` / `ecc-migrated/rules/common/performance.md` |
| Git/提交流程相关 | 上述 3 条 common 基线 | `ecc-migrated/rules/common/git-workflow.md` / `ecc-migrated/rules/common/hooks.md` |
| 需要 Agent 协同 | 上述 3 条 common 基线 | `ecc-migrated/rules/common/agents.md` |
| TypeScript 项目任务 | 上述 3 条 common 基线 | `ecc-migrated/rules/typescript/coding-style.md` / `ecc-migrated/rules/typescript/security.md` / `ecc-migrated/rules/typescript/testing.md` / `ecc-migrated/rules/typescript/patterns.md` / `ecc-migrated/rules/typescript/hooks.md` |

---

我的需求：
{{input}}
