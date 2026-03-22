package com.example.christophers.soundgeneration;

import android.util.Log;

import java.io.Serializable;

/**
 * Created by christopherS on 09.03.2017.
 */

public class Profile implements Serializable {  //contains the values of the sliders on the UI == progress
    public int balance;
    public int baseToneHeight;
    public boolean bullseyeMode;
    public int bullseyeSize;
    public int xOffset;
    public int yOffset;
    public boolean invertY;
    public boolean invertX;
    public String name;

    public Profile(int balance, int baseToneHeight, boolean bullseyeMode, int bullseyeSize, int xOffset, int yOffset, String name, boolean invertX, boolean invertY) {
        this.balance = balance;
        this.baseToneHeight = baseToneHeight;
        this.bullseyeMode = bullseyeMode;
        this.bullseyeSize = bullseyeSize;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.name = name;
        this.invertX = invertX;
        this.invertY = invertY;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Profile) {
            if (this.name.trim().equalsIgnoreCase(((Profile) obj).name.trim())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().trim().hashCode();
    }
}
