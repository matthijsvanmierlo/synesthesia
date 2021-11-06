package com.example.mtvan15.ui_sye;

import android.graphics.Point;

import java.util.ArrayList;

public class Radial {
    // Keep track of the coordinates you want to draw at
    private double angleStep;
    private double currAngle1;
    private double currAngle2;
    private double currAngle3;
    private double currAngle4;
    private int dimX;
    private int dimY;
    private int maxRadius;
    private int radius;
    private int radiusStep;

    public Radial(int dim1, int dim2){
        // Keep track of the width and height of the window
        dimX = dim1;
        dimY = dim2;

        // Keep track of the minimum dimension so that it will always draw on the screen
        if(dimX < dimY) {
            maxRadius = dimX / 2;
        }else{
            maxRadius = dimY / 2;
        }

        currAngle1 = 0.0;
        currAngle2 = Math.PI/2;
        currAngle3 = Math.PI;
        currAngle4 = 3*Math.PI/2;

        angleStep = Math.PI / 64;

        radiusStep = 1;
        radius = 1;

    }

    public int[] next(){
        // Check to see if the maximum radius has been exceeded
        if(radius + radiusStep >= maxRadius){
            if(radiusStep > 0){
                radiusStep = -1;
            }
        }else if (radius + radiusStep <= 1){
            if(radiusStep < 0){
                radiusStep = 1;
            }
        }
        // Update the angle you want to use in this situation
        radius += radiusStep;
        currAngle1 += angleStep;
        currAngle2 += angleStep;
        currAngle3 += angleStep;
        currAngle4 += angleStep;


//        // Check to see if the maximum angle has been exceeded
//        if(angleStep > 0 && currAngle + angleStep >= 2 * Math.PI){
//            angleStep = -angleStep;
//        }else if(radiusStep < 0 && currAngle + angleStep <= 0){
//            angleStep = -angleStep;
//        }

        Point p1 = new Point((int) ((dimX/2) + radius * Math.cos(currAngle1)), (int) ((dimY/2) + radius * Math.sin(currAngle1)));
        Point p2 = new Point((int) ((dimX/2) + radius * Math.cos(currAngle2)), (int) ((dimY/2) + radius * Math.sin(currAngle2)));
        Point p3 = new Point((int) ((dimX/2) + radius * Math.cos(currAngle3)), (int) ((dimY/2) + radius * Math.sin(currAngle3)));
        Point p4 = new Point((int) ((dimX/2) + radius * Math.cos(currAngle4)), (int) ((dimY/2) + radius * Math.sin(currAngle4)));

        return new int[]{p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y};

    }
}
