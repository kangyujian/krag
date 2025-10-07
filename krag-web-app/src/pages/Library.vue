<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>文档库（分页）</span>
        <div class="muted">查看文档、分块与向量</div>
      </div>
    </template>

    <div style="margin-bottom:8px;display:flex;gap:8px;align-items:center">
      <el-input-number v-model="size" :min="5" :max="50" />
      <el-button @click="fetchDocs" :loading="loading">刷新</el-button>
    </div>

    <el-table :data="items" style="width:100%" v-loading="loading">
      <el-table-column prop="docId" label="Doc ID" />
      <el-table-column prop="chunks" label="Chunks" width="120" />
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button size="small" @click="viewDoc(scope.row.docId)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display:flex;justify-content:flex-end;margin-top:8px">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="onPageChange"
      />
    </div>

    <el-drawer v-model="drawer" title="文档详情" size="50%">
      <div v-if="docChunks.length">
        <el-table :data="docChunks" style="width:100%">
          <el-table-column prop="chunkId" label="Chunk ID" width="220" />
          <el-table-column label="Text">
            <template #default="scope">
              <div class="chunk-text">{{ scope.row.text }}</div>
            </template>
          </el-table-column>
          <el-table-column label="向量" width="200">
            <template #default="scope">
              <div v-if="vectors[scope.$index]" class="vector-box">
                <div class="vector-mini">
                  <canvas :ref="setCanvasRef(scope.$index)" width="180" height="40"></canvas>
                </div>
                <el-button text type="primary" size="small" @click="viewVector(scope.$index)">查看数值</el-button>
              </div>
              <span v-else class="muted">无向量</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div v-else class="muted">该文档暂无分块</div>
    </el-drawer>

    <el-dialog v-model="vectorDialog" width="600px" title="向量值">
      <div v-if="currentVector">
        <div class="muted" style="margin-bottom:8px">维度：{{ dimension }}，显示前 64 项（如存在）</div>
        <pre class="mono">{{ vectorPreview }}</pre>
      </div>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref, nextTick } from 'vue';
import { ElMessage } from 'element-plus';
import { listDocs, getDocChunks, type DocListItem } from '../api/client';

const props = defineProps<{ tenantId: string; kbId: string }>();

const loading = ref(false);
const items = ref<DocListItem[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);

const drawer = ref(false);
const docChunks = ref<{ chunkId: string; text?: string }[]>([]);
const vectors = ref<number[][]>([]);
const dimension = ref<number | undefined>(undefined);
const canvasRefs = ref<Record<number, HTMLCanvasElement | null>>({});

const vectorDialog = ref(false);
const currentVector = ref<number[] | null>(null);
const vectorPreview = ref('');

function notify(type: 'success' | 'warning' | 'error', message: string) {
  ElMessage({ type, message, duration: 2200 });
}

async function fetchDocs() {
  loading.value = true;
  try {
    const data = await listDocs({ tenantId: props.tenantId, kbId: props.kbId, page: page.value, size: size.value });
    items.value = data.items || [];
    total.value = data.total || 0;
  } catch (e: any) {
    notify('error', e?.message || String(e));
  } finally {
    loading.value = false;
  }
}

function onPageChange(p: number) {
  page.value = p;
  fetchDocs();
}

function setCanvasRef(idx: number) {
  return (el: HTMLCanvasElement | null) => {
    canvasRefs.value[idx] = el;
    drawMiniVector(idx);
  };
}

function drawMiniVector(idx: number) {
  const el = canvasRefs.value[idx];
  const vec = vectors.value[idx];
  if (!el || !vec || !vec.length) return;
  const ctx = el.getContext('2d');
  if (!ctx) return;
  ctx.clearRect(0, 0, el.width, el.height);
  const w = el.width;
  const h = el.height;
  const n = Math.min(vec.length, 64);
  const step = w / n;
  const min = Math.min(...vec);
  const max = Math.max(...vec);
  const range = max - min || 1;
  ctx.fillStyle = '#3b82f6';
  for (let i = 0; i < n; i++) {
    const v = (vec[i] - min) / range;
    const barH = 4 + v * (h - 8);
    ctx.fillRect(i * step, h - barH, step - 1, barH);
  }
}

async function viewDoc(docId: string) {
  try {
    drawer.value = true;
    const data = await getDocChunks({ tenantId: props.tenantId, kbId: props.kbId, docId, includeVectors: true });
    docChunks.value = data.chunks || [];
    vectors.value = data.vectors || [];
    dimension.value = data.dimension;
    await nextTick();
    docChunks.value.forEach((_c, idx) => drawMiniVector(idx));
  } catch (e: any) {
    notify('error', e?.message || String(e));
  }
}

function viewVector(idx: number) {
  const vec = vectors.value[idx];
  if (!vec) return;
  currentVector.value = vec;
  const preview = vec.slice(0, 64).map(v => Number(v.toFixed(6))).join(', ');
  vectorPreview.value = '[' + preview + (vec.length > 64 ? ', ...' : '') + ']';
  vectorDialog.value = true;
}

onMounted(fetchDocs);
</script>

<style scoped>
.muted { color:#6b7280; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; }
.chunk-text { max-height: 120px; overflow: auto; }
.vector-box { display:flex; align-items:center; gap:8px; }
.vector-mini { border: 1px solid #e5e7eb; border-radius:4px; padding:4px; }
</style>