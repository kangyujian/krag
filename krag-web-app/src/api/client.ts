import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
});

export interface QueryResultItem {
  docId: string;
  chunkId?: string | number;
  score?: number;
  text?: string;
}

export async function ingestText(
  tenantId: string,
  kbId: string,
  filename: string,
  text: string
) {
  const res = await api.post('/api/v1/ingest/text', text, {
    params: { tenantId, kbId, filename },
    headers: { 'Content-Type': 'text/plain' },
  });
  return res.data as { docId?: string };
}

export async function ingestFile(
  tenantId: string,
  kbId: string,
  file: File
) {
  const fd = new FormData();
  fd.append('file', file, file.name);
  const res = await api.post('/api/v1/ingest/txt', fd, {
    params: { tenantId, kbId },
  });
  return res.data as { docId?: string };
}

export async function queryVector(body: {
  tenantId: string;
  kbId: string;
  query?: string;
  topK?: number;
  minScore?: number;
  docId?: string;
  full?: boolean;
}) {
  const res = await api.post('/api/v1/query', body);
  return res.data as {
    results?: QueryResultItem[];
    documentText?: string;
    message?: string;
  };
}