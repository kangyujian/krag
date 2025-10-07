package com.krag.vector;

import com.krag.core.model.SearchResult;
import com.krag.core.model.VectorRecord;
import com.krag.core.store.VectorStore;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.KnnVectorQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Fields;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class LuceneMemoryVectorStore implements VectorStore {

    private static class NamespaceIndex {
        final Directory directory = new ByteBuffersDirectory();
        final Analyzer analyzer = new StandardAnalyzer();
        final IndexWriter writer;
        Integer dimension = null;
        NamespaceIndex() {
            try {
                this.writer = new IndexWriter(directory, new IndexWriterConfig(analyzer));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private final Map<String, NamespaceIndex> indices = new HashMap<>();

    private NamespaceIndex ns(String tenantId, String kbId) {
        String key = tenantId + ":" + kbId;
        return indices.computeIfAbsent(key, k -> new NamespaceIndex());
    }

    @Override
    public synchronized void upsert(String tenantId, String kbId, List<VectorRecord> records) {
        NamespaceIndex ni = ns(tenantId, kbId);
        try {
            for (VectorRecord r : records) {
                if (ni.dimension == null) {
                    ni.dimension = r.getVector().length;
                }
                if (r.getVector().length != ni.dimension) {
                    throw new IllegalArgumentException("Vector dimension mismatch for namespace " + tenantId + ":" + kbId);
                }
                Document doc = new Document();
                doc.add(new StringField("docId", r.getDocId(), Field.Store.YES));
                doc.add(new StringField("chunkId", r.getChunkId(), Field.Store.YES));
                if (r.getSource() != null) {
                    doc.add(new StringField("source", r.getSource(), Field.Store.YES));
                }
                // Store original text if provided in metadata under key 'text'
                String text = r.getMetadata() != null ? r.getMetadata().getOrDefault("text", null) : null;
                if (text != null) {
                    doc.add(new StoredField("text", text));
                }
                doc.add(new KnnVectorField("vector", r.getVector()));
                // We don't dedupe here; caller can call deleteByDoc before upsert if needed
                ni.writer.addDocument(doc);
            }
            ni.writer.commit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized List<SearchResult> search(String tenantId, String kbId, float[] queryVector, int topK, float minScore) {
        NamespaceIndex ni = ns(tenantId, kbId);
        try {
            DirectoryReader reader = DirectoryReader.open(ni.directory);
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs topDocs = searcher.search(new KnnVectorQuery("vector", queryVector, topK), topK);
            List<SearchResult> results = new ArrayList<>();
            for (ScoreDoc sd : topDocs.scoreDocs) {
                if (sd.score < minScore) continue;
                Document doc = searcher.doc(sd.doc);
                SearchResult sr = new SearchResult();
                sr.setDocId(doc.get("docId"));
                sr.setChunkId(doc.get("chunkId"));
                sr.setText(doc.get("text"));
                sr.setScore(sd.score);
                results.add(sr);
            }
            reader.close();
            return results;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void deleteByDoc(String tenantId, String kbId, String docId) {
        NamespaceIndex ni = ns(tenantId, kbId);
        try {
            ni.writer.deleteDocuments(new Term("docId", docId));
            ni.writer.commit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized List<SearchResult> findByDoc(String tenantId, String kbId, String docId) {
        NamespaceIndex ni = ns(tenantId, kbId);
        try {
            DirectoryReader reader = DirectoryReader.open(ni.directory);
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs topDocs = searcher.search(new TermQuery(new Term("docId", docId)), Integer.MAX_VALUE);
            List<SearchResult> results = new ArrayList<>();
            for (ScoreDoc sd : topDocs.scoreDocs) {
                Document doc = searcher.doc(sd.doc);
                SearchResult sr = new SearchResult();
                sr.setDocId(doc.get("docId"));
                sr.setChunkId(doc.get("chunkId"));
                sr.setText(doc.get("text"));
                sr.setScore(sd.score);
                results.add(sr);
            }
            reader.close();
            return results;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized List<String> listDocIds(String tenantId, String kbId) {
        NamespaceIndex ni = ns(tenantId, kbId);
        try {
            DirectoryReader reader = DirectoryReader.open(ni.directory);
            IndexSearcher searcher = new IndexSearcher(reader);
            LinkedHashSet<String> ids = new LinkedHashSet<>();
            TopDocs topDocs = searcher.search(new org.apache.lucene.search.MatchAllDocsQuery(), Integer.MAX_VALUE);
            for (ScoreDoc sd : topDocs.scoreDocs) {
                Document doc = searcher.doc(sd.doc);
                String id = doc.get("docId");
                if (id != null) {
                    ids.add(id);
                }
            }
            reader.close();
            return new ArrayList<>(ids);
        } catch (org.apache.lucene.index.IndexNotFoundException e) {
            // No index segments exist yet for this namespace; return empty list gracefully
            return Collections.emptyList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}