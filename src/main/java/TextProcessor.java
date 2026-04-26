import java.util.*;
import java.util.regex.*;

public class TextProcessor {

    // 英文停用词，去掉这些没意义的词
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "shall", "can", "need", "dare", "ought",
            "in", "on", "at", "to", "for", "of", "with", "by", "from", "as",
            "into", "through", "during", "before", "after", "above", "below",
            "and", "or", "but", "if", "while", "although", "because", "since",
            "this", "that", "these", "those", "it", "its", "i", "you", "he",
            "she", "we", "they", "what", "which", "who", "not", "no", "also"
    ));

    /**
     * 分词：把一段文本切成词的列表
     * 例如："Hello World" -> ["hello", "world"]
     */
    public List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        // 只保留字母，转小写，按非字母字符切分
        String[] words = text.toLowerCase().split("[^a-zA-Z]+");
        for (String word : words) {
            if (word.length() > 2 && !STOP_WORDS.contains(word)) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    /**
     * 词频统计（TF）：统计每个词出现了几次
     * 例如：["java", "java", "code"] -> {java=2, code=1}
     */
    public Map<String, Integer> termFrequency(List<String> tokens) {
        Map<String, Integer> tf = new HashMap<>();
        for (String token : tokens) {
            tf.put(token, tf.getOrDefault(token, 0) + 1);
        }
        return tf;
    }

    // 测试入口
    public static void main(String[] args) {
        TextProcessor processor = new TextProcessor();

        String text = "Java is a high-level, class-based, object-oriented programming language. " +
                "Java is widely used for building web applications and Android apps.";

        List<String> tokens = processor.tokenize(text);
        System.out.println("Tokens: " + tokens);

        Map<String, Integer> tf = processor.termFrequency(tokens);
        // 按词频排序输出
        tf.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
    }
}