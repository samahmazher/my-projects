package FaceDetector;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.LinkedList;
import java.util.Queue;
import javax.imageio.ImageIO;
import javax.swing.*;

public class FaceDetector {

    public JFrame frame;
    BufferedImage img;
    MyJpanel loadimage;

    public int WIDTH = 800;
    public int HEIGHT = 600;

    BufferedImage convertToGray(BufferedImage image) {
        BufferedImage temp = new BufferedImage(image.getWidth(),
                image.getHeight(), BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {

                Color c = new Color(image.getRGB(x, y));
                int red = c.getRed();
                int green = c.getGreen();
                int blue = c.getBlue();
                int average = (red + green + blue) / 3;
                c = new Color(average, average, average);
                temp.setRGB(x, y, c.getRGB());
            }
        }
        return temp;
    }

    void startTraining(RandomForest rf) {
        //for () {
        File face = new File("E:\\New folder\\our dataset");
        File nonface = new File("E:\\New folder\\nonfaces");

        Queue<File> facesQueue = new LinkedList<>();
        Queue<File> nonfacesQueue = new LinkedList<>();

        facesQueue.add(face);
        nonfacesQueue.add(nonface);

        while (!facesQueue.isEmpty() || !nonfacesQueue.isEmpty()) {
            if (!facesQueue.isEmpty()) {
                face = facesQueue.remove();
                for (File f : face.listFiles()) {
                    if (f.isFile()) {
                        try {
                            BufferedImage img = ImageIO.read(f);
                            System.err.println("training started & it is face image " + f.getPath());
                            rf.trainForest(img, true);
                            rf.safeForest(new File("test.rff"));
                        } catch (IOException ex) {
                            Logger.getLogger(FaceDetection.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else if (f.isDirectory()) {
                        facesQueue.add(f);
                    }
                }
            }
            if (!nonfacesQueue.isEmpty()) {
                nonface = nonfacesQueue.remove();
                for (File nf : nonface.listFiles()) {
                    if (nf.isFile()) {
                        try {
                            BufferedImage img = ImageIO.read(nf);
                            System.err.println("training started & it is nonface image " + nf.getPath());
                            rf.trainForest(img, false);
                            rf.safeForest(new File("test.rff"));
                        } catch (IOException ex) {
                            Logger.getLogger(FaceDetection.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else if (nf.isDirectory()) {
                        nonfacesQueue.add(nf);
                    }
                }
            }
        }
        //}
    }

    public static void main(String[] a) {
        FaceDetector fd = new FaceDetector();
        SlidingWindow sw;
        BufferedImage image;
        // TODO code application logic here
        RandomForest rf = new RandomForest(4, 7, 10);
        rf.buildForest();
        rf.loadForest(new File("test.rff"));
        fd.startTraining(rf);
        try {
            image = ImageIO.read(new File("E:\\New folder\\Dataset\\Training\\DSC00218.jpg"));
            image = fd.convertToGray(image);
            sw = new SlidingWindow(0, 0, 150, 300, image.getWidth(), image.getHeight());
            fd.loadimage = new MyJpanel(image);
            fd.loadimage.setBounds(0, 0, 700, 400);
            fd.frame = new JFrame();
            fd.frame.setBounds(0, 0, 700, 400);

            fd.frame.add(fd.loadimage);
            fd.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            fd.frame.setLayout(new BorderLayout());
            fd.frame.setVisible(true);
            while (sw.slidable) {
            sw.slid(5);
            BufferedImage sub = image.getSubimage(sw.windowx, sw.windowy, sw.width, sw.hight);
            boolean result = rf.detectFace(image);
            System.err.println(sw.windowx + " " + sw.windowy);
            if (result) {
            fd.loadimage.draw(sw.windowx, sw.windowy, sw.width, sw.hight);
            System.err.println(result);
            }
            }
        } catch (IOException ex) {
            Logger.getLogger(FaceDetector.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}

class SlidingWindow {

    int windowx;
    int windowy;
    int width;
    int hight;
    int containerwidth;
    int containerhight;
    boolean slidable = true;

    public SlidingWindow(int windowx, int windowy, int width, int hight, int containerwidth, int containerhight) {
        this.windowx = windowx;
        this.windowy = windowy;
        this.width = width;
        this.hight = hight;
        this.containerwidth = containerwidth;
        this.containerhight = containerhight;
    }

    void slid(int step) {
        if (windowx + width + step < containerwidth) {
            windowx += step;

        } else if (windowy + hight + step < containerhight) {

            windowx = 0;
            windowy += step;
        } else {
            slidable = false;
        }

    }

    void rest() {
        windowx = 0;
        windowy = 0;
        slidable = true;
    }
}

class MyJpanel extends JPanel {

    BufferedImage image;

    public MyJpanel(BufferedImage image) {
        this.image = image;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); //To change body of generated methods, choose Tools | Templates.
        g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
    }

    void draw(int x, int y, int w, int h) {
        int scale = (image.getHeight() + image.getWidth()) / (getWidth() + getHeight());
        Graphics2D g = image.createGraphics();
        g.setColor(Color.red);
        g.setStroke(new BasicStroke(scale));
        g.drawRect(x, y, w, h);
        repaint();
    }
    void setImage(BufferedImage img){
        this.image=img;
        repaint();
    }

}
