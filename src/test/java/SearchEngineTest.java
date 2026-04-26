import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class SearchEngineTest {

    private static DatabaseManager db;
    private static SearchEngine engine;

    @BeforeAll
    static void setUp() throws Exception {
        db = new DatabaseManager();
        db.connect();
        engine = new SearchEngine(db, new TextProcessor());
    }

    @AfterAll
    static void tearDown() throws Exception {
        db.close();
    }

    @Test
    @DisplayName("Search: returns results for valid query")
    void testSearchReturnsResults() throws Exception {
        List<String> results = engine.search("java programming", 5);
        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("Search: results are sorted by score descending")
    void testSearchSortedByScore() throws Exception {
        List<String> results = engine.search("machine learning", 5);
        if (results.size() < 2) return;
        double first  = Double.parseDouble(results.get(0).split(" \\| ")[0]);
        double second = Double.parseDouble(results.get(1).split(" \\| ")[0]);
        assertTrue(first >= second);
    }

    @Test
    @DisplayName("Search: no duplicate titles in results")
    void testNoDuplicateTitles() throws Exception {
        List<String> results = engine.search("machine learning algorithm", 10);
        long uniqueCount = results.stream()
                .map(r -> r.split(" \\| ")[1])
                .distinct()
                .count();
        assertEquals(results.size(), uniqueCount);
    }

    @Test
    @DisplayName("Search: empty query returns no results")
    void testEmptyQuery() throws Exception {
        List<String> results = engine.search("", 5);
        assertTrue(results.isEmpty() || results.get(0).equals("No results found."));
    }

    @Test
    @DisplayName("Search: unknown query returns no results found")
    void testUnknownQuery() throws Exception {
        List<String> results = engine.search("xyzxyzxyz123456", 5);
        assertEquals("No results found.", results.get(0));
    }

    @Test
    @DisplayName("Search: topK limits result count")
    void testTopKLimit() throws Exception {
        List<String> results = engine.search("computer science", 3);
        assertTrue(results.size() <= 3);
    }
}