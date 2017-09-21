package VectorGroupingAlgorithm;

import java.util.ArrayList;

/**
 * Created by rajatvaryani on 3/8/17.
 */
public class ThresholdAlgorithm {

    private ArrayList<Group> groupList;
    private double THRESHOLD = 0.97;
    private Vector[] vectors;



    public ThresholdAlgorithm(Vector[] vectors) {
        this.groupList = new ArrayList<Group>();
        this.vectors = vectors;

    }

    public ArrayList<Group> getGroupList(){
        return groupList;
    }


    public void assignGroup() {
        for (int vectorCount = 0; vectorCount < vectors.length; vectorCount++) {
            boolean isAssigned = false;
            for (int groupCount = 0; groupCount < groupList.size(); groupCount++) {
                double angle = groupList.get(groupCount).getCentroid().getAngle(vectors[vectorCount]);
               System.out.println("");
                if (groupList.get(groupCount).getCentroid().getAngle(vectors[vectorCount]) > THRESHOLD) {

                    groupList.get(groupCount).addVectorAndUpdateGroup(vectors[vectorCount]);
                    vectors[vectorCount].setGroupNumber(groupCount);
                    isAssigned = true;
                    break;
                }

            }
            if (!isAssigned) {
                groupList.add(new Group(groupList.size()).addVectorAndUpdateGroup(vectors[vectorCount]));
                vectors[vectorCount].setGroupNumber(groupList.size()-1);
            }

        }
    }





}
