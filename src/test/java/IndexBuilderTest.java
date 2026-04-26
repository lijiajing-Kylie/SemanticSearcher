import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

public class IndexBuilderTest {

    private static DatabaseManager db;
    private static final String TEST_DIR = "test_pages";

    @BeforeAll
    static void setUp() throws Exception {
        // 创建测试用临时文件
        Files.createDirectories(Paths.get(TEST_DIR));
        Files.writeString(Paths.get(TEST_DIR + "/1_test.txt"),
                "URL: https://test.com\n\nJava is a programming language. " +
                        "Java is used for machine learning and algorithms.");

        db = new DatabaseManager();
        db.connect();
        db.createTables();

        IndexBuilder builder = new IndexBuilder(db, new TextProcessor());
        builder.buildIndex(TEST_DIR);
    }

    @AfterAll
    static void tearDown() throws Exception {
        db.close();
        // 清理测试文件
        Files.walk(Paths.get(TEST_DIR))
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> p.toFile().delete());
    }

    @Test
    @DisplayName("IndexBuilder: document is stored in Documents table")
    void testDocumentStored() throws Exception {
        ResultSet rs = db.getConnection()
                .createStatement()
                .executeQuery("SELECT COUNT(*) FROM Documents WHERE url = 'https://test.com'");
        assertTrue(rs.next());
        assertTrue(rs.getInt(1) > 0);
    }

    @Test
    @DisplayName("IndexBuilder: terms are stored in InvertedIndex table")
    void testTermsIndexed() throws Exception {
        ResultSet rs = db.getConnection()
                .createStatement()
                .executeQuery("SELECT COUNT(*) FROM InvertedIndex WHERE term = 'java'");
        assertTrue(rs.next());
        assertTrue(rs.getInt(1) > 0);
    }

    @Test
    @DisplayName("IndexBuilder: TF value is between 0 and 1")
    void testTFRange() throws Exception {
        ResultSet rs = db.getConnection()
                .createStatement()
                .executeQuery("SELECT tf FROM InvertedIndex WHERE term = 'java'");
        assertTrue(rs.next());
        double tf = rs.getDouble(1);
        assertTrue(tf > 0 && tf <= 1.0);
    }
}