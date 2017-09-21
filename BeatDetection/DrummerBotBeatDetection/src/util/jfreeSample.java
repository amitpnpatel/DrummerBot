package util; /**
 * Created by rajatvaryani on 2/1/17.
 */


import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import java.awt.*;



public class jfreeSample extends ApplicationFrame {

    public jfreeSample(String applicationTitle, String chartTitle, int[][] data,int[] data2) {
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
        XYSeries series = new XYSeries("k"+100);
        for (int i = 0; i < data2.length; i++) {

            series.add(i, data2[i]);
        }
        dataset.addSeries(series);
        // Generate the graph
        JFreeChart chart = ChartFactory.createXYLineChart(
                "XY Chart", // Title
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
    }

    public jfreeSample(String applicationTitle, String chartTitle, int[][] data) {
        super(applicationTitle);

        JFreeChart jfreechart = ChartFactory.createBubbleChart(
                "",
                "bin",
                "sample",
                createxyzDataset(data),
                PlotOrientation.HORIZONTAL,
                true, true, false);
        XYPlot xyplot = ( XYPlot )jfreechart.getPlot( );
        xyplot.setForegroundAlpha( 0.65F );
        XYItemRenderer xyitemrenderer = xyplot.getRenderer( );
        xyitemrenderer.setSeriesPaint( 0 , Color.red );
        NumberAxis numberaxis = ( NumberAxis )xyplot.getDomainAxis( );
        numberaxis.setLowerMargin( 0.2 );
        numberaxis.setUpperMargin( 0.5 );
        NumberAxis numberaxis1 = ( NumberAxis )xyplot.getRangeAxis( );
        numberaxis1.setLowerMargin( 0.8 );
        numberaxis1.setUpperMargin( 0.9 );
        ChartPanel chartPanel = new ChartPanel(jfreechart);
        chartPanel.setPreferredSize(new java.awt.Dimension(560, 367));
        setContentPane(chartPanel);
    }


    public jfreeSample(String applicationTitle, String chartTitle) {
        super(applicationTitle);
        JFreeChart lineChart = ChartFactory.createLineChart(
                chartTitle,
                "Frame number", "Magnitude",
                createDataset(),
                PlotOrientation.VERTICAL,
                true, true, false);

        ChartPanel chartPanel = new ChartPanel(lineChart);
        chartPanel.setPreferredSize(new java.awt.Dimension(560, 367));
        setContentPane(chartPanel);
    }





    private static JFreeChart createChart( XYZDataset xyzdataset )
    {
        JFreeChart jfreechart = ChartFactory.createBubbleChart(
                "",
                "bin",
                "Smaple",
                xyzdataset,
                PlotOrientation.HORIZONTAL,
                true, true, false);

        XYPlot xyplot = ( XYPlot )jfreechart.getPlot( );
        xyplot.setForegroundAlpha( 0.65F );
        XYItemRenderer xyitemrenderer = xyplot.getRenderer( );
        xyitemrenderer.setSeriesPaint( 0 , Color.red );
        NumberAxis numberaxis = ( NumberAxis )xyplot.getDomainAxis( );
        numberaxis.setLowerMargin( 0.2 );
        numberaxis.setUpperMargin( 0.5 );
        NumberAxis numberaxis1 = ( NumberAxis )xyplot.getRangeAxis( );
        numberaxis1.setLowerMargin( 0.8 );
        numberaxis1.setUpperMargin( 0.9 );

        return jfreechart;
    }

    public static XYZDataset createxyzDataset(int [][] data)
    {
        DefaultXYZDataset defaultxyzdataset = new DefaultXYZDataset();
        double[][] newData= new double[data.length][data[0].length];
       for(int counter=0;counter<newData.length;counter++){
           for(int innerCounter=0;innerCounter<newData[counter].length;innerCounter++){
               newData[counter][innerCounter] = (1.0/(counter+1)) * data[counter][innerCounter];

           }
       }
        double[] adx = new double[newData.length*newData[0].length];
        double []ady = new double[newData.length*newData[0].length];
        double []adz = new double[newData.length*newData[0].length];
        for(int i=0;i<adx.length;i++){
            ady[i]=i%newData[0].length;
            adx[i]=i/newData[0].length;
            adz[i]=newData[i/newData[0].length][i%newData[0].length];

        }
        double [][] ad3 = { adx , ady , adz };

        defaultxyzdataset.addSeries( "Series 1" ,ad3 );

        return defaultxyzdataset;
    }

    public static void main(String[] args) {
        jfreeSample chart = new jfreeSample(
                "Demo Chart",
                "Demo Chart");

        //chart.pack();
        RefineryUtilities.centerFrameOnScreen(chart);
        chart.setVisible(true);
    }

    private DefaultCategoryDataset createDataset() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int differentoator = 0;
        for (int outerIndex = 0; outerIndex < 5; outerIndex++) {

            for (int innerIndex = 0; innerIndex < 5; innerIndex++) {
                dataset.addValue(Math.random(), differentoator + "", "" + innerIndex);
            }
            differentoator++;
        }

        return dataset;
    }

    public DefaultCategoryDataset createDataset(int[][] data) {

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int outerIndex = 0; outerIndex < 3; outerIndex++) {

            for (int innerIndex = 0; innerIndex < data[outerIndex].length; innerIndex++) {
                dataset.addValue(data[outerIndex][innerIndex], (outerIndex + 1) + "", "" + innerIndex);
            }
        }

        return dataset;
    }
}
