/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FaceDetector;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author MyMain
 */
public class FaceDetection {

    /**
     * @param args the command line arguments
     */
}

class FeatureNode {
    //the node features

    int LBPVal;
    int x, y;
    //the node childs in the tree
    List<FeatureNode> childs = new LinkedList<>();
    //mother node
    FeatureNode mother;
    int depth;
    int power;

    FeatureNode() {
        this.depth = 0;
        this.power = 0;
    }

    void setFeatures(int LBPVal, int xShift, int yShift) {
        this.LBPVal = LBPVal;
        this.x = xShift;
        this.y = yShift;
    }

    void giveRandomFeatures() {
        //in this fuction we will creat random values "features" to
        //be given to the node this will be used to build the initial tree
        //and modify the tree node randomly
        int LBPval = (int) (Math.random() * 256); //LBP rang from 0 - 256
        int rx = (int) (Math.random() * 100); //image width
        int ry = (int) (Math.random() * 100); //image height
        setFeatures(LBPval, rx, ry);
    }

}

class decisionTree {

    List<FeatureNode> treeNodes = new LinkedList<>();
    FeatureNode motherFeatureNode;
    int branchesCount;
    int treeDepth;

    public decisionTree(int branchesCount, int treeDepth) {
        this.branchesCount = branchesCount;
        this.treeDepth = treeDepth;
    }

    void buildTree() {
        System.err.println("building tree");
        motherFeatureNode = new FeatureNode();
        motherFeatureNode.giveRandomFeatures();
        motherFeatureNode.depth = 0;
        treeNodes.add(motherFeatureNode);
        Queue<FeatureNode> buildQueue = new LinkedList<>();
        buildQueue.add(motherFeatureNode);
        while (!buildQueue.isEmpty()) {
            FeatureNode current = buildQueue.remove();
            //System.err.println(current.depth);
            for (int i = 0; i < branchesCount; i++) {
                FeatureNode n = new FeatureNode();
                n.mother = current;
                n.depth = current.depth + 1;
                n.giveRandomFeatures();
                treeNodes.add(n);
                current.childs.add(n);
                if (n.depth < treeDepth) {
                    buildQueue.add(n);
                }
            }
        }
        System.err.println("building tree done ####################################################################");
    }
}

class RandomForest {

    List<decisionTree> forestTrees = new LinkedList<>();
    int branchesCount, treeDepth;
    int treeCount;
    int successCounter = 1, failCounter = 1;
    int lowestPower = 1, highestPower = 0;
    double scaleFactorX, scaleFactorY;

    public RandomForest(int branchesCount, int treeDepth, int treeCount) {
        this.branchesCount = branchesCount;
        this.treeDepth = treeDepth;
        this.treeCount = treeCount;
    }

    void buildForest() {
        System.err.println("building forest");
        for (int i = 0; i < treeCount; i++) {
            decisionTree dt = new decisionTree(branchesCount, treeDepth);
            dt.buildTree();
            forestTrees.add(dt);
        }
        System.err.println("done building forest #############################################################################");

    }

    void trainForest(BufferedImage img, boolean isFace) {
        this.scaleFactorX = img.getWidth() / 100;;
        this.scaleFactorY = img.getHeight() / 100;
        List<FeatureNode> searchSpace = new LinkedList<>();
        boolean succeeded = false;
        for (decisionTree dt : forestTrees) {
            searchSpace.add(dt.motherFeatureNode);
        }
        while (!succeeded) {
            //iterate over search space to find correct node
            //if didn't find it modify the least powefull node
            //if found correct one change search space to next level nodes

            FeatureNode correctnode = null;
            for (int i = 0; i < searchSpace.size(); i++) {
                FeatureNode n = searchSpace.get(i);
                if (correctFeature(n, img)) {
                    correctnode = n;
                    /*System.err.println("correct  node at depth " + n.depth
                     //       + " and power is " + n.power);*/
                    break;
                }
            }
            if (correctnode != null) {
                //not an end node "leaf"
                if (correctnode.depth < treeDepth) {
                    //continue in child nodes
                    searchSpace.clear();
                    searchSpace.addAll(correctnode.childs);
                } else {
                    if (isFace == true) {
                        //search succeeded
                        succeeded = true;
                        upwardPowerUp(correctnode);
                    } else {
                        //error result
                        upwardPowerDown(correctnode);
                        //this because this is the node that cause the error
                        correctnode.giveRandomFeatures();
                    }
                }
            } else {
                //no node foud in search space
                //either the image not a face one
                //so tree successfully excluded it
                if (isFace == false) {
                    //search succeeded
                    succeeded = true;
                    successCounter++;
                    System.err.println("successCounter " + successCounter + " failCounter " + failCounter);
                } else {
                    //premature classification stop
                    //modify the least powefull node of search space randomly
                    //and search again
                    failCounter++;
                    System.err.println("successCounter " + successCounter + " failCounter " + failCounter);
                    //because no correct node in search space we modify one of them
                    modifyRandomly(searchSpace);
                }

            }
        }
    }

    boolean detectFace(BufferedImage img) {
        this.scaleFactorX = img.getWidth() / 100;;
        this.scaleFactorY = img.getHeight() / 100;
        List<FeatureNode> searchSpace = new LinkedList<>();
        boolean result = false;
        for (decisionTree dt : forestTrees) {
            searchSpace.add(dt.motherFeatureNode);
        }
        //iterate over search space to find correct node
        //if didn't find it mean not face
        //if found correct one change search space to next level nodes
        //till end node reached so its face
        FeatureNode correctnode = null;
        for (int i = 0; i < searchSpace.size(); i++) {
            FeatureNode n = searchSpace.get(i);
            if (correctFeature(n, img)) {
                correctnode = n;
                /*System.err.println("correct  node at depth " + n.depth
                 //       + " and power is " + n.power);*/
                break;
            }
        }
        if (correctnode != null) {
            //not an end node "leaf"
            if (correctnode.depth < treeDepth) {
                //continue in child nodes
                searchSpace.clear();
                searchSpace.addAll(correctnode.childs);
            } else {
                result = true;
            }
        } else {
            result = false;
        }
        return result;
    }

    boolean correctFeature(FeatureNode n, BufferedImage img) {
        int x = (int) (n.x * scaleFactorX);
        int y = (int) (n.y * scaleFactorY);
        //to avoid out of index exception
        if (x <= 0) {
            x = 1;
        } else if (x >= img.getWidth() - 1) {
            x = img.getWidth() - 2;
        }
        if (y <= 0) {
            y = 1;
        } else if (y >= img.getHeight() - 1) {
            y = img.getHeight() - 2;
        }
        int centerPixel = img.getRGB(x, y);
        int[] neighbourPixels = new int[8];
        neighbourPixels[0] = img.getRGB(x - 1, y);
        neighbourPixels[1] = img.getRGB(x - 1, y - 1);
        neighbourPixels[2] = img.getRGB(x, y - 1);
        neighbourPixels[3] = img.getRGB(x + 1, y - 1);
        neighbourPixels[4] = img.getRGB(x + 1, y);
        neighbourPixels[5] = img.getRGB(x + 1, y + 1);
        neighbourPixels[6] = img.getRGB(x, y + 1);
        neighbourPixels[7] = img.getRGB(x - 1, y + 1);
        return n.LBPVal == LBP(centerPixel, neighbourPixels);

    }

    int LBP(int centerPixel, int[] neighbours) {
        int result = 0;
        for (int i = 0; i < 8; i++) {
            if (centerPixel >= neighbours[i]) {
                result += Math.pow(2, i);
            }
        }
        return result;
    }

    void modifyRandomly(List<FeatureNode> nl) {
        FeatureNode leastPowernode = nl.get(0);
        for (FeatureNode n : nl) {
            if (leastPowernode.power > n.power) {
                leastPowernode = n;
                lowestPower = leastPowernode.power;
            }
            if (highestPower < n.power) {
                highestPower = n.power;
            }
        }
        System.err.println("modifying forest : fail counter " + failCounter
                + " success counter is " + successCounter
                + " lowest power " + lowestPower + " highest power " + highestPower);
        leastPowernode.giveRandomFeatures();
    }

    //increase power of nodes from current node upwaed
    void upwardPowerUp(FeatureNode n) {
        successCounter++;
        System.err.println("successCounter " + successCounter + " failCounter " + failCounter);

        while (n.mother != null) {
            n.power++;
            n = n.mother;
        }
    }

    //increase power of nodes from current node upwaed
    void upwardPowerDown(FeatureNode n) {
        failCounter++;
        System.err.println("successCounter " + successCounter + " failCounter " + failCounter);
        while (n.mother != null) {
            n.power = n.power > 0 ? n.power-- : 0;
            n = n.mother;
        }
    }

    void safeForest(File f) {
        Charset charset = Charset.forName("UTF-8");
        try (BufferedWriter writer = Files.newBufferedWriter(f.toPath(), charset)) {
            String s = "random forest file format (rff) "
                    + "first line is branches count , tree depth , tree count "
                    + "comma separatted followed by lines representing "
                    + "node lbp ,x,y,power,depth in DFS order \n";
            writer.write(s);
            s = branchesCount + "," + treeDepth + "," + treeCount + "\n";
            writer.write(s);
            //iterate over nodes by DFS
            Queue<FeatureNode> nodesQueue = new LinkedList<>();
            //prime the queue
            for (decisionTree dt : forestTrees) {
                nodesQueue.add(dt.motherFeatureNode);
            }
            while (!nodesQueue.isEmpty()) {
                FeatureNode current = nodesQueue.remove();
                s = current.LBPVal + "," + current.x + "," + current.y + ","
                        + current.power + "," + current.depth + "\n";
                writer.write(s);
                for (FeatureNode n : current.childs) {
                    nodesQueue.add(n);
                }
            }
            System.err.println("saving done");
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }

    }

    void loadForest(File f) {
        Charset charset = Charset.forName("UTF-8");
        try (BufferedReader reader = Files.newBufferedReader(f.toPath(), charset)) {
            String s = null;
            s = reader.readLine();//read description
            System.err.println(s);
            s = reader.readLine();//read forest properties
            System.err.println(s);

            //iterate over nodes by DFS
            Queue<FeatureNode> nodesQueue = new LinkedList<>();
            //prime the queue
            for (decisionTree dt : forestTrees) {
                nodesQueue.add(dt.motherFeatureNode);
            }
            while (!nodesQueue.isEmpty()) {
                FeatureNode current = nodesQueue.remove();
                s = reader.readLine();
                if (s != null) {
                    String[] ss = s.split(",");
                    current.LBPVal = Integer.valueOf(ss[0]);
                    current.x = Integer.valueOf(ss[1]);
                    current.y = Integer.valueOf(ss[2]);
                    current.power = Integer.valueOf(ss[3]);
                    current.depth = Integer.valueOf(ss[4]);
                    /*System.err.println(current.LBPVal + "," + current.x + "," + current.y + ","
                     + current.power + "," + current.depth + "\n");*/
                }
                for (FeatureNode n : current.childs) {
                    nodesQueue.add(n);
                }
            }
            System.err.println("loading done");
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }

    }
}
