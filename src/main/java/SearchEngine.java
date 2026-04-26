import java.sql.*;
import java.util.*;

public class SearchEngine {

    private final DatabaseManager db;
    private final TextProcessor processor;

    public SearchEngine(DatabaseManager db, TextProcessor processor) {
        this.db = db;
        this.processor = processor;
    }

    /**
     * 查询入口：输入搜索词，返回候选文档ID列表
     */
    public List<Integer> getCandidateDocs(String query) throws SQLException {
        // 第一步：把用户输入分词
        List<String> queryTokens = processor.tokenize(query);
        System.out.println("Query tokens: " + queryTokens);

        // 第二步：去数据库查每个词出现在哪些文章里
        Set<Integer> docIds = new LinkedHashSet<>();
        String sql = "SELECT DISTINCT doc_id FROM InvertedIndex WHERE term = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            for (String token : queryTokens) {
                ps.setString(1, token);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    docIds.add(rs.getInt("doc_id"));
                }
            }
        }

        return new ArrayList<>(docIds);
    }
    /**
     * BM25 核心：对候选文档打分，返回 Top-K 结果
     */
    public List<String> search(String query, int topK) throws SQLException {
        List<Integer> candidates = getCandidateDocs(query);
        if (candidates.isEmpty()) return List.of("No results found.");

        List<String> queryTokens = processor.tokenize(query);

        double k1 = 1.5, b = 0.75;
        int totalDocs      = getTotalDocs();
        double avgLength   = getAvgDocLength();

        // 批量取文档长度（1条SQL搞定，不是N条）
        Map<Integer, Integer> docLengths = getBatchDocLengths(candidates);

        // 批量取TF值
        Map<String, Map<Integer, Double>> tfMap = getBatchTF(queryTokens, candidates);

        // 批量取DF值
        Map<String, Integer> dfMap = getBatchDF(queryTokens);

        // 批量取文档标题（同时去重，title相同只保留最高分）
        Map<Integer, String> docTitles = getBatchTitles(candidates);

        // 用Map去重：同一个title只保留最高分
        Map<String, Double> titleScores = new HashMap<>();

        for (int docId : candidates) {
            double score = 0.0;
            int docLength = docLengths.getOrDefault(docId, 1);

            for (String term : queryTokens) {
                double tf = tfMap.getOrDefault(term, Map.of()).getOrDefault(docId, 0.0);
                int df    = dfMap.getOrDefault(term, 0);
                if (df == 0 || tf == 0) continue;

                double idf     = Math.log((totalDocs - df + 0.5) / (df + 0.5) + 1);
                double tfNorm  = (tf * (k1 + 1)) /
                        (tf + k1 * (1 - b + b * docLength / avgLength));
                score += idf * tfNorm;
            }

            String title = docTitles.getOrDefault(docId, "Unknown");
            // 同标题只保留最高分（去重核心）
            titleScores.merge(title, score, Math::max);
        }

        // PriorityQueue Top-K
        PriorityQueue<Map.Entry<String, Double>> heap = new PriorityQueue<>(
                Comparator.comparingDouble(Map.Entry::getValue));

        for (Map.Entry<String, Double> entry : titleScores.entrySet()) {
            heap.offer(entry);
            if (heap.size() > topK) heap.poll();
        }

        List<Map.Entry<String, Double>> results = new ArrayList<>(heap);
        results.sort((a, b2) -> Double.compare(b2.getValue(), a.getValue()));

        return results.stream()
                .map(e -> String.format("%.4f | %s", e.getValue(), e.getKey()))
                .toList();
    }

// ── 批量查询方法（性能关键）──────────────────────

    private Map<Integer, Integer> getBatchDocLengths(List<Integer> docIds) throws SQLException {
        Map<Integer, Integer> map = new HashMap<>();
        String ids = docIds.toString().replace("[", "(").replace("]", ")");
        String sql = "SELECT id, length FROM Documents WHERE id IN " + ids;
        ResultSet rs = db.getConnection().createStatement().executeQuery(sql);
        while (rs.next()) map.put(rs.getInt("id"), rs.getInt("length"));
        return map;
    }

    private Map<String, Map<Integer, Double>> getBatchTF(
            List<String> terms, List<Integer> docIds) throws SQLException {
        Map<String, Map<Integer, Double>> map = new HashMap<>();
        String ids = docIds.toString().replace("[", "(").replace("]", ")");
        String placeholders = String.join(",", terms.stream().map(t -> "?").toList());
        String sql = "SELECT term, doc_id, tf FROM InvertedIndex " +
                "WHERE term IN (" + placeholders + ") AND doc_id IN " + ids;
        PreparedStatement ps = db.getConnection().prepareStatement(sql);
        for (int i = 0; i < terms.size(); i++) ps.setString(i + 1, terms.get(i));
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            map.computeIfAbsent(rs.getString("term"), k -> new HashMap<>())
                    .put(rs.getInt("doc_id"), rs.getDouble("tf"));
        }
        return map;
    }

    private Map<String, Integer> getBatchDF(List<String> terms) throws SQLException {
        Map<String, Integer> map = new HashMap<>();
        String placeholders = String.join(",", terms.stream().map(t -> "?").toList());
        String sql = "SELECT term, COUNT(*) as df FROM InvertedIndex " +
                "WHERE term IN (" + placeholders + ") GROUP BY term";
        PreparedStatement ps = db.getConnection().prepareStatement(sql);
        for (int i = 0; i < terms.size(); i++) ps.setString(i + 1, terms.get(i));
        ResultSet rs = ps.executeQuery();
        while (rs.next()) map.put(rs.getString("term"), rs.getInt("df"));
        return map;
    }

    private Map<Integer, String> getBatchTitles(List<Integer> docIds) throws SQLException {
        Map<Integer, String> map = new HashMap<>();
        String ids = docIds.toString().replace("[", "(").replace("]", ")");
        String sql = "SELECT id, title FROM Documents WHERE id IN " + ids;
        ResultSet rs = db.getConnection().createStatement().executeQuery(sql);
        while (rs.next()) map.put(rs.getInt("id"), rs.getString("title"));
        return map;
    }

    private int getTotalDocs() throws SQLException {
        ResultSet rs = db.getConnection()
                .createStatement().executeQuery("SELECT COUNT(*) FROM Documents");
        return rs.next() ? rs.getInt(1) : 1;
    }

    private double getAvgDocLength() throws SQLException {
        ResultSet rs = db.getConnection()
                .createStatement().executeQuery("SELECT AVG(length) FROM Documents");
        return rs.next() ? rs.getDouble(1) : 1.0;
    }

    public static void main(String[] args) throws Exception {
        DatabaseManager db = new DatabaseManager();
        db.connect();

        SearchEngine engine = new SearchEngine(db, new TextProcessor());
        List<String> results = engine.search("machine learning algorithm", 5);

        System.out.println("\n=== Top 5 Results ===");
        results.forEach(System.out::println);

        db.close();
    }
}