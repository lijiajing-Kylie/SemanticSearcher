import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

public class SearchGUI extends JFrame {

    // Color palette
    private static final Color BG        = new Color(15, 15, 25);
    private static final Color CARD      = new Color(28, 28, 42);
    private static final Color ACCENT    = new Color(99, 102, 241);
    private static final Color ACCENT2   = new Color(139, 92, 246);
    private static final Color TEXT      = new Color(240, 240, 255);
    private static final Color SUBTEXT   = new Color(140, 140, 180);
    private static final Color BORDER    = new Color(50, 50, 75);

    private final JTextField searchField;
    private final JButton searchButton;
    private final JPanel resultPanel;
    private final JLabel statusLabel;
    private SearchEngine engine;

    public SearchGUI() {
        try {
            DatabaseManager db = new DatabaseManager();
            db.connect();
            engine = new SearchEngine(db, new TextProcessor());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "DB error: " + e.getMessage());
        }

        setTitle("SemanticSearcher");
        setSize(780, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setBackground(BG);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(30, 40, 20, 40));

        // ── Title ──
        JLabel title = new JLabel("SemanticSearcher");
        title.setFont(new Font("Arial", Font.BOLD, 26));
        title.setForeground(TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setBorder(new EmptyBorder(0, 0, 6, 0));

        JLabel subtitle = new JLabel("BM25-powered full-text search engine");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitle.setForeground(SUBTEXT);
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG);
        headerPanel.add(title, BorderLayout.NORTH);
        headerPanel.add(subtitle, BorderLayout.CENTER);
        headerPanel.setBorder(new EmptyBorder(0, 0, 24, 0));

        // ── Search bar ──
        searchField = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
                super.paintComponent(g);
                g2.dispose();
            }
        };
        searchField.setFont(new Font("Arial", Font.PLAIN, 15));
        searchField.setForeground(TEXT);
        searchField.setCaretColor(ACCENT);
        searchField.setOpaque(false);
        searchField.setBorder(new EmptyBorder(10, 16, 10, 16));

        searchButton = new JButton("Search") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled() ? ACCENT : BORDER);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(TEXT);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        searchButton.setFont(new Font("Arial", Font.BOLD, 14));
        searchButton.setForeground(TEXT);
        searchButton.setOpaque(false);
        searchButton.setContentAreaFilled(false);
        searchButton.setBorderPainted(false);
        searchButton.setPreferredSize(new Dimension(100, 44));
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel searchBar = new JPanel(new BorderLayout(10, 0));
        searchBar.setBackground(BG);
        searchBar.add(searchField, BorderLayout.CENTER);
        searchBar.add(searchButton, BorderLayout.EAST);
        searchBar.setBorder(new EmptyBorder(0, 0, 20, 0));

        // ── Results panel ──
        resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setBackground(BG);

        JScrollPane scroll = new JScrollPane(resultPanel);
        scroll.setBackground(BG);
        scroll.getViewport().setBackground(BG);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));

        // ── Status bar ──
        statusLabel = new JLabel("Ready — enter a query above");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(SUBTEXT);
        statusLabel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // ── Assemble ──
        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(BG);
        topSection.add(headerPanel, BorderLayout.NORTH);
        topSection.add(searchBar, BorderLayout.CENTER);

        root.add(topSection, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);
        setContentPane(root);

        searchButton.addActionListener(e -> doSearch());
        searchField.addActionListener(e -> doSearch());
    }

    private void doSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        searchButton.setEnabled(false);
        statusLabel.setText("Searching...");
        resultPanel.removeAll();
        resultPanel.revalidate();
        resultPanel.repaint();

        new Thread(() -> {
            long start = System.currentTimeMillis();
            try {
                List<String> results = engine.search(query, 10);
                long elapsed = System.currentTimeMillis() - start;

                SwingUtilities.invokeLater(() -> {
                    resultPanel.removeAll();
                    if (results.isEmpty()) {
                        JLabel none = new JLabel("No results found.");
                        none.setForeground(SUBTEXT);
                        none.setBorder(new EmptyBorder(20, 20, 0, 0));
                        resultPanel.add(none);
                    } else {
                        for (int i = 0; i < results.size(); i++) {
                            resultPanel.add(makeCard(i + 1, results.get(i)));
                            resultPanel.add(Box.createVerticalStrut(8));
                        }
                    }
                    resultPanel.revalidate();
                    resultPanel.repaint();
                    statusLabel.setText("Found " + results.size()
                            + " results in " + elapsed + "ms");
                    searchButton.setEnabled(true);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Error: " + ex.getMessage());
                    searchButton.setEnabled(true);
                });
            }
        }).start();
    }

    private JPanel makeCard(int rank, String resultLine) {
        // resultLine format: "0.8072 | Machine_learning"
        String[] parts = resultLine.split(" \\| ", 2);
        String score = parts.length > 0 ? parts[0].trim() : "";
        String docTitle = parts.length > 1 ? parts[1].trim()
                .replace("_", " ") : resultLine;

        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(12, 16, 12, 16)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        // Rank badge
        JLabel rankLabel = new JLabel(String.valueOf(rank));
        rankLabel.setFont(new Font("Arial", Font.BOLD, 14));
        rankLabel.setForeground(ACCENT2);
        rankLabel.setPreferredSize(new Dimension(24, 24));

        // Title
        JLabel titleLabel = new JLabel(docTitle);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 15));
        titleLabel.setForeground(TEXT);

        // Score
        JLabel scoreLabel = new JLabel("score " + score);
        scoreLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        scoreLabel.setForeground(SUBTEXT);

        JPanel textPanel = new JPanel(new BorderLayout(0, 4));
        textPanel.setBackground(CARD);
        textPanel.add(titleLabel, BorderLayout.NORTH);
        textPanel.add(scoreLabel, BorderLayout.SOUTH);

        card.add(rankLabel, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);
        return card;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SearchGUI().setVisible(true));
    }
}