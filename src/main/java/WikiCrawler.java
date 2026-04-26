import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WikiCrawler {

    private static final int TARGET_COUNT  = 1000;
    private static final int THREAD_COUNT  = 4;
    private static final String OUTPUT_DIR = "wiki_pages";
    private static final String START_URL  =
            "https://en.wikipedia.org/wiki/Java_(programming_language)";

    private final BlockingQueue<String> queue      = new LinkedBlockingQueue<>();
    private final Set<String>           visited    = ConcurrentHashMap.newKeySet();
    private final AtomicInteger         savedCount = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        new WikiCrawler().start();
    }

    public void start() throws Exception {
        Files.createDirectories(Paths.get(OUTPUT_DIR));

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(this::crawlLoop);
        }

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.HOURS);
        System.out.println("✅ Done! Total saved: " + savedCount.get() + " pages");
    }
    private void crawlLoop() {
        while (savedCount.get() < TARGET_COUNT) {
            String url = null;
            try {
                // 优先从队列取，队列空了就请求随机文章
                url = queue.poll(1, TimeUnit.SECONDS);
                if (url == null) {
                    url = "https://en.wikipedia.org/api/rest_v1/page/random/summary";
                }

                String apiUrl = url.contains("/api/rest_v1/") ? url :
                        url.replace("https://en.wikipedia.org/wiki/",
                                "https://en.wikipedia.org/api/rest_v1/page/summary/");

                Document doc = Jsoup.connect(apiUrl)
                        .userAgent("Mozilla/5.0")
                        .ignoreContentType(true)
                        .timeout(5000)
                        .get();

                String body = doc.body().text();
                if (body.isBlank() || body.length() < 100) continue;

                // 从返回的JSON里提取title
                String title = "unknown";
                if (body.contains("\"title\":\"")) {
                    title = body.split("\"title\":\"")[1].split("\"")[0]
                            .replace(" ", "_");
                }

                // 去重：同一个title不重复保存
                if (!visited.add(title)) continue;

                int myIndex = savedCount.incrementAndGet();
                if (myIndex > TARGET_COUNT) break;

                String cleanTitle = title.substring(0, Math.min(50, title.length()));
                String filename = OUTPUT_DIR + "/" + myIndex + "_" + cleanTitle + ".txt";
                Files.writeString(Paths.get(filename), "URL: https://en.wikipedia.org/wiki/"
                                + title + "\n\n" + body,
                        java.nio.charset.StandardCharsets.UTF_8);

                System.out.printf("[%d/%d] Saved: %s%n", myIndex, TARGET_COUNT, cleanTitle);

                Thread.sleep(300);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Failed: " + url + " -> " + e.getMessage());
            }
        }
    }
}