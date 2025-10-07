# RAG 中台 MVP 技术方案（Java + Spring Boot）

## 1. 背景与目标
- 目标：建设一个可扩展的通用 RAG 检索中台，支持多租户隔离、可插拔的向量数据库、可插拔的大模型与嵌入算法，满足不同业务快速接入。
- MVP 范围（一期）：
  - 支持 `.txt` 文档入库（分块、向量化、入向量库）。
  - 支持内存型向量库（易实现与验证）。
  - 支持查询文本向量检索，检索到的相关文档与问题交给大模型生成答案。
  - 提供基础的租户与知识库隔离（`tenantId` + `kbId`）。
  - 先对接一个模型供应商（建议：阿里「千问/Qwen」），后续可扩展到 ChatGPT、豆包等。

## 2. 总体架构

```
                +-----------------------+
                |     API Gateway       |
                |  (REST, Auth, Rate)   |
                +-----------+-----------+
                            |
            +---------------+----------------+
            |                                |
    +-------v------+                    +----v------+
    | IngestionSvc |                    | QuerySvc  |
    | (解析/分块/嵌入)|                    | (嵌入/检索/LLM)|
    +-------+------+                    +----+------+
            |                                |
    +-------v-----------+             +------v-----------+
    | EmbeddingProvider |             | Retriever        |
    | (算法可插拔)       |             | (向量库抽象层)   |
    +-------+-----------+             +------+-----------+
            |                                |
    +-------v-----------+             +------v-----------+
    | VectorStore Abstr.|<----------->| MemoryVectorStore|
    | (库可插拔)         |             | (MVP,内存)       |
    +-------------------+             +------------------+
                            |
                      +-----v------+
                      |  LLMClient |
                      | (Qwen MVP) |
                      +------------+

      +-------------+   +----------------+   +--------------+
      | TenancyMgr  |   | ModelRegistry  |   | Parser Abstr.|
      |(租户隔离)    |   |(模型/嵌入配置)  |   |(txt/pdf/office)|
      +-------------+   +----------------+   +--------------+
```

- API Gateway：统一对外 REST 接口，鉴权与限流（MVP 可简化为 API Key）。
- Ingestion Service：文档入库流程（解析→分块→嵌入→入库）。
- Query Service：查询流程（嵌入→向量检索→上下文组装→LLM 应答）。
- Embedding Provider：嵌入算法接口，支持多种实现（MVP 先实现 1 种）。
- VectorStore Abstraction：向量库抽象层，MVP 使用内存库，后续可扩展到 Milvus、Qdrant、pgvector、Elasticsearch、Pinecone 等。
- LLM Client：大模型访问客户端，MVP 对接「千问」，后续可插拔。
- Tenancy Manager：多租户隔离（`tenantId`/`kbId` 维度的数据域）。
- Parser Abstraction：文档解析接口，MVP 支持 `.txt`，后续扩展 pdf、word、excel。
- Model Registry：统一管理模型与嵌入算法的配置与选择。

## 3. 模块与包规划（建议）
- `krag-api`：对外 REST API（Controller 层）。
- `krag-core`：领域模型、接口定义（向量库、嵌入、检索、LLM、解析、租户等）。
- `krag-vector`：向量库实现（MVP：内存库；扩展：Milvus/Qdrant/pgvector/ES 等）。
- `krag-embedding`：嵌入算法实现（MVP：一种实现；扩展：BGE、GTE、OpenAI Embeddings 等）。
- `krag-llm`：LLM 客户端实现（MVP：Qwen；扩展：OpenAI/豆包/本地模型）。
- `krag-ingest`：入库流程（解析、分块、嵌入、入库）。
- `krag-tenancy`：多租户域、上下文、隔离策略。
- `krag-common`：通用工具（配置、异常、分页、拦截器等）。

## 4. 数据模型（MVP）
- `Tenant`：租户实体（`tenantId`、名称、状态）。
- `KnowledgeBase`：知识库实体（`kbId`、`tenantId`、名称、描述）。
- `Document`：文档元数据（`docId`、`kbId`、`tenantId`、文件名、来源）。
- `Chunk`：分块数据（`chunkId`、`docId`、内容、顺序）。
- `Embedding`：分块向量（`chunkId`、`float[] vector`、`dim`）。
- `VectorRecord`：向量库记录（`tenantId`、`kbId`、`chunkId`、`vector`、`metadata`）。
- 内存库结构（MVP）：`Map<String tenantId, Map<String kbId, List<VectorRecord>>>`。

## 5. 接口定义草案（只定义接口，便于后续实现）

```java
// krag-core
public interface DocumentParser {
    boolean supports(String contentTypeOrExt);
    List<String> parseToTexts(InputStream in, String filename);
}

public interface TextChunker {
    List<String> chunk(List<String> texts, int maxTokensOrChars);
}

public interface EmbeddingModel {
    String id(); // 模型标识，如 "qwen-embedding-bge-base"
    int dimension();
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
}

public interface VectorStore {
    void upsert(String tenantId, String kbId, List<VectorRecord> records);
    List<SearchResult> search(String tenantId, String kbId, float[] queryVector, int topK, float minScore);
    void deleteByDoc(String tenantId, String kbId, String docId);
}

public interface Retriever {
    List<SearchResult> retrieve(String tenantId, String kbId, String query, int topK);
}

public interface LLMClient {
    String modelId();
    String chat(String systemPrompt, String userPrompt, List<ContextDoc> contexts);
}

public interface TenancyContextHolder {
    void set(String tenantId, String kbId);
    String tenantId();
    String kbId();
    void clear();
}

// 元数据与结果类型
public class VectorRecord { String chunkId; String docId; String source; float[] vector; Map<String, String> metadata; }
public class SearchResult { String chunkId; String docId; String text; float score; Map<String, String> metadata; }
public class ContextDoc { String docId; String text; }
```

- 以上为 MVP 必要接口，后续可扩展：`ReRanker`、`PromptTemplate`、`AuthorizationService`、`RateLimiter` 等。

## 6. 关键流程设计

### 6.1 入库流程（`.txt`）
- 输入：`tenantId`、`kbId`、文件（`text/plain`）。
- 步骤：
  - 解析：`DocumentParser` 读取文本。
  - 分块：`TextChunker` 以固定字符或简易句段（MVP 可 500~1000 字）切分。
  - 向量化：`EmbeddingModel.embedBatch`。
  - 入库：组装 `VectorRecord`，调用 `VectorStore.upsert`。
- 输出：`docId`、分块统计、成功入库条数。

### 6.2 查询流程
- 输入：`tenantId`、`kbId`、查询文本 `query`、可选参数 `topK`。
- 步骤：
  - 查询文本嵌入：`EmbeddingModel.embed`。
  - 检索：`VectorStore.search`（内存库用余弦相似度/向量点积）。
  - 上下文组装：选取 Top-K 结果作为 `ContextDoc`，拼提示词。
  - LLM 应答：`LLMClient.chat(system, user, contexts)`。
- 输出：答案文本、引用片段与来源（docId、chunkId）。

## 7. 可扩展性设计与选型建议

### 7.1 向量库选型与抽象
- MVP：`MemoryVectorStore`（`List<VectorRecord>` + 简单检索，适合快速开发与单机验证）。
- 可扩展：
  - `Milvus/Qdrant`：高性能、社区成熟，适合生产。
  - `pgvector(PostgreSQL)`：易运维、与关系数据共存。
  - `Elasticsearch`：已有集群可复用，向量检索支持度提升中。
  - `Pinecone`：SaaS 化，省心但成本可控性差。
- 抽象要求：统一 `upsert/search/delete` 接口，支持 `tenantId/kbId` 维度隔离（命名空间或前缀）。

### 7.2 嵌入算法选型
- MVP：选择一种可用且易接入的中文嵌入（如 BGE/GTE 家族或 Qwen Embedding）。
- 可扩展：OpenAI `text-embedding-3-large/small`、Jina、SentenceTransformers、本地模型（Ollama + bge/gte）。
- 抽象要求：统一 `embed/embedBatch/dimension/modelId`；支持不同向量维度。

### 7.3 LLM 选型
- MVP：阿里「千问/Qwen」作为首个实现。
- 可扩展：OpenAI（ChatGPT）、字节「豆包」、本地 LLM（如 Ollama/LM Studio）。
- 抽象要求：统一 `chat(system, user, contexts)`；后续可扩展工具/函数调用。

### 7.4 文档解析
- MVP：`.txt` 解析（直接读取）。
- 扩展：
  - `pdf`：Apache PDFBox / pdfbox-android。
  - `word/excel/ppt`：Apache POI。
  - 通用：Apache Tika 做 MIME 检测与统一解析入口。
- 抽象要求：`supports()` + `parseToTexts()`。

### 7.5 多租户隔离策略
- 命名空间隔离：所有数据以 `tenantId/kbId` 二级命名空间组织。
- API 约束：所有入库/检索必须携带 `tenantId/kbId`。
- 存储层：向量库按命名空间独立集合/索引（内存库为独立 Map）。
- 管理：`TenancyMgr` 统一创建/删除租户与知识库，管理配额与模型绑定。

## 8. API 设计（MVP）

- `POST /api/v1/tenants`：创建租户（返回 `tenantId`）。
- `POST /api/v1/kbs`：创建知识库（入参 `tenantId`，返回 `kbId`）。
- `POST /api/v1/ingest/txt`：上传 `.txt` 入库（`tenantId/kbId` + 文件）。
- `POST /api/v1/query`：提交问题（`tenantId/kbId` + `query` + 可选 `topK`）。
- `GET /api/v1/models`：列出可用的模型与嵌入算法。
- `GET /api/v1/stores`：列出可用的向量库实现。
- 鉴权：MVP 使用 `x-api-key` 头简化；后续支持 OAuth2/JWT。

### 8.1 请求示例

创建租户：
```
curl -X POST 'http://localhost:8080/api/v1/tenants' \
  -H 'x-api-key: <your-key>' \
  -H 'Content-Type: application/json' \
  -d '{"name":"demo-tenant"}'
```

创建知识库：
```
curl -X POST 'http://localhost:8080/api/v1/kbs' \
  -H 'x-api-key: <your-key>' \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":"t1","name":"kb1"}'
```

上传 `.txt` 入库：
```
curl -X POST 'http://localhost:8080/api/v1/ingest/txt' \
  -H 'x-api-key: <your-key>' \
  -F 'tenantId=t1' -F 'kbId=kb1' \
  -F 'file=@/path/to/sample.txt'
```

查询：
```
curl -X POST 'http://localhost:8080/api/v1/query' \
  -H 'x-api-key: <your-key>' \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":"t1","kbId":"kb1","query":"什么是KRAG？","topK":4}'
```

### 8.2 响应示例（参考）

`/ingest/txt`：
```json
{
  "tenantId": "t1",
  "kbId": "kb1",
  "docId": "doc-20250101-0001",
  "chunks": 12,
  "embedded": 12,
  "stored": 12
}
```

`/query`：
```json
{
  "answer": "KRAG 是一个可扩展的 RAG 检索中台……",
  "contexts": [
    {"docId": "doc-20250101-0001", "chunkId": "c01", "text": "……", "score": 0.83, "source": "sample.txt"},
    {"docId": "doc-20250101-0001", "chunkId": "c02", "text": "……", "score": 0.79, "source": "sample.txt"}
  ]
}
```

### 8.3 错误码与约定（MVP）
- 400：参数缺失或非法（如未携带 `tenantId/kbId`）。
- 401：鉴权失败（`x-api-key` 无效）。
- 404：租户或知识库不存在。
- 422：文件解析失败或嵌入失败。
- 500：服务内部错误（记录 `traceId` 便于排查）。

## 9. 配置与管理
- `ModelRegistry`：模型与嵌入算法注册/选择（可通过配置文件或数据库）。
- 业务快速接入：
  - 创建租户 → 分配知识库 → 选择模型与嵌入 → 配置配额。
  - 每个业务通过 `tenantId/kbId` 调用 API，即可隔离使用。

## 10. MVP 里程碑
- M1：项目骨架（Spring Boot 多模块）与核心接口定义。
- M2：`MemoryVectorStore` 与简单相似度实现（余弦/点积）。
- M3：`.txt` 解析与分块（`TextChunker`）。
- M4：嵌入算法 MVP 实现（任选其一，配置化）。
- M5：查询流程（检索 + LLM 应答），接入 Qwen。
- M6：API 层与租户/知识库管理基础接口。
- M7：日志、监控与基础测试用例（入库/检索/端到端）。

## 11. 提示词与对话策略（MVP）
- System Prompt：角色与限制（简洁、中文优先、引用上下文）。
- User Prompt：原始问题 + Top-K 上下文片段（控制总长度）。
- 策略：
  - 上下文拼接前先去重与归并相似片段。
  - 引用返回：携带 `docId/chunkId/source` 以便追溯。
  - 长度控制：根据模型最大上下文自动截断。

## 12. 安全与合规
- 多租户隔离：在 API、存储、日志维度严格使用 `tenantId/kbId` 隔离。
- 访问控制：MVP 使用 API Key；后续 JWT/OAuth2 与 RBAC。
- 数据隐私：不保留用户问题与上下文（或可配置保留周期与脱敏）。
- 速率限制：基础限流，防止单租户滥用。

## 13. 风险与权衡
- MVP 内存库不持久：重启丢数据（权衡开发速度）。
- 嵌入算法与 LLM 供应商差异：需在抽象层规避耦合。
- 文档解析复杂度（PDF/Office）：后续引入 Tika/POI 统一入口。
- 成本与延迟：云模型存在费用与延迟，支持本地模型是长期方向。

## 14. 测试与观测
- 单元测试：接口层（Chunker/Embedding/VectorStore/Retriever/LLMClient）。
- 集成测试：入库与检索串联。
- 端到端：模拟租户创建 → `.txt` 入库 → 查询。
- 观测：基础日志（traceId、tenantId、kbId、耗时、topK、score）。

## 15. 下一步实现建议（MVP）
- 创建多模块工程骨架，落地第 5 节接口（仅接口与基础类型）。
- 先实现：`MemoryVectorStore`、`SimpleTextChunker`、`TxtParser`、一种 `EmbeddingModel`（可用占位或真实接入）。
- 对接 Qwen 的 `LLMClient`，打通查询闭环。
- 提供最小 API：`/ingest/txt` 与 `/query`，带 `tenantId/kbId`。
- 加入 `ModelRegistry` 与配置化选择嵌入与 LLM。

---

本文档定位为架构与接口蓝图。MVP 实现阶段请严格遵循抽象接口，避免实现层泄露到 API 与业务逻辑中，以保障后续向量库、嵌入与 LLM 的可插拔扩展。