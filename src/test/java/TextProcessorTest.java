import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class TextProcessorTest {

    private TextProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TextProcessor();
    }

    @Test
    @DisplayName("Tokenize: basic splitting and lowercase")
    void testTokenizeBasic() {
        List<String> tokens = processor.tokenize("Hello World Java");
        assertTrue(tokens.contains("hello"));
        assertTrue(tokens.contains("world"));
        assertTrue(tokens.contains("java"));
    }

    @Test
    @DisplayName("Tokenize: stop words are removed")
    void testStopWordsRemoved() {
        List<String> tokens = processor.tokenize("the quick brown fox");
        assertFalse(tokens.contains("the"));
        assertTrue(tokens.contains("quick"));
        assertTrue(tokens.contains("brown"));
    }

    @Test
    @DisplayName("Tokenize: short words under length 3 are removed")
    void testShortWordsRemoved() {
        List<String> tokens = processor.tokenize("a to be or not");
        assertTrue(tokens.isEmpty());
    }

    @Test
    @DisplayName("Tokenize: punctuation is handled correctly")
    void testPunctuation() {
        List<String> tokens = processor.tokenize("Java, Python! C++.");
        assertTrue(tokens.contains("java"));
        assertTrue(tokens.contains("python"));
        assertFalse(tokens.contains("java,"));
    }

    @Test
    @DisplayName("TermFrequency: counts are correct")
    void testTermFrequency() {
        List<String> tokens = Arrays.asList("java", "java", "python", "java");
        Map<String, Integer> tf = processor.termFrequency(tokens);
        assertEquals(3, tf.get("java"));
        assertEquals(1, tf.get("python"));
    }

    @Test
    @DisplayName("TermFrequency: empty input returns empty map")
    void testTermFrequencyEmpty() {
        Map<String, Integer> tf = processor.termFrequency(List.of());
        assertTrue(tf.isEmpty());
    }
}
