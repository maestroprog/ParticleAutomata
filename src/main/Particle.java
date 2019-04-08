package main;

import java.util.HashSet;
import java.util.Set;

public class Particle {

    public int type;
    public double x;
    public double y;
    public double sx;
    public double sy;
    public int links;
    public Set<Particle> bonds;

    public Particle(int type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.sx = 0;
        this.sy = 0;
        this.links = 0;
        this.bonds = new HashSet<>();
    }

}