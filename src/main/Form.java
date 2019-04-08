package main;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Form extends JFrame implements Runnable {

    private final int w = 2600;
    private final int h = 900;

    private final Color BG = new Color(20, 55, 75, 255);
    private final Color LINK = new Color(255, 230, 0, 30);

    private final int NODE_RADIUS = 1;
    private final int NODE_COUNT = 20000;
    private final int MAX_DIST = 20;
    private final int MAX_DIST2 = MAX_DIST * MAX_DIST;
    private final double SPEED = 1;
    private final int SKIP_FRAMES = 1;
    private final int BORDER = 30;

    private final int fw = w / MAX_DIST + 1;
    private final int fh = h / MAX_DIST + 1;

    private final ArrayList<Link> links = new ArrayList<>();
    private final double LINK_FORCE = -0.225;
    private int frame = 0;

    private BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

    // array for dividing scene into parts to reduce complexity
    private final Field[][] fields = new Field[fw][fh];

    private static final double[][] COUPLING = {
            {1, 1, -1},
            {1, 1, 1},
            {1, 1, 1}
    };

    private static int[] LINKS = {
            1,
            3,
            2
    };

    private static final double[][] LINKS_POSSIBLE = {
            {0, 1, 1},
            {1, 2, 1},
            {1, 1, 2}
    };

    private static final Color[] COLORS = {
            new Color(250, 20, 20),
            new Color(200, 140, 100),
            new Color(80, 170, 140)
    };

    public Form() {
        for (int i = 0; i < fw; i++) {
            for (int j = 0; j < fh; j++) {
                fields[i][j] = new Field();
            }
        }
        // put particles randomly
        for (int i = 0; i < NODE_COUNT; i++) {
            add((int) (Math.random() * COUPLING.length), (Math.random() * w/2), (float) (Math.random() * h));
        }

        this.setSize(w + 16, h + 38);
        this.setVisible(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocation(50, 50);
        this.add(new JLabel(new ImageIcon(img)));
    }

    private Particle add(int type, double x, double y) {
        Particle p = new Particle(type, x, y);
        fields[(int) (p.x / MAX_DIST)][(int) (p.y / MAX_DIST)].particles.add(p);
        return p;
    }

    @Override
    public void run() {
        while (true) {
            this.repaint();
        }
    }

    @Override
    public void paint(Graphics g) {
        drawScene(img);
        for (int i = 0; i < SKIP_FRAMES; i++) logic();
        ((Graphics2D) g).drawImage(img, null, 8, 30);
        frame++;
    }

    private void drawScene(BufferedImage image) {
        Graphics2D g2 = image.createGraphics();
        g2.setColor(BG);
        g2.fillRect(0, 0, w, h);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < fw; i++) {
            for (int j = 0; j < fh; j++) {
                Field field = fields[i][j];
                for (Particle a : field.particles) {
//                    Particle a = field.particles.get(i1);
                    g2.setColor(COLORS[a.type]);
                    g2.fillOval((int) a.x - NODE_RADIUS, (int) a.y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
                    g2.setColor(LINK);
                    for (Particle b : a.bonds) {
                        g2.drawLine((int) a.x, (int) a.y, (int) b.x, (int) b.y);
                    }
                }
            }
        }
    }

    private void logic() {
        for (int i = 0; i < fw; i++) {
            for (int j = 0; j < fh; j++) {
                Field field = fields[i][j];
                for (Particle a : field.particles) {
                    a.x += a.sx;
                    a.y += a.sy;
                    a.sx *= 0.98f;
                    a.sy *= 0.98f;
                    // velocity normalization
                    // idk if it is still necessary
                    double magnitude = Math.sqrt(a.sx * a.sx + a.sy * a.sy);
                    if (magnitude > 1f) {
                        a.sx /= magnitude;
                        a.sy /= magnitude;
                    }
                    // border repulsion
                    if (a.x < BORDER) {
                        a.sx += SPEED * 0.05f;
                        if (a.x < 0) {
                            a.x = -a.x;
                            a.sx *= -0.5f;
                        }
                    } else if (a.x > w - BORDER) {
                        a.sx -= SPEED * 0.05f;
                        if (a.x > w) {
                            a.x = w * 2 - a.x;
                            a.sx *= -0.5f;
                        }
                    }
                    if (a.y < BORDER) {
                        a.sy += SPEED * 0.05f;
                        if (a.y < 0) {
                            a.y = -a.y;
                            a.sy *= -0.5f;
                        }
                    } else if (a.y > h - BORDER) {
                        a.sy -= SPEED * 0.05f;
                        if (a.y > h) {
                            a.y = h * 2 - a.y;
                            a.sy *= -0.5f;
                        }
                    }
                }
            }
        }
        ArrayList<Link> removeLinks = new ArrayList<>();
        for (Link link:links){
            Particle a = link.a;
            Particle b = link.b;
            double d2 = (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
            if (d2 > MAX_DIST2 / 4) {
                a.links--;
                b.links--;
                a.bonds.remove(b);
                b.bonds.remove(a);
                removeLinks.add(link);
            }
            else {
                if(d2 > NODE_RADIUS * NODE_RADIUS * 4) {
//                    double angle = Math.atan2(a.y - b.y, a.x - b.x);
                    Vector2D vector = new Vector2D(a.x - b.x, a.y - b.y).normalize();
                    a.sx += vector.getX() * LINK_FORCE * SPEED;
                    a.sy += vector.getY() * LINK_FORCE * SPEED;
                    b.sx -= vector.getX() * LINK_FORCE * SPEED;
                    b.sy -= vector.getY() * LINK_FORCE * SPEED;
                }
            }
        }
        for (Link link: removeLinks){
            links.remove(link);
        }
        ArrayList<Particle> remove = new ArrayList<>();
        // moving particle to another field
        for (int i = 0; i < fw; i++) {
            for (int j = 0; j < fh; j++) {
                Field field = fields[i][j];
                for (Particle a : field.particles) {
                    if (((int) (a.x / MAX_DIST) != i) || ((int) (a.y / MAX_DIST) != j)) {
//                        field.particles.remove(a);
                        remove.add(a);
                        fields[(int) (a.x / MAX_DIST)][(int) (a.y / MAX_DIST)].particles.add(a);
                    }
                }
                for (Particle a : remove) {
                    field.particles.remove(a);
                }
                remove.clear();
            }
        }
        // dividing scene into parts to reduce complexity
        for (int i = 0; i < fw; i++) {
            for (int j = 0; j < fh; j++) {
                Field field = fields[i][j];
                for (int i1 = 0; i1 < field.particles.size(); i1++) {
                    Particle a = field.particles.get(i1);
                    for (int j1 = i1 + 1; j1 < field.particles.size(); j1++) {
                        Particle b = field.particles.get(j1);
                        applyForce(a, b);
                    }
                    if (i < fw - 1) {
                        int iNext = i + 1;
                        Field field1 = fields[iNext][j];
                        for (Particle b: field1.particles) {
                            applyForce(a, b);
                        }
                    }
                    if (j < fh - 1) {
                        int jNext = j + 1;
                        Field field1 = fields[i][jNext];
                        for (Particle b: field1.particles){
                            applyForce(a, b);
                        }
                        if (i < fw - 1) {
                            int iNext = i + 1;
                            Field field2 = fields[iNext][jNext];
                            for (Particle b: field2.particles){
                                applyForce(a, b);
                            }
                        }
                    }
                }
            }
        }
    }

    private void applyForce(Particle a, Particle b) {
        double d2 = (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
        if (d2 < MAX_DIST2) {
            double dA = COUPLING[a.type][b.type] / d2;
            double dB = COUPLING[b.type][a.type] / d2;
            if (a.links < LINKS[a.type] && b.links < LINKS[b.type]) {
                if (d2 < MAX_DIST2 / 4) {
                    if (!a.bonds.contains(b) && !b.bonds.contains(a)) {
                        int typeCountA = 0;
                        typeCountA = (int) a.bonds.stream().filter((Particle p) -> p.type == b.type).count();
//                        for (Particle p : a.bonds) {
//                            if (p.type == b.type) typeCountA++;
//                        }
                        int typeCountB = 0;
                        typeCountB = (int) b.bonds.stream().filter((Particle p) -> p.type == a.type).count();
//                        for (Particle p : b.bonds) {
//                            if (p.type == a.type) typeCountB++;
//                        }
                        // TODO: particles should connect to closest neighbors not to just first in a list
                        if (typeCountA < LINKS_POSSIBLE[a.type][b.type] && typeCountB < LINKS_POSSIBLE[b.type][a.type]) {
                            a.bonds.add(b);
                            b.bonds.add(a);
                            a.links++;
                            b.links++;
                            links.add(new Link(a, b));
                        }
                    }
                }
            } else {
                if (!a.bonds.contains(b) && !b.bonds.contains(a)) {
                    dA = 1 / d2;
                    dB = 1 / d2;
                }
            }
//            double angle = Math.atan2(a.y - b.y, a.x - b.x);
            if(d2 < 1) d2 = 1;
            if(d2 < NODE_RADIUS * NODE_RADIUS * 4) {
                dA = 1 / d2;
                dB = 1 / d2;
            }

            Vector2D vector = new Vector2D(a.x - b.x, a.y - b.y).normalize();
            a.sx += vector.getX() * dA * SPEED;
            a.sy += vector.getY() * dA * SPEED;
            b.sx -= vector.getX() * dB * SPEED;
            b.sy -= vector.getY() * dB * SPEED;
//            a.sx += Math.cos(angle) * dA * SPEED;
//            a.sy += Math.sin(angle) * dA * SPEED;
//            b.sx -= Math.cos(angle) * dB * SPEED;
//            b.sy -= Math.sin(angle) * dB * SPEED;
        }
    }
}
