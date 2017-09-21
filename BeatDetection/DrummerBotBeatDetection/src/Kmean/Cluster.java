package Kmean;

import java.util.ArrayList;

/**
 * Created by amitp on 1/31/17.
 */
public class Cluster {
    public ArrayList<Point> points = new ArrayList<>();
    public Point centroid;
    public int id;




    public Cluster(int id) {
        this.id = id;
    }

    public void addPoint(Point point){
        points.add(point);
    }

    public ArrayList<Point> getPoints() {
        return points;
    }

    public void setPoints(ArrayList<Point> points) {
        this.points = points;
    }

    public Point getCentroid() {
        return centroid;
    }

    public void setCentroid(Point centroid) {
        this.centroid = centroid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Point calculateCentroid() {
        if (points.size() == 0) {
            return null;
        }

        Point newCentroid = new Point(new double[points.get(0).getVector().length]);


        for(int outerCounter = 0;outerCounter<newCentroid.getVector().length;outerCounter++) {
            for(int counter = 0; counter < points.size(); counter++) {
               newCentroid.getVector()[outerCounter]+=points.get(counter).getVector()[outerCounter];
            }

            newCentroid.getVector()[outerCounter] /= points.size();
        }
        this.centroid=newCentroid;
        return centroid;
    }

    public void clearCluster(){
         this.points = new ArrayList<>();
    }
}
