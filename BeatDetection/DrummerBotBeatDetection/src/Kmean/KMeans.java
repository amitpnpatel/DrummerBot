package Kmean;

/**
 * Created by amitp on 1/31/17.
 */
public class KMeans {

    private int numberOfClusters;
    private Point[] points;
    private Cluster[] clusters;
    private int MAX_ITERATIONS = 20;



    public KMeans(int numberOfClusters, Point[] points) {
        this.numberOfClusters = numberOfClusters;
        this.points = points;
        initialiseClusters();
    }

    private void initialiseClusters(){
        clusters = new Cluster[numberOfClusters];

        for(int counter = 0;counter<numberOfClusters;counter++){
            clusters[counter]=new Cluster(counter+1);
            if(points.length>numberOfClusters){
               clusters[counter].setCentroid(points[(points.length/numberOfClusters) * counter]);
           }

        }
    }

    public int getNumberOfClusters() {
        return numberOfClusters;
    }

    public Point[] getPoints() {
        return points;
    }

    public Cluster[] getClusters() {
        return clusters;
    }

    public void setNumberOfClusters(int numberOfClusters) {
        this.numberOfClusters = numberOfClusters;
    }

    public void setPoints(Point[] points) {
        this.points = points;
    }

    public void setClusters(Cluster[] clusters) {
        this.clusters = clusters;
    }

    public Point[] getCentroids(){
        Point[] centroid = new Point[numberOfClusters];

        for(int counter = 0;counter < centroid.length; counter++){
            centroid[counter] = clusters[counter].getCentroid();

        }
        return centroid;
    }

    public void clearClusters(){

        for(int counter = 0;counter < clusters.length; counter++){
            clusters[counter].clearCluster();
        }
    }


    public void assignCluster(){

        for(int counter=0;counter<points.length;counter++){

            int clusterId = 1;
            double tempDistance = 0.0;
            double[] distanceFromCentroids = new double[numberOfClusters];
            double shortestDistancetoCentroid = clusters[0].centroid.getDistance(points[counter]);
            distanceFromCentroids[0] = shortestDistancetoCentroid;


            for(int innerCounter = 1;innerCounter < numberOfClusters;innerCounter++){

                tempDistance = clusters[innerCounter].centroid.getDistance(points[counter]);
                distanceFromCentroids[innerCounter] = tempDistance;
                if (tempDistance < shortestDistancetoCentroid){
                    clusterId = innerCounter + 1;
                    shortestDistancetoCentroid = tempDistance;
                }
                clusters[clusterId-1].addPoint(points[counter]);
                points[counter].setClusterNumber(clusterId);

                for (int index = 0;index < distanceFromCentroids.length;index++){

                    if(shortestDistancetoCentroid == 0){
                         distanceFromCentroids[index] = 0;
                         if (index == clusterId - 1){
                             distanceFromCentroids[index] = 1;
                         }
                    }
                    else{
                        distanceFromCentroids[index] = shortestDistancetoCentroid/distanceFromCentroids[index];

                    }
                }
                points[counter].setConfidence(distanceFromCentroids);

            }

        }
    }

    public void calculateCentroids(){

        for(int counter = 0;counter<numberOfClusters;counter++){
            clusters[counter].calculateCentroid();
        }
    }


    public KMeans calculate() {
        boolean finish = false;
        int iteration = 0;

        // Add in new data, one at a time, recalculating centroids with each new one.
        while(!finish) {
            //Clear cluster state

            Point[] lastCentroids = getCentroids();

            clearClusters();

            //Assign points to the closer cluster
            assignCluster();

            //Calculate new centroids.
            calculateCentroids();

            iteration++;

            Point[] currentCentroids = getCentroids();

            //Calculates total distance between new and old Centroids
            double distance = 0;
            for(int index = 0;index< currentCentroids.length;index++){
                distance += currentCentroids[index].getDistance(lastCentroids[index]);
            }
            System.out.println("#################");
            System.out.println("Iteration: " + iteration);
            System.out.println("Centroid distances: " + distance);

            if(distance == 0 || iteration>MAX_ITERATIONS) {
                finish = true;
            }
        }

        return this;
    }
}
