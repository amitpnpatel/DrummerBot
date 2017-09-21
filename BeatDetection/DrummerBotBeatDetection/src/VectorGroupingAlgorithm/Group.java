package VectorGroupingAlgorithm;
import java.util.ArrayList;

/**
 * Created by rajatvaryani on 3/8/17.
 */

public class Group {


    public ArrayList<Vector> vectors = new ArrayList<>();
    public Vector centroid;
    public int id;




    public Group(int id) {
        this.id = id;
    }

    public Group addVectorAndUpdateGroup(Vector Vector){
        vectors.add(Vector);
        this.calculateCentroid();
        return this;
    }

    public ArrayList<Vector> getVectors() {
        return vectors;
    }

    public void setVectors(ArrayList<Vector> vectors) {
        this.vectors = vectors;
    }

    public Vector getCentroid() {
        return centroid;

    }

    public void setCentroid(Vector centroid) {
        this.centroid = centroid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }



    public Vector calculateCentroid() {
        if (vectors.size() == 0) {
            return null;
        }

        Vector newCentroid = new Vector(new double[vectors.get(0).getVector().length]);
        int groupSize = vectors.size();

        for(int counter = 0; counter< newCentroid.getVector().length;counter++){

            for (int vectorCounter = 0;vectorCounter<groupSize;vectorCounter++){
                newCentroid.getVector()[counter] +=vectors.get(vectorCounter).getVector()[counter];
            }

        }

        newCentroid.getUnitVector();
        this.centroid=newCentroid;
        return centroid;
    }

    public void clearCluster(){
        this.vectors = new ArrayList<>();
    }
}
