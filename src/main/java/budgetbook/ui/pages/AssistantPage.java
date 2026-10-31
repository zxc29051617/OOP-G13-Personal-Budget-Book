package budgetbook.ui.pages;

import budgetbook.event.Topic;
import budgetbook.service.assistant.QueryAssistantService;
import budgetbook.ui.Page;
import budgetbook.ui.UiContext;
import budgetbook.ui.components.Card;
import budgetbook.ui.components.ScrollablePanel;
import budgetbook.ui.components.Widgets;
import budgetbook.ui.components.WrapLayout;
import budgetbook.ui.theme.Fonts;
import budgetbook.ui.theme.Palette;
import com.formdev.flatlaf.FlatClientProperties;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.EnumSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * 查詢助手：用中文問自己的記帳資料，例如「上個月餐飲花多少」。
 * 以規則判斷問題，答案都是從資料庫算出來的，不會連到外部 AI 服務。
 */
public final class AssistantPage extends Page {
    private final ScrollablePanel transcript = new ScrollablePanel(null);
    private final JScrollPane scroll;
    private final JTextField input = new JTextField();

    public AssistantPage(UiContext ui) {
        super(ui);
        add(header("用中文問問題，答案來自你的記帳資料（不使用外部 AI 服務）"), BorderLayout.NORTH);
        transcript.setLayout(new BoxLayout(transcript, BoxLayout.Y_AXIS));
        transcript.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        scroll = Widgets.scroll(transcript);

        JPanel chips = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 6));
        chips.setOpaque(false);
        for (String question : QueryAssistantService.SAMPLE_QUESTIONS) {
            JButton chip = Widgets.button(question, () -> ask(question));
            chip.putClientProperty(FlatClientProperties.STYLE, "arc:999;margin:3,10,3,10");
            chips.add(chip);
        }
        input.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "例如：上個月交通花了多少？");
        input.addActionListener(e -> ask(input.getText()));
        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        inputRow.add(input, BorderLayout.CENTER);
        inputRow.add(Widgets.primary("送出", () -> ask(input.getText())), BorderLayout.EAST);

        Card card = new Card(new BorderLayout(0, 10));
        card.add(scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.add(chips, BorderLayout.NORTH);
        bottom.add(inputRow, BorderLayout.SOUTH);
        card.add(bottom, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
        addMessage(QueryAssistantService.helpText(), false);
    }

    /** 問一個問題並把問答加入對話紀錄。 */
    public void ask(String question) {
        if (question == null || question.isBlank()) {
            return;
        }
        addMessage(question.strip(), true);
        addMessage(ui.app().assistant().answer(question), false);
        input.setText("");
        transcript.revalidate();
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum()));
    }

    private void addMessage(String text, boolean fromUser) {
        Bubble bubble = new Bubble(text, fromUser);
        JPanel row = new JPanel(new FlowLayout(fromUser ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(bubble);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        transcript.add(row);
        transcript.add(Box.createVerticalStrut(8));
    }

    @Override
    public void onShown() {
        input.requestFocusInWindow();
    }

    @Override
    public String id() {
        return "assistant";
    }

    @Override
    public String title() {
        return "助手";
    }

    @Override
    public String glyph() {
        return "問";
    }

    @Override
    public Set<Topic> topics() {
        return EnumSet.noneOf(Topic.class);
    }

    @Override
    protected void reload() {
        // 對話紀錄保留；每次提問都會即時查詢最新資料
    }

    /** 對話泡泡：以 HTML 標籤固定最大寬度，長句會自動換行。 */
    private static final class Bubble extends JLabel {
        private static final int MAX_WIDTH = 520;
        private final boolean fromUser;

        Bubble(String text, boolean fromUser) {
            this.fromUser = fromUser;
            setFont(Fonts.regular());
            setBorder(BorderFactory.createEmptyBorder(9, 13, 9, 13));
            FontMetrics metrics = getFontMetrics(getFont());
            int natural = text.lines().mapToInt(metrics::stringWidth).max().orElse(40);
            String html = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
            setText("<html><div style='width:" + Math.min(MAX_WIDTH, natural + 4) + "px'>" + html + "</div></html>");
        }

        @Override
        public Color getForeground() {
            return fromUser ? Color.WHITE : Palette.text();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fromUser ? Palette.accent() : Palette.accentSoft());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
