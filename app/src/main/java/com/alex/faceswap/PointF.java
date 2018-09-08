package com.alex.faceswap;

/**
 * Created by alexander on 2016-07-03.
 */
@SuppressWarnings("DefaultFileTemplate")
class PointF {
    /* X-coordinate position. */
    private final int x;
    /* Y-coordinate position. */
    private final int y;

    /**
     * Constructor, defines a new coordinate.
     * @param x sets the x-coordinate value.
     * @param y sets the y-coordinate value.
     */
    PointF(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the x-coordinate value.
     * @return x value
     */
    int X() {
        return x;
    }

    /**
     * Returns the y-coordinate value.
     * @return y value
     */
    int Y() {
        return y;
    }

}
