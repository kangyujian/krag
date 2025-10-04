<template>
  <el-card shadow="never">
    <template #header>
      <span>语义搜索</span>
    </template>
    <el-form label-width="120px">
      <el-form-item label="Query">
        <el-input v-model="query" placeholder="输入查询文本..." />
      </el-form-item>
      <el-form-item label="TopK / minScore">
        <el-input-number v-model="topK" :min="1" :max="50" />
        <el-input-number v-model="minScore" :step="0.01" :min="0" style="margin-left:8px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="doQuery" :loading="loading">Search</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="results" style="width:100%" v-if="results.length">
      <el-table-column prop="docId" label="Doc ID" width="260" />
      <el-table-column prop="chunkId" label="Chunk" width="140" />
      <el-table-column prop="score" label="Score" width="120" />
      <el-table-column prop="text" label="Text" />
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { queryVector, type QueryResultItem } from '../api/client';

const props = defineProps<{ tenantId: string; kbId: string }>();

const query = ref('');
const topK = ref(5);
const minScore = ref(0.0);
const loading = ref(false);
const results = ref<QueryResultItem[]>([]);

function notify(type: 'success' | 'warning' | 'error', message: string) {
  ElMessage({ type, message, duration: 2200 });
}

async function doQuery() {
  if (!query.value.trim()) return notify('warning', 'Query 不能为空');
  loading.value = true;
  try {
    const data = await queryVector({
      tenantId: props.tenantId,
      kbId: props.kbId,
      query: query.value,
      topK: topK.value,
      minScore: minScore.value,
    });
    results.value = Array.isArray(data.results) ? data.results : [];
    notify('success', `找到 ${results.value.length} 条结果`);
  } catch (e: any) {
    notify('error', e?.message || String(e));
  } finally {
    loading.value = false;
  }
}
</script>