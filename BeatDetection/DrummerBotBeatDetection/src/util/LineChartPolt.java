package util;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

/**
 * Created by amitp on 2/2/17.
 */
public class LineChartPolt extends ApplicationFrame {


    public LineChartPolt(String applicationTitle, String chartTitle,double [] data) {
        super(applicationTitle);

        XYSeriesCollection dataset = new XYSeriesCollection();

        XYSeries series = new XYSeries("k"+1);
        for (int i = 0; i < data.length; i++) {

            series.add(i, data[i]);
        }
        dataset.addSeries(series);
        // Generate the graph
        generateTheGraph(chartTitle, dataset);
    }

    private void generateTheGraph(String chartTitle, XYSeriesCollection dataset) {
        JFreeChart chart = ChartFactory.createXYLineChart(
                chartTitle, // Title
                "x-axis", // x-axis Label
                "y-axis", // y-axis Label
                dataset, // Dataset
                PlotOrientation.VERTICAL, // Plot Orientation
                true, // Show Legend
                true, // Use tooltips
                false // Configure chart to generate URLs?
        );
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(560, 367));
        setContentPane(chartPanel);
        this.pack();
        RefineryUtilities.centerFrameOnScreen(this);
        this.setVisible(true);
    }



        public LineChartPolt(String applicationTitle, String chartTitle, double[][] data) {
            super(applicationTitle);

            XYSeriesCollection dataset = new XYSeriesCollection();
            for(int bincounter=0;bincounter<data.length;bincounter++){
                XYSeries series = new XYSeries("k"+bincounter);
                for (int i = 0; i < data[bincounter].length; i++) {

                    series.add(i, data[bincounter][i]);
                }

                // Add the series to your data set

                dataset.addSeries(series);
            }

            // Generate the graph
            generateTheGraph(chartTitle, dataset);
        }
}
