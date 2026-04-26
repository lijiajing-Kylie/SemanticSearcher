import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class IndexBuilder {

    private final DatabaseManager db;
    private final TextProcessor processor;

    public IndexBuilder(DatabaseManager db, TextProcessor processor) {
        this.db = db;
        this.processor = processor;
    }

    /**
     * 读取 wiki_pages 下所有 .txt 文件，建立倒排索引存入数据库
     */
    public void buildIndex(String pagesDir) throws Exception {
        File folder = new File(pagesDir);
        File[] files = folder.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null || files.length == 0) {
            System.out.println("No files found in " + pagesDir);
            return;
        }

        System.out.println("Found " + files.length + " files, building index...");

        for (File file : files) {
            String raw = Files.readString(file.toPath());

            // 第一行是URL，剩下是正文
            String[] parts = raw.split("\n\n", 2);
            String url     = parts[0].replace("URL: ", "").trim();
            String content = parts.length > 1 ? parts[1] : "";

            String title = file.getName().replaceAll("^\\d+_", "").replace(".txt", "");

            // 1. 存入 Documents 表
            int docId = insertDocument(url, title, content);

            // 2. 分词 + 词频
            List<String> tokens = processor.tokenize(content);
            Map<String, Integer> tf = processor.termFrequency(tokens);

            // 3. 存入 InvertedIndex 表
            insertTerms(docId, tf, tokens.size());

            System.out.println("Indexed: " + title + " (" + tf.size() + " unique terms)");
        }

        System.out.println("Index build complete!");
    }

    private int insertDocument(String url, String title, String content) throws SQLException {
        String sql = "INSERT OR IGNORE INTO Documents (url, title, content, length) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, url);
            ps.setString(2, title);
            ps.setString(3, content);
            ps.setInt(4, content.split("\\s+").length);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private void insertTerms(int docId, Map<String, Integer> tf, int totalTerms) throws SQLException {
        String sql = "INSERT OR IGNORE INTO InvertedIndex (term, doc_id, tf) VALUES (?, ?, ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            for (Map.Entry<String, Integer> entry : tf.entrySet()) {
                double normalizedTf = (double) entry.getValue() / totalTerms;
                ps.setString(1, entry.getKey());
                ps.setInt(2, docId);
                ps.setDouble(3, normalizedTf);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public static void main(String[] args) throws Exception {
        DatabaseManager db = new DatabaseManager();
        db.connect();
        db.createTables();

        IndexBuilder builder = new IndexBuilder(db, new TextProcessor());
        builder.buildIndex("wiki_pages");

        db.close();
    }
}