<template>
  <el-card shadow="never">
    <template #header>
      <span>按文档ID获取全文</span>
    </template>
    <el-form label-width="120px">
      <el-form-item label="Doc ID">
        <el-input v-model="docId" placeholder="输入文档ID" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="fetchDoc" :loading="loading">Fetch</el-button>
      </el-form-item>
    </el-form>
    <div v-if="docText">
      <h4>Document Text</h4>
      <pre>{{ docText }}</pre>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { queryVector } from '../api/client';

const props = defineProps<{ tenantId: string; kbId: string }>();

const docId = ref('');
const docText = ref('');
const loading = ref(false);

function notify(type: 'success' | 'warning' | 'error', message: string) {
  ElMessage({ type, message, duration: 2200 });
}

async function fetchDoc() {
  if (!docId.value.trim()) return notify('warning', 'Doc ID 不能为空');
  loading.value = true;
  try {
    const data = await queryVector({
      tenantId: props.tenantId,
      kbId: props.kbId,
      docId: docId.value,
      full: true,
    });
    docText.value = data.documentText || '';
    notify('success', '获取成功');
  } catch (e: any) {
    notify('error', e?.message || String(e));
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
pre { background:#0b1020; color:#d6e2ff; padding:12px; border-radius:8px; overflow:auto; }
</style>