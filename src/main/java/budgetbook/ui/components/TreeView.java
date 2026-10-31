package budgetbook.ui.components;

import budgetbook.model.TreeStage;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/** 顯示一棵樹的元件。 */
public final class TreeView extends JComponent {
    private TreeStage stage = TreeStage.SEED;
    private final int size;

    public TreeView(int size) {
        this.size = size;
        setOpaque(false);
    }

    public void setStage(TreeStage stage) {
        this.stage = stage;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(size, size);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(size / 2, size / 2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        int side = Math.min(getWidth(), getHeight());
        TreePainter.paint((Graphics2D) g, (getWidth() - side) / 2, (getHeight() - side) / 2, side, side, stage);
    }
}
