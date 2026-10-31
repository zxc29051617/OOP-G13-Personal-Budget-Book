package budgetbook.model;

/**
 * 種樹遊戲化：每記一筆帳就是澆一次水，依本月澆水次數決定小樹的成長階段。
 * 沿用原版規則：0 次枯萎、達到目標（預設 20 次）完成一棵樹，中間再細分成四個階段。
 */
public enum TreeStage {
    WITHERED("枯萎"),
    SEED("種子"),
    SPROUT("發芽"),
    SAPLING("幼苗"),
    YOUNG_TREE("小樹"),
    FULL_TREE("大樹");

    private final String displayName;

    TreeStage(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static TreeStage of(int records, int goal) {
        if (records <= 0) {
            return WITHERED;
        }
        if (records >= goal) {
            return FULL_TREE;
        }
        double ratio = records / (double) goal;
        if (ratio < 0.25) {
            return SEED;
        }
        if (ratio < 0.5) {
            return SPROUT;
        }
        return ratio < 0.75 ? SAPLING : YOUNG_TREE;
    }

    /** 階段加上原版的狀態，例如「小樹（成長中）」；枯萎只顯示一次。 */
    public String label() {
        return this == WITHERED ? displayName : displayName + "（" + statusText() + "）";
    }

    /** 原版的三種狀態文字：枯萎、成長中、完成一棵樹。 */
    public String statusText() {
        return switch (this) {
            case WITHERED -> "枯萎";
            case FULL_TREE -> "完成一棵樹";
            default -> "成長中";
        };
    }
}
