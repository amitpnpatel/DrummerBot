package Kmean;

/**
 * Created by amitp on 1/31/17.
 */
public class Point {
    private double[] vector;
    private int clusterNumber=0;
    private double[] confidence;
    public Point(double[] vector) {
        this.vector = vector;
    }

    public double[] getVector() {
        return vector;
    }

    public void setVector(double[] vector) {
        this.vector = vector;
    }

    public int getClusterNumber() {
        return clusterNumber;
    }

    public void setClusterNumber(int clusterNumber) {
        this.clusterNumber = clusterNumber;
    }

    public double[] getConfidence() {
        return confidence;
    }

    public void setConfidence(double[] confidence) {
        this.confidence = confidence;
    }

    public double getDistance(Point point){
        if (point.getVector().length!=this.getVector().length){
            return 0;
        }
        double distance=0;
        for(int counter=0;counter<point.getVector().length;counter++){
            distance+=Math.pow(point.getVector()[counter]-this.getVector()[counter],2);
        }
        return Math.sqrt(distance);
    }
}
