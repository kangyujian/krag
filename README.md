# KRAG — 可扩展 RAG 检索中台（MVP）

一个以 Java + Spring Boot 实现的通用 RAG 检索中台，强调可插拔与多租户隔离。当前为第一期 MVP：支持 `.txt` 文档入库（接口预留）、内存向量库占位、查询与 LLM 客户端抽象，便于后续扩展到主流向量数据库与多模型供应商。

## 功能概述
- 多租户与知识库隔离：以 `tenantId/kbId` 进行命名空间管理。
- 文档入库（MVP 目标）：`.txt` 解析 → 分块 → 嵌入 → 入向量库（内存）。
- 查询与生成：查询文本向量化 → Top-K 检索 → 搭配上下文交给 LLM 输出。
- 可插拔设计：向量库、嵌入模型、LLM 客户端与文档解析均有统一接口。

## 项目结构
- `krag-api`：对外 REST API（Spring Boot 应用）。
- `krag-core`：核心接口与模型类型（向量库、嵌入、LLM、解析、租户等）。
- `krag-vector`：向量库实现（MVP：内存；后续 Milvus/Qdrant/pgvector/ES 等）。
- `krag-embedding`：嵌入算法实现（MVP 先占位；后续接入 BGE/GTE/OpenAI 等）。
- `krag-llm`：LLM 客户端实现（MVP：先对接一个供应商；后续可扩展）。
- `krag-ingest`：入库流程（解析/分块/嵌入/入向量库）。
- `krag-tenancy`：多租户上下文与隔离策略。
- `krag-common`：通用工具与基础类型。
- `docs/RAG-MVP-设计.md`：架构与接口设计蓝图。

## 快速开始
- 环境要求：`Java 17+`、`Maven 3.9+`。
- 构建与安装到本地仓库：
  - `mvn -DskipTests install`
- 启动 API 服务：
  - `cd krag-api`
  - `mvn -DskipTests spring-boot:run`
- 验证 Hello 接口：
  - `curl http://localhost:8080/api/v1/hello`
  - 响应示例：`{"message":"Hello, KRAG!","version":"0.1.0-SNAPSHOT"}`

### 前端（krag-web-app）
- 安装 Node（macOS）：`brew install node`
- 安装依赖：`make web-install`（等价于 `cd krag-web-app && npm install`）
- 启动开发服务器：`make start-web`（优先 Vite `http://localhost:5173/`，无 Node 时回退到静态预览 `http://localhost:8000/index.html`）
- 生产构建与预览：
  - 构建：`make web-build`（等价于 `cd krag-web-app && npm run build`）
  - 预览：`make web-preview`（或 `python3 -m http.server 8000 --directory krag-web-app/dist`）
- 后端地址配置：在 `krag-web-app/.env.development` 中设置 `VITE_API_BASE_URL`，默认 `http://localhost:8080`。

## 配置说明
- 应用配置：`krag-api/src/main/resources/application.yml`
  - `krag.model.provider`: 模型供应商（MVP 默认占位）。
  - `krag.embedding.provider`: 嵌入算法提供者（占位）。
  - `krag.store.provider`: 向量库提供者（MVP 为 `memory`）。

## 路线图（MVP）
- M1：项目骨架与核心接口（已完成）。
- M2：`MemoryVectorStore` 与相似度计算（进行中/待实现）。
- M3：`.txt` 解析与简易分块（待实现）。
- M4：嵌入算法 MVP 实现（待实现）。
- M5：查询流程闭环 + LLM 客户端（待实现）。
- M6：基础 API（`/ingest/txt`、`/query`）与租户/知识库管理（待实现）。
- M7：测试与观测（待实现）。

## 设计蓝图
- 详见文档：`docs/RAG-MVP-设计.md`
- 核心接口包括：`DocumentParser`、`TextChunker`、`EmbeddingModel`、`VectorStore`、`Retriever`、`LLMClient`、`TenancyContextHolder` 等。

## 贡献与开发建议
- 保持实现与接口的解耦，优先通过抽象层扩展新能力。
- 代码风格遵循现有模块结构，避免不必要的重命名与耦合。
- 构建产物与 IDE 文件已在 `.gitignore` 中忽略。

---

Copyright © 2025 KRAG