package com.example.hospital.patient.wx.api.agent.multi.rag;

import com.example.hospital.patient.wx.api.agent.multi.config.MultiAgentProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MultiAgentKnowledgeBase {
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    private static final Pattern LIST_PREFIX_PATTERN = Pattern.compile("^([-*+]\\s+|\\d+\\.\\s+)");
    private static final Set<String> STOP_WORDS = new HashSet<String>(Arrays.asList(
            "为什么", "为啥", "这个", "那个", "一下", "请问", "怎么", "如何", "可以", "能否", "是否"
    ));
    private static final int MAX_DOC_LOOKUP_LEVEL = 6;

    @Resource
    private MultiAgentProperties properties;

    @Resource
    private EmbeddingVectorIndex vectorIndex;

    private volatile CorpusSnapshot cachedSnapshot;

    public MultiAgentKnowledgeBase() {
        this.properties = new MultiAgentProperties();
        this.vectorIndex = new EmbeddingVectorIndex(this.properties, new EmbeddingClient(this.properties));
    }

    public MultiAgentKnowledgeBase(MultiAgentProperties properties,
                                   EmbeddingVectorIndex vectorIndex) {
        this.properties = properties == null ? new MultiAgentProperties() : properties;
        this.vectorIndex = vectorIndex == null ? new EmbeddingVectorIndex(this.properties, new EmbeddingClient(this.properties)) : vectorIndex;
    }

    public List<KnowledgeSnippet> search(String query, int limit) {
        return retrieve(query, limit).getSnippets();
    }

    public SearchResult retrieve(String query, int limit) {
        if (!StringUtils.hasText(query)) {
            return SearchResult.empty("empty_query", "empty_query");
        }
        CorpusSnapshot snapshot = loadSnapshot();
        if (snapshot.snippets.isEmpty()) {
            return SearchResult.empty("fallback", "empty_corpus");
        }
        String normalizedQuery = normalizeText(query);
        List<String> queryTerms = buildQueryTerms(normalizedQuery);
        Map<String, RetrievalHit> merged = new LinkedHashMap<String, RetrievalHit>();
        for (RetrievalHit hit : buildKeywordHits(normalizedQuery, queryTerms, snapshot.snippets)) {
            merged.put(hit.getSnippet().getCacheKey(), hit);
        }
        List<RetrievalHit> vectorHits = vectorIndex == null
                ? Collections.<RetrievalHit>emptyList()
                : vectorIndex.search(query, snapshot.snippets, snapshot.version, properties.getRagVectorTopK());
        for (RetrievalHit vectorHit : vectorHits) {
            String key = vectorHit.getSnippet().getCacheKey();
            RetrievalHit existing = merged.get(key);
            if (existing == null) {
                vectorHit.setFinalScore(vectorHit.getVectorScore() * 10D);
                merged.put(key, vectorHit);
                continue;
            }
            existing.setVectorScore(vectorHit.getVectorScore());
            existing.setFinalScore(existing.getKeywordScore() + vectorHit.getVectorScore() * 10D);
            existing.setMode("hybrid");
        }
        List<RetrievalHit> hits = new ArrayList<RetrievalHit>(merged.values());
        Collections.sort(hits, new Comparator<RetrievalHit>() {
            @Override
            public int compare(RetrievalHit left, RetrievalHit right) {
                int scoreCompare = Double.compare(right.getFinalScore(), left.getFinalScore());
                if (scoreCompare != 0) {
                    return scoreCompare;
                }
                return left.getSnippet().getTitle().compareTo(right.getSnippet().getTitle());
            }
        });
        int size = Math.min(limit <= 0 ? Math.max(properties.getRagKeywordTopK(), properties.getRagVectorTopK()) : limit, hits.size());
        List<KnowledgeSnippet> snippets = new ArrayList<KnowledgeSnippet>();
        List<RetrievalHit> selectedHits = new ArrayList<RetrievalHit>();
        double maxScore = 0D;
        for (int i = 0; i < size; i++) {
            RetrievalHit hit = hits.get(i);
            selectedHits.add(hit);
            snippets.add(hit.getSnippet());
            maxScore = Math.max(maxScore, hit.getFinalScore());
        }
        String mode = resolveMode(selectedHits, vectorHits);
        String fallbackReason = selectedHits.isEmpty() ? "no_hit" : (vectorHits.isEmpty() && properties.isRagEmbeddingEnabled() ? "vector_unavailable" : null);
        return new SearchResult(snippets, selectedHits, mode, maxScore, fallbackReason, snapshot.version);
    }

    private List<RetrievalHit> buildKeywordHits(String normalizedQuery,
                                                List<String> queryTerms,
                                                List<KnowledgeSnippet> corpus) {
        List<RetrievalHit> hits = new ArrayList<RetrievalHit>();
        for (KnowledgeSnippet snippet : corpus) {
            int score = score(normalizedQuery, queryTerms, snippet);
            if (score <= 0) {
                continue;
            }
            RetrievalHit hit = new RetrievalHit();
            hit.setSnippet(snippet);
            hit.setKeywordScore(score);
            hit.setFinalScore(score);
            hit.setMode("keyword");
            hits.add(hit);
        }
        return hits;
    }

    private String resolveMode(List<RetrievalHit> hits, List<RetrievalHit> vectorHits) {
        boolean hasVector = vectorHits != null && !vectorHits.isEmpty();
        boolean hasKeyword = false;
        boolean hasHybrid = false;
        for (RetrievalHit hit : hits) {
            if (hit == null) {
                continue;
            }
            if ("hybrid".equals(hit.getMode())) {
                hasHybrid = true;
            }
            if (hit.getKeywordScore() > 0D) {
                hasKeyword = true;
            }
        }
        if (hasHybrid || (hasVector && hasKeyword)) {
            return "hybrid";
        }
        if (hasVector) {
            return "vector";
        }
        return "keyword";
    }

    private int score(String normalizedQuery, List<String> queryTerms, KnowledgeSnippet snippet) {
        String normalizedTitle = normalizeText(snippet.getTitle());
        String normalizedContent = normalizeText(snippet.getContent());
        int score = 0;
        if (normalizedTitle.contains(normalizedQuery)) {
            score += 20;
        }
        if (normalizedContent.contains(normalizedQuery)) {
            score += 12;
        }
        for (String term : queryTerms) {
            if (!StringUtils.hasText(term)) {
                continue;
            }
            if (normalizedTitle.equals(term)) {
                score += 12;
                continue;
            }
            if (normalizedTitle.contains(term)) {
                score += term.length() >= 4 ? 8 : 4;
                continue;
            }
            if (containsKeyword(snippet.getKeywords(), term)) {
                score += term.length() >= 4 ? 6 : 3;
                continue;
            }
            if (normalizedContent.contains(term)) {
                score += term.length() >= 4 ? 4 : 2;
            }
        }
        return score;
    }

    private boolean containsKeyword(List<String> keywords, String term) {
        for (String keyword : keywords) {
            if (keyword.equals(term) || keyword.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private CorpusSnapshot loadSnapshot() {
        CorpusSnapshot snapshot = cachedSnapshot;
        if (snapshot != null) {
            return snapshot;
        }
        synchronized (this) {
            if (cachedSnapshot == null) {
                cachedSnapshot = loadCorpusFromDocs();
            }
            return cachedSnapshot;
        }
    }

    private CorpusSnapshot loadCorpusFromDocs() {
        Path docsDir = locateDocsDirectory();
        if (docsDir == null) {
            return new CorpusSnapshot(Collections.<KnowledgeSnippet>emptyList(), "missing_docs");
        }
        List<Path> files = new ArrayList<Path>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(docsDir, "*.md")) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    files.add(path);
                }
            }
        } catch (IOException e) {
            return new CorpusSnapshot(Collections.<KnowledgeSnippet>emptyList(), "load_error");
        }
        Collections.sort(files, new Comparator<Path>() {
            @Override
            public int compare(Path left, Path right) {
                return left.getFileName().toString().compareTo(right.getFileName().toString());
            }
        });
        List<KnowledgeSnippet> snippets = new ArrayList<KnowledgeSnippet>();
        StringBuilder versionBuilder = new StringBuilder();
        for (Path file : files) {
            snippets.addAll(parseMarkdown(file));
            try {
                versionBuilder.append(file.getFileName()).append(':').append(Files.getLastModifiedTime(file).toMillis()).append(';');
            } catch (IOException e) {
                versionBuilder.append(file.getFileName()).append(';');
            }
        }
        return new CorpusSnapshot(Collections.unmodifiableList(snippets), Integer.toHexString(versionBuilder.toString().hashCode()));
    }

    private Path locateDocsDirectory() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        for (int i = 0; current != null && i <= MAX_DOC_LOOKUP_LEVEL; i++) {
            Path candidate = current.resolve("docs").resolve("agent");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }

    private List<KnowledgeSnippet> parseMarkdown(Path path) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return Collections.emptyList();
        }
        List<KnowledgeSnippet> snippets = new ArrayList<KnowledgeSnippet>();
        List<String> headings = new ArrayList<String>();
        List<String> block = new ArrayList<String>();
        for (String rawLine : lines) {
            String line = stripBom(rawLine);
            Matcher matcher = HEADING_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                flushBlock(path, headings, block, snippets);
                updateHeadings(headings, matcher.group(1).length(), matcher.group(2));
                continue;
            }
            if (!StringUtils.hasText(line)) {
                flushBlock(path, headings, block, snippets);
                continue;
            }
            block.add(line);
        }
        flushBlock(path, headings, block, snippets);
        return snippets;
    }

    private void updateHeadings(List<String> headings, int level, String title) {
        while (headings.size() >= level) {
            headings.remove(headings.size() - 1);
        }
        String normalizedTitle = normalizeLine(title);
        if (StringUtils.hasText(normalizedTitle)) {
            headings.add(normalizedTitle);
        }
    }

    private void flushBlock(Path path,
                            List<String> headings,
                            List<String> block,
                            List<KnowledgeSnippet> snippets) {
        if (block.isEmpty()) {
            return;
        }
        String content = normalizeBlock(block);
        block.clear();
        if (!StringUtils.hasText(content)) {
            return;
        }
        snippets.add(new KnowledgeSnippet(buildTitle(path, headings), content, buildKeywords(path, headings)));
    }

    private String normalizeBlock(List<String> block) {
        StringBuilder builder = new StringBuilder();
        for (String rawLine : block) {
            String line = normalizeLine(rawLine);
            if (!StringUtils.hasText(line)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(line);
        }
        return builder.toString();
    }

    private String normalizeLine(String rawLine) {
        if (!StringUtils.hasText(rawLine)) {
            return null;
        }
        String line = stripBom(rawLine).trim();
        if (!StringUtils.hasText(line) || isTableDivider(line)) {
            return null;
        }
        if (line.startsWith("|") && line.endsWith("|")) {
            line = line.substring(1, line.length() - 1).replace('|', ' ');
        }
        line = LIST_PREFIX_PATTERN.matcher(line).replaceFirst("");
        line = line.replace("`", "")
                .replace("**", "")
                .replace("__", "")
                .replace("#", "")
                .replace(">", "");
        line = line.replaceAll("\\s+", " ").trim();
        return StringUtils.hasText(line) ? line : null;
    }

    private boolean isTableDivider(String line) {
        String normalized = line.replace("|", "")
                .replace(":", "")
                .replace("-", "")
                .replace(" ", "")
                .trim();
        return normalized.isEmpty() && line.contains("-");
    }

    private String buildTitle(Path path, List<String> headings) {
        if (headings.isEmpty()) {
            return stripExtension(path.getFileName().toString());
        }
        if (headings.size() == 1) {
            return headings.get(0);
        }
        return headings.get(0) + " / " + headings.get(headings.size() - 1);
    }

    private List<String> buildKeywords(Path path, List<String> headings) {
        LinkedHashSet<String> keywords = new LinkedHashSet<String>();
        addKeywords(keywords, stripExtension(path.getFileName().toString()));
        for (String heading : headings) {
            addKeywords(keywords, heading);
        }
        return new ArrayList<String>(keywords);
    }

    private void addKeywords(Set<String> keywords, String text) {
        String normalized = normalizeText(text);
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        keywords.add(normalized);
        int maxSize = Math.min(4, normalized.length());
        for (int size = maxSize; size >= 2; size--) {
            for (int i = 0; i + size <= normalized.length(); i++) {
                String term = normalized.substring(i, i + size);
                if (!STOP_WORDS.contains(term)) {
                    keywords.add(term);
                }
            }
        }
    }

    private List<String> buildQueryTerms(String normalizedQuery) {
        LinkedHashSet<String> terms = new LinkedHashSet<String>();
        addKeywords(terms, normalizedQuery);
        return new ArrayList<String>(terms);
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replace("`", "")
                .replace("*", "")
                .replace("_", "")
                .replace("#", "")
                .replace(">", "")
                .replaceAll("\\s+", "")
                .toLowerCase();
    }

    private String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }

    private String stripBom(String text) {
        return text == null ? null : text.replace("\ufeff", "");
    }

    public static class SearchResult {
        private final List<KnowledgeSnippet> snippets;
        private final List<RetrievalHit> hits;
        private final String mode;
        private final double maxScore;
        private final String fallbackReason;
        private final String corpusVersion;

        public SearchResult(List<KnowledgeSnippet> snippets,
                            List<RetrievalHit> hits,
                            String mode,
                            double maxScore,
                            String fallbackReason,
                            String corpusVersion) {
            this.snippets = snippets == null ? Collections.<KnowledgeSnippet>emptyList() : snippets;
            this.hits = hits == null ? Collections.<RetrievalHit>emptyList() : hits;
            this.mode = mode;
            this.maxScore = maxScore;
            this.fallbackReason = fallbackReason;
            this.corpusVersion = corpusVersion;
        }

        public static SearchResult empty(String mode, String fallbackReason) {
            return new SearchResult(Collections.<KnowledgeSnippet>emptyList(), Collections.<RetrievalHit>emptyList(), mode, 0D, fallbackReason, null);
        }

        public List<KnowledgeSnippet> getSnippets() {
            return snippets;
        }

        public List<RetrievalHit> getHits() {
            return hits;
        }

        public String getMode() {
            return mode;
        }

        public double getMaxScore() {
            return maxScore;
        }

        public String getFallbackReason() {
            return fallbackReason;
        }

        public String getCorpusVersion() {
            return corpusVersion;
        }
    }

    public static class KnowledgeSnippet {
        private final String title;
        private final String content;
        private final List<String> keywords;
        private final String cacheKey;

        public KnowledgeSnippet(String title, String content, List<String> keywords) {
            this.title = title;
            this.content = content;
            this.keywords = keywords == null ? Collections.<String>emptyList() : keywords;
            this.cacheKey = Integer.toHexString((StringUtils.hasText(title) ? title : "")
                    .concat("::")
                    .concat(StringUtils.hasText(content) ? content : "").hashCode());
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        public List<String> getKeywords() {
            return keywords;
        }

        public String getCacheKey() {
            return cacheKey;
        }
    }

    private static class CorpusSnapshot {
        private final List<KnowledgeSnippet> snippets;
        private final String version;

        private CorpusSnapshot(List<KnowledgeSnippet> snippets, String version) {
            this.snippets = snippets;
            this.version = version;
        }
    }
}
