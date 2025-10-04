<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>上传文档</span>
      </div>
    </template>
    <el-form label-width="120px">
      <el-form-item label="文件名">
        <el-input v-model="filename" placeholder="例如 sample.txt" />
      </el-form-item>
      <el-form-item label="文本内容">
        <el-input v-model="text" type="textarea" :rows="5" placeholder="粘贴文本进行入库..." />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="ingestTextClick" :loading="loading">文本入库</el-button>
        <el-divider direction="vertical" />
        <input type="file" ref="fileInput" style="display:none" @change="ingestFileChange" />
        <el-button @click="chooseFile">选择文件</el-button>
        <el-button type="success" @click="ingestFileClick" :disabled="!selectedFile" :loading="loading">上传文件</el-button>
        <span class="muted" v-if="selectedFile" style="margin-left:8px">{{ selectedFile.name }} ({{ selectedFile.size }} bytes)</span>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { ingestText, ingestFile } from '../api/client';

const props = defineProps<{ tenantId: string; kbId: string }>();
const emit = defineEmits<{ (e: 'doc', docId: string): void }>();

const filename = ref('inline.txt');
const text = ref('');
const loading = ref(false);
const selectedFile = ref<File | null>(null);

function notify(type: 'success' | 'warning' | 'error', message: string) {
  ElMessage({ type, message, duration: 2200 });
}

async function ingestTextClick() {
  if (!text.value.trim()) return notify('warning', '文本内容不能为空');
  if (!filename.value.endsWith('.txt')) return notify('warning', '文件名需以 .txt 结尾');
  loading.value = true;
  try {
    const data = await ingestText(props.tenantId, props.kbId, filename.value, text.value);
    if (data?.docId) {
      emit('doc', data.docId);
      notify('success', `入库成功: ${data.docId}`);
    } else {
      notify('error', '入库失败');
    }
  } catch (e: any) {
    notify('error', e?.message || String(e));
  } finally {
    loading.value = false;
  }
}

function chooseFile() {
  (document.querySelector('input[type=file]') as HTMLInputElement)?.click();
}

function ingestFileChange(e: Event) {
  const input = e.target as HTMLInputElement;
  selectedFile.value = input.files && input.files[0] ? input.files[0] : null;
}

async function ingestFileClick() {
  if (!selectedFile.value) return notify('warning', '请选择文件');
  loading.value = true;
  try {
    const data = await ingestFile(props.tenantId, props.kbId, selectedFile.value);
    if (data?.docId) {
      emit('doc', data.docId);
      notify('success', `上传成功: ${data.docId}`);
    } else {
      notify('error', '上传失败');
    }
  } catch (e: any) {
    notify('error', e?.message || String(e));
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.muted { color:#6b7280; }
</style>