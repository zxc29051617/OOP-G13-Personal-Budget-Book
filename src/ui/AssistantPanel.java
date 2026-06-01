package ui;

import service.QueryAssistantService;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class AssistantPanel extends RefreshablePanel {
    private static final String[] SUGGESTED_QUESTIONS = {
            "本月花最多的是哪個分類？",
            "本月支出多少？",
            "我還剩多少預算？",
            "目前總餘額是多少？",
            "我有幾個錢包？",
            "本月小樹狀態？",
            "最近一筆交易是什麼？"
    };

    private final BudgetBookFrame frame;
    private final JTextField question = new JTextField();
    private final JTextArea answer = new JTextArea(9, 20);
    private QueryAssistantService assistant;

    public AssistantPanel(BudgetBookFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(Ui.SURFACE);

        Ui.styleInput(question);
        question.addActionListener(e -> ask(question.getText()));

        answer.setEditable(false);
        answer.setLineWrap(true);
        answer.setWrapStyleWord(true);
        answer.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        answer.setForeground(Ui.TEXT);
        answer.setBackground(Ui.CARD);
        answer.setBorder(null);

        JPanel body = Ui.page();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(buildHeader());
        body.add(Box.createVerticalStrut(16));
        body.add(buildSuggestions());
        body.add(Box.createVerticalStrut(12));
        body.add(buildQuestionBox());
        body.add(Box.createVerticalStrut(12));
        body.add(buildAnswerBox());

        add(Ui.scroll(body), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        assistant = new QueryAssistantService(frame.getStore(), frame.getStats());
        if (answer.getText().isBlank()) {
            answer.setText(assistant.answer(""));
        }
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setOpaque(false);
        text.add(Ui.appName(), BorderLayout.NORTH);
        text.add(Ui.title("查詢助手"), BorderLayout.CENTER);
        text.add(Ui.small("快速查看目前財務狀態"), BorderLayout.SOUTH);
        header.add(text, BorderLayout.WEST);
        header.add(frame.settingsButton(), BorderLayout.EAST);
        header.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 86));
        return header;
    }

    private JPanel buildSuggestions() {
        JPanel card = Ui.tintedCard(Ui.MINT);
        card.setLayout(new BorderLayout(0, 12));
        card.add(Ui.sectionTitle("常用問題"), BorderLayout.NORTH);

        JPanel questions = new JPanel(new GridLayout(0, 1, 0, 8));
        questions.setOpaque(false);
        for (String suggestedQuestion : SUGGESTED_QUESTIONS) {
            JButton button = Ui.quietButton(suggestedQuestion);
            button.setHorizontalAlignment(JButton.LEFT);
            button.addActionListener(e -> {
                question.setText(suggestedQuestion);
                ask(suggestedQuestion);
            });
            questions.add(button);
        }
        card.add(questions, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildQuestionBox() {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(10, 10));
        card.add(Ui.sectionTitle("自己輸入問題"), BorderLayout.NORTH);

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(question, BorderLayout.CENTER);
        JButton ask = Ui.primaryButton("詢問");
        ask.addActionListener(e -> ask(question.getText()));
        row.add(ask, BorderLayout.EAST);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildAnswerBox() {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 10));
        card.add(Ui.sectionTitle("查詢結果"), BorderLayout.NORTH);
        card.add(Ui.scroll(answer), BorderLayout.CENTER);
        return card;
    }

    private void ask(String text) {
        if (assistant == null) {
            assistant = new QueryAssistantService(frame.getStore(), frame.getStats());
        }
        answer.setText(assistant.answer(text));
        answer.setCaretPosition(0);
    }
}
