package budgetbook.ui.components;

import budgetbook.model.BalancePoint;
import budgetbook.model.CategoryTotal;
import budgetbook.model.Money;
import budgetbook.model.PeriodTotals;
import budgetbook.ui.theme.Fonts;
import budgetbook.ui.theme.Palette;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.CategorySeries;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.PieSeries;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.AxesChartStyler;
import org.knowm.xchart.style.PieStyler;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Factory：建立外觀一致的 XChart 圖表（字型、顏色、背景都跟隨目前主題）。
 */
public final class Charts {
    private Charts() {
    }

    /** 分類占比甜甜圈圖，中央顯示總額；超過 {@code maxSlices} 的小分類合併成「其他」。 */
    public static PieChart donut(List<CategoryTotal> totals, int maxSlices) {
        PieChart chart = new PieChartBuilder().width(420).height(300).build();
        PieStyler styler = chart.getStyler();
        base(styler);
        styler.setDefaultSeriesRenderStyle(PieSeries.PieSeriesRenderStyle.Donut);
        styler.setDonutThickness(0.36);
        styler.setCircular(true);
        styler.setPlotContentSize(0.92);
        styler.setStartAngleInDegrees(90);
        styler.setLabelType(PieStyler.LabelType.Percentage);
        styler.setLabelsDistance(0.82);
        styler.setLabelsFont(Fonts.of(Font.BOLD, 0.78f));
        styler.setLabelsFontColorAutomaticEnabled(true);
        styler.setLabelsFontColorAutomaticLight(Color.WHITE);
        styler.setLabelsFontColorAutomaticDark(new Color(0x283028));
        styler.setSumVisible(true);
        styler.setSumFormat("$%,.0f");
        styler.setSumFont(Fonts.of(Font.BOLD, 1.15f));
        styler.setSliceBorderWidth(2);
        styler.setLegendPosition(Styler.LegendPosition.OutsideE);
        styler.setDecimalPattern("#,##0");

        List<CategoryTotal> shown = totals.size() > maxSlices ? totals.subList(0, maxSlices - 1) : totals;
        for (CategoryTotal total : shown) {
            PieSeries series = chart.addSeries(total.category().name(), total.total().toDouble());
            series.setFillColor(Palette.of(total.category().color()));
        }
        if (totals.size() > maxSlices) {
            Money rest = Money.sum(totals.subList(maxSlices - 1, totals.size()).stream().map(CategoryTotal::total));
            chart.addSeries("其他 " + (totals.size() - maxSlices + 1) + " 類", rest.toDouble())
                    .setFillColor(Palette.muted());
        }
        return chart;
    }

    /** 收入與支出的分組長條圖。 */
    public static CategoryChart incomeExpenseBars(List<PeriodTotals> totals) {
        CategoryChart chart = new CategoryChartBuilder().width(480).height(260).build();
        axes(chart.getStyler());
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        chart.getStyler().setAvailableSpaceFill(0.62);
        List<String> labels = totals.stream().map(PeriodTotals::label).toList();
        chart.addSeries("收入", labels, totals.stream().map(t -> t.income().toDouble()).toList())
                .setFillColor(Palette.income());
        chart.addSeries("支出", labels, totals.stream().map(t -> t.expense().toDouble()).toList())
                .setFillColor(Palette.expense());
        return chart;
    }

    /** 每日支出長條圖（月報用，收入多半集中在發薪日，放在一起會看不清楚）。 */
    public static CategoryChart expenseBars(List<PeriodTotals> totals) {
        CategoryChart chart = new CategoryChartBuilder().width(480).height(260).build();
        axes(chart.getStyler());
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setAvailableSpaceFill(0.7);
        List<String> labels = totals.stream().map(t -> Integer.toString(t.start().getDayOfMonth())).toList();
        CategorySeries series = chart.addSeries("支出", labels,
                totals.stream().map(t -> t.expense().toDouble()).toList());
        series.setFillColor(Palette.expense());
        return chart;
    }

    /** 總資產趨勢折線（面積）圖。 */
    public static XYChart balanceLine(List<BalancePoint> points) {
        XYChart chart = new XYChartBuilder().width(480).height(240).build();
        axes(chart.getStyler());
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setDatePattern("M/d");
        chart.getStyler().setXAxisMaxLabelCount(8);
        chart.getStyler().setYAxisMin((Double) null);
        List<Date> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        for (BalancePoint point : points) {
            x.add(Date.from(point.date().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            y.add(point.balance().toDouble());
        }
        XYSeries series = chart.addSeries("總資產", x, y);
        series.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Area);
        series.setMarker(SeriesMarkers.NONE);
        series.setLineColor(Palette.accent());
        series.setLineStyle(new BasicStroke(2.2f));
        Color fill = Palette.accent();
        series.setFillColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 48));
        return chart;
    }

    private static void base(Styler styler) {
        styler.setChartBackgroundColor(Palette.card());
        styler.setPlotBackgroundColor(Palette.card());
        styler.setPlotBorderVisible(false);
        styler.setChartFontColor(Palette.text());
        styler.setBaseFont(Fonts.regular());
        styler.setLegendFont(Fonts.small());
        styler.setLegendBackgroundColor(Palette.card());
        styler.setLegendBorderColor(Palette.card());
        styler.setChartTitleVisible(false);
        styler.setChartPadding(2);
        styler.setAntiAlias(true);
        styler.setToolTipsEnabled(false);
    }

    private static void axes(AxesChartStyler styler) {
        base(styler);
        styler.setAxisTickLabelsFont(Fonts.small());
        styler.setAxisTickLabelsColor(Palette.muted());
        styler.setPlotGridLinesColor(Palette.border());
        styler.setPlotGridVerticalLinesVisible(false);
        styler.setAxisTicksMarksVisible(false);
        styler.setAxisTicksLineVisible(false);
        styler.setXAxisTitleVisible(false);
        styler.setYAxisTitleVisible(false);
        styler.setYAxisMin(0.0);
        styler.setyAxisTickLabelsFormattingFunction(Charts::compact);
        styler.setPlotMargin(4);
    }

    /** 軸上的金額縮寫：12,500 → 1.3萬。 */
    static String compact(Double value) {
        double v = value == null ? 0 : value;
        if (Math.abs(v) >= 10_000) {
            double wan = v / 10_000;
            return (wan == Math.rint(wan) ? String.format("%.0f", wan) : String.format("%.1f", wan)) + "萬";
        }
        return String.format("%,.0f", v);
    }
}
