<template>
  <div>
    <header class="app-header">
      <div class="brand">KRAG</div>
      <div class="muted">Extensible RAG Platform</div>
    </header>

    <div class="container">
      <el-card class="card" shadow="never">
        <el-row :gutter="12" style="margin-bottom: 12px">
          <el-col :span="6">
            <el-input v-model="tenantId" placeholder="tenantId" />
          </el-col>
          <el-col :span="6">
            <el-input v-model="kbId" placeholder="kbId" />
          </el-col>
          <el-col :span="12">
            <el-input v-model="apiBase" placeholder="Backend Base URL" class="mono" />
          </el-col>
        </el-row>

        <el-tabs v-model="tab">
          <el-tab-pane label="Ingest" name="ingest">
            <IngestPage :tenant-id="tenantId" :kb-id="kbId" @doc="onDoc" />
          </el-tab-pane>
          <el-tab-pane label="Query" name="query">
            <QueryPage :tenant-id="tenantId" :kb-id="kbId" />
          </el-tab-pane>
          <el-tab-pane label="Document" name="doc">
            <DocumentPage :tenant-id="tenantId" :kb-id="kbId" />
          </el-tab-pane>
          <el-tab-pane label="Library" name="library">
            <LibraryPage :tenant-id="tenantId" :kb-id="kbId" />
          </el-tab-pane>
        </el-tabs>

        <div v-if="lastDocId" style="margin-top:8px">
          <el-tag type="success">Last Doc ID: {{ lastDocId }}</el-tag>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import IngestPage from './pages/Ingest.vue';
import QueryPage from './pages/Query.vue';
import DocumentPage from './pages/Document.vue';
import LibraryPage from './pages/Library.vue';

const tenantId = ref('tenant1');
const kbId = ref('kb1');
const apiBase = ref(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080');
const tab = ref('ingest');
const lastDocId = ref('');

function onDoc(docId: string) {
  lastDocId.value = docId;
}

// 允许在界面中动态修改 baseURL（例如切换不同后端）
watch(apiBase, (val) => {
  // 动态更新，但 axios 实例在 client.ts 中创建；
  // 简化处理：让用户通过 .env 设定，或在此处提示。
  console.warn('当前仅支持通过环境变量设置 API Base URL，实际值：', val);
});
</script>

<style scoped>
.app-header { background: white; border-bottom: 1px solid #e5e7eb; padding: 12px 20px; display:flex; align-items:center; gap:12px; }
.brand { font-weight: 700; color: #3b82f6; }
.container { max-width: 1080px; margin: 16px auto; padding: 0 16px; }
.card { background: white; border: 1px solid #e5e7eb; border-radius: 8px; }
.muted { color:#6b7280; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; }
</style>