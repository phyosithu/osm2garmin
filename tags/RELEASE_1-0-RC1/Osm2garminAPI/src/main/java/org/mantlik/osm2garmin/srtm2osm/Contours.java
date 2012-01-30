/*
 * #%L
 * Osm2garminAPI
 * %%
 * Copyright (C) 2011 Frantisek Mantlik <frantisek at mantlik.cz>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package org.mantlik.osm2garmin.srtm2osm;

//----------------------------------------------------------

import java.util.ArrayList;

//----------------------------------------------------------
public class Contours {

    // Below, constant data members:
    final static boolean SHOW_NUMBERS = true;
    static double Z_MAX_MAX = 1.0E+10,
            Z_MIN_MIN = -1.0E+10;
    // Below, data members which store the grid steps,
    // the z values, the interpolation flag, the dimensions
    // of the contour plot and the increments in the grid:
    int xSteps, ySteps;
    float z[][];
    // boolean logInterpolation = false; // not implemented
    double xmin, ymin, deltaX, deltaY;
    ArrayList<Contour> contours;
    // Below, data members, most of which are adapted from
    // Fortran variables in Snyder's code:
    int ncv;
    double cv[] = new double[2];
    double contourStep;
    int l1[] = new int[4];
    int l2[] = new int[4];
    int ij[] = new int[2];
    int i1[] = new int[2];
    int i2[] = new int[2];
    int i3[] = new int[6];
    int ibkey, icur, jcur, ii, jj, elle, ix, iedge, iflag, ni, ks;
    int cntrIndex, prevIndex;
    int idir, nxidir, k;
    double z1, z2, cval, zMax, zMin;
    double intersect[] = new double[4];
    double xy[] = new double[2];
    double prevXY[] = new double[2];
    boolean jump;

    //-------------------------------------------------------
    // A constructor method.
    //-------------------------------------------------------
    public Contours(float[][] z, int nx, int ny, double xmin, double ymin, double step,
            double contourStep, double nanValue) {
        this.z = z;
        xSteps = nx;
        ySteps = ny;
        this.xmin = xmin;
        this.ymin = ymin;
        deltaX = step;
        deltaY = step;
        this.contourStep = contourStep;
        Z_MAX_MAX = nanValue-1;
        Z_MIN_MIN = -Z_MAX_MAX;
    }

    public ArrayList<Contour> makeContours() throws Exception {
        assignContourValues();
        int workLength = 2 * xSteps * ySteps * ncv;
        boolean workSpace[]; // Allocate below if data valid
        contours = new ArrayList<Contour>();
        /*try {*/
        if (cv[0] != cv[1]) { // Valid data
            workSpace = new boolean[workLength];
            ContourPlotKernel(workSpace);
        }
        /*} catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }*/
        return contours;
    }

    //-------------------------------------------------------
    int sign(int a, int b) {
        a = Math.abs(a);
        if (b < 0) {
            return -a;
        } else {
            return a;
        }
    }

    //-------------------------------------------------------
    // "InvalidData" sets the first two components of the
    // contour value array to equal values, thus preventing
    // subsequent drawing of the contour plot.
    //-------------------------------------------------------
    void InvalidData() {
        cv[0] = (float) 0.0;
        cv[1] = (float) 0.0;
    }

    //-------------------------------------------------------
    // "GetExtremes" scans the data in "z" in order
    // to assign values to "zMin" and "zMax".
    //-------------------------------------------------------
    void getExtremes() {
        int i, j;
        double here;
        boolean dataSet = false;
        for (i = 0; i < xSteps; i++) {
            for (j = 0; j < ySteps; j++) {
                here = z[i][j];
                if (here < Z_MAX_MAX) {
                    if (dataSet) {
                    if (zMin > here) {
                        zMin = here;
                    }
                    if (zMax < here) {
                        zMax = here;
                    }
                    } else {
                        zMin = here;
                        zMax = here;
                        dataSet = true;
                    }
                }
            }
        }
        if (zMin == zMax) {
            InvalidData();
        }
        return;
    }

    //-------------------------------------------------------
    // "AssignContourValues" interpolates between "zMin" and
    // "zMax", either logarithmically or linearly, in order
    // to assign contour values to the array "cv".
    //-------------------------------------------------------
    void assignContourValues() {
        if (contourStep <= 0d) {
            InvalidData();
            return;
        }
        int i;
        getExtremes();
        /*if ((logInterpolation) && (zMin <= 0.0)) {
        InvalidData();
        }
        if (logInterpolation) {
        double temp = Math.log(zMin);

        delta = (Math.log(zMax) - temp) / ncv;
        for (i = 0; i < ncv; i++) {
        cv[i] = (float) Math.exp(temp + (i + 1) * delta);
        }
        } else {*/
        double firstContour = Math.floor(zMin / contourStep) * contourStep + contourStep;
        ncv = (int) Math.floor((zMax - firstContour) / contourStep) + 1;
        if (ncv <= 1) {
            InvalidData();
            return;
        }
        cv = new double[ncv];
        for (i = 0; i < ncv; i++) {
            cv[i] = firstContour + i * contourStep;
        }
        //}
    }

    //-------------------------------------------------------
    // "DrawKernel" is the guts of drawing and is called
    // directly or indirectly by "ContourPlotKernel" in order
    // to draw a segment of a contour or to set the pen
    // position "prevXY". Its action depends on "iflag":
    //
    // iflag == 1 means Continue a contour
    // iflag == 2 means Start a contour at a boundary
    // iflag == 3 means Start a contour not at a boundary
    // iflag == 4 means Finish contour at a boundary
    // iflag == 5 means Finish closed contour not at boundary
    // iflag == 6 means Set pen position
    //
    // If the constant "SHOW_NUMBERS" is true then when
    // completing a contour ("iflag" == 4 or 5) the contour
    // index is drawn adjacent to where the contour ends.
    //-------------------------------------------------------
    void DrawKernel() {
        double prevU, prevV, u, v;

        if ((iflag == 1) || (iflag == 4) || (iflag == 5)) {
            if (cntrIndex != prevIndex) { // Must change colour
                //newContour();
                prevIndex = cntrIndex;
            }
            prevU = (prevXY[0] - 1.0) * deltaX + xmin;
            prevV = (prevXY[1] - 1.0) * deltaY + ymin;
            u = (xy[0] - 1.0) * deltaX + xmin;
            v = (xy[1] - 1.0) * deltaY + ymin;

            if (z1 < Z_MAX_MAX && z2 < Z_MAX_MAX) {
                contourSegment(prevU, prevV,
                        u, v, cv[cntrIndex]);
            }
        }
        prevXY[0] = xy[0];
        prevXY[1] = xy[1];
    }

    //-------------------------------------------------------
    // "DetectBoundary"
    //-------------------------------------------------------
    void DetectBoundary() {
        ix = 1;
        if (ij[1 - elle] != 1) {
            ii = ij[0] - i1[1 - elle];
            jj = ij[1] - i1[elle];
            if (z[ii - 1][jj - 1] <= Z_MAX_MAX) {
                ii = ij[0] + i2[elle];
                jj = ij[1] + i2[1 - elle];
                if (z[ii - 1][jj - 1] < Z_MAX_MAX) {
                    ix = 0;
                }
            }
            if (ij[1 - elle] >= l1[1 - elle]) {
                ix = ix + 2;
                return;
            }
        }
        ii = ij[0] + i1[1 - elle];
        jj = ij[1] + i1[elle];
        if (z[ii - 1][jj - 1] > Z_MAX_MAX) {
            ix = ix + 2;
            return;
        }
        if (z[ij[0]][ij[1]] >= Z_MAX_MAX) {
            ix = ix + 2;
        }
    }

    //-------------------------------------------------------
    // "Routine_label_020" corresponds to a block of code
    // starting at label 20 in Synder's subroutine "GCONTR".
    //-------------------------------------------------------
    boolean Routine_label_020() {
        l2[0] = ij[0];
        l2[1] = ij[1];
        l2[2] = -ij[0];
        l2[3] = -ij[1];
        idir = 0;
        nxidir = 1;
        k = 1;
        ij[0] = Math.abs(ij[0]);
        ij[1] = Math.abs(ij[1]);
        if (z[ij[0] - 1][ij[1] - 1] > Z_MAX_MAX) {
            elle = idir % 2;
            ij[elle] = sign(ij[elle], l1[k - 1]);
            return true;
        }
        elle = 0;
        return false;
    }

    //-------------------------------------------------------
    // "Routine_label_050" corresponds to a block of code
    // starting at label 50 in Synder's subroutine "GCONTR".
    //-------------------------------------------------------
    boolean Routine_label_050() {
        while (true) {
            if (ij[elle] >= l1[elle]) {
                if (++elle <= 1) {
                    continue;
                }
                elle = idir % 2;
                ij[elle] = sign(ij[elle], l1[k - 1]);
                if (Routine_label_150()) {
                    return true;
                }
                continue;
            }
            ii = ij[0] + i1[elle];
            jj = ij[1] + i1[1 - elle];
            if (z[ii - 1][jj - 1] > Z_MAX_MAX) {
                if (++elle <= 1) {
                    continue;
                }
                elle = idir % 2;
                ij[elle] = sign(ij[elle], l1[k - 1]);
                if (Routine_label_150()) {
                    return true;
                }
                continue;
            }
            break;
        }
        jump = false;
        return false;
    }

    //-------------------------------------------------------
    // "Routine_label_150" corresponds to a block of code
    // starting at label 150 in Synder's subroutine "GCONTR".
    //-------------------------------------------------------
    boolean Routine_label_150() {
        while (true) {
            //------------------------------------------------
            // Lines from z[ij[0]-1][ij[1]-1]
            //	   to z[ij[0]  ][ij[1]-1]
            //	  and z[ij[0]-1][ij[1]]
            // are not satisfactory. Continue the spiral.
            //------------------------------------------------
            if (ij[elle] < l1[k - 1]) {
                ij[elle]++;
                if (ij[elle] > l2[k - 1]) {
                    l2[k - 1] = ij[elle];
                    idir = nxidir;
                    nxidir = idir + 1;
                    k = nxidir;
                    if (nxidir > 3) {
                        nxidir = 0;
                    }
                }
                ij[0] = Math.abs(ij[0]);
                ij[1] = Math.abs(ij[1]);
                if (z[ij[0] - 1][ij[1] - 1] > Z_MAX_MAX) {
                    elle = idir % 2;
                    ij[elle] = sign(ij[elle], l1[k - 1]);
                    continue;
                }
                elle = 0;
                return false;
            }
            if (idir != nxidir) {
                nxidir++;
                ij[elle] = l1[k - 1];
                k = nxidir;
                elle = 1 - elle;
                ij[elle] = l2[k - 1];
                if (nxidir > 3) {
                    nxidir = 0;
                }
                continue;
            }

            if (ibkey != 0) {
                return true;
            }
            ibkey = 1;
            ij[0] = icur;
            ij[1] = jcur;
            if (Routine_label_020()) {
                continue;
            }
            return false;
        }
    }

    //-------------------------------------------------------
    // "Routine_label_200" corresponds to a block of code
    // starting at label 200 in Synder's subroutine "GCONTR".
    // It has return values 0, 1 or 2.
    //-------------------------------------------------------
    short Routine_label_200(boolean workSpace[]) {
        while (true) {
            xy[elle] = 1.0 * ij[elle] + intersect[iedge - 1];
            xy[1 - elle] = 1.0 * ij[1 - elle];
            workSpace[2 * (xSteps * (ySteps * cntrIndex + ij[1] - 1)
                    + ij[0] - 1) + elle] = true;
            DrawKernel();
            if (iflag >= 4) {
                icur = ij[0];
                jcur = ij[1];
                return 1;
            }
            ContinueContour();
            if (!workSpace[2 * (xSteps * (ySteps * cntrIndex
                    + ij[1] - 1) + ij[0] - 1) + elle]) {
                return 2;
            }
            iflag = 5;		// 5. Finish a closed contour
            iedge = ks + 2;
            if (iedge > 4) {
                iedge = iedge - 4;
            }
            intersect[iedge - 1] = intersect[ks - 1];
        }
    }

    //-------------------------------------------------------
    // "CrossedByContour" is true iff the current segment in
    // the grid is crossed by one of the contour values and
    // has not already been processed for that value.
    //-------------------------------------------------------
    boolean CrossedByContour(boolean workSpace[]) {
        ii = ij[0] + i1[elle];
        jj = ij[1] + i1[1 - elle];
        z1 = z[ij[0] - 1][ij[1] - 1];
        z2 = z[ii - 1][jj - 1];
        for (cntrIndex = 0; cntrIndex < ncv; cntrIndex++) {
            int i = 2 * (xSteps * (ySteps * cntrIndex + ij[1] - 1) + ij[0] - 1) + elle;

            if (!workSpace[i]) {
                double x = cv[cntrIndex];
                if ((x > Math.min(z1, z2)) && (x <= Math.max(z1, z2))) {
                    workSpace[i] = true;
                    return true;
                }
            }
        }
        return false;
    }

    //-------------------------------------------------------
    // "ContinueContour" continues tracing a contour. Edges
    // are numbered clockwise, the bottom edge being # 1.
    //-------------------------------------------------------
    void ContinueContour() {
        short local_k;

        ni = 1;
        if (iedge >= 3) {
            ij[0] = ij[0] - i3[iedge - 1];
            ij[1] = ij[1] - i3[iedge + 1];
        }
        for (local_k = 1; local_k < 5; local_k++) {
            if (local_k != iedge) {
                ii = ij[0] + i3[local_k - 1];
                jj = ij[1] + i3[local_k];
                z1 = z[ii - 1][jj - 1];
                ii = ij[0] + i3[local_k];
                jj = ij[1] + i3[local_k + 1];
                z2 = z[ii - 1][jj - 1];
                if ((cval > Math.min(z1, z2) && (cval <= Math.max(z1, z2)))) {
                    if ((local_k == 1) || (local_k == 4)) {
                        double zz = z2;

                        z2 = z1;
                        z1 = zz;
                    }
                    intersect[local_k - 1] = (cval - z1) / (z2 - z1);
                    ni++;
                    ks = local_k;
                }
            }
        }
        if (ni != 2) {
            //-------------------------------------------------
            // The contour crosses all 4 edges of cell being
            // examined. Choose lines top-to-left & bottom-to-
            // right if interpolation point on top edge is
            // less than interpolation point on bottom edge.
            // Otherwise, choose the other pair. This method
            // produces the same results if axes are reversed.
            // The contour may close at any edge, but must not
            // cross itself inside any cell.
            //-------------------------------------------------
            ks = 5 - iedge;
            if (intersect[2] >= intersect[0]) {
                ks = 3 - iedge;
                if (ks <= 0) {
                    ks = ks + 4;
                }
            }
        }
        //----------------------------------------------------
        // Determine whether the contour will close or run
        // into a boundary at edge ks of the current cell.
        //----------------------------------------------------
        elle = ks - 1;
        iflag = 1;		// 1. Continue a contour
        jump = true;
        if (ks >= 3) {
            ij[0] = ij[0] + i3[ks - 1];
            ij[1] = ij[1] + i3[ks + 1];
            elle = ks - 3;
        }
    }

    //-------------------------------------------------------
    // "ContourPlotKernel" is the guts of this class and
    // corresponds to Synder's subroutine "GCONTR".
    //-------------------------------------------------------
    void ContourPlotKernel(boolean workSpace[]) {
        short val_label_200;

        l1[0] = xSteps;
        l1[1] = ySteps;
        l1[2] = -1;
        l1[3] = -1;
        i1[0] = 1;
        i1[1] = 0;
        i2[0] = 1;
        i2[1] = -1;
        i3[0] = 1;
        i3[1] = 0;
        i3[2] = 0;
        i3[3] = 1;
        i3[4] = 1;
        i3[5] = 0;
        prevXY[0] = 0.0;
        prevXY[1] = 0.0;
        xy[0] = 1.0;
        xy[1] = 1.0;
        cntrIndex = 0;
        prevIndex = -1;
        iflag = 6;
        DrawKernel();
        icur = Math.max(1, Math.min((int) Math.floor(xy[0]), xSteps));
        jcur = Math.max(1, Math.min((int) Math.floor(xy[1]), ySteps));
        ibkey = 0;
        ij[0] = icur;
        ij[1] = jcur;
        if (Routine_label_020()
                && Routine_label_150()) {
            return;
        }
        if (Routine_label_050()) {
            return;
        }
        while (true) {
            DetectBoundary();
            if (jump) {
                if (ix != 0) {
                    iflag = 4; // Finish contour at boundary
                }
                iedge = ks + 2;
                if (iedge > 4) {
                    iedge = iedge - 4;
                }
                intersect[iedge - 1] = intersect[ks - 1];
                val_label_200 = Routine_label_200(workSpace);
                if (val_label_200 == 1) {
                    if (Routine_label_020() && Routine_label_150()) {
                        return;
                    }
                    if (Routine_label_050()) {
                        return;
                    }
                    continue;
                }
                if (val_label_200 == 2) {
                    continue;
                }
                return;
            }
            if ((ix != 3) && (ix + ibkey != 0) && CrossedByContour(workSpace)) {
                //
                // An acceptable line segment has been found.
                // Follow contour until it hits a
                // boundary or closes.
                //
                iedge = elle + 1;
                cval = cv[cntrIndex];
                if (ix != 1) {
                    iedge = iedge + 2;
                }
                iflag = 2 + ibkey;
                intersect[iedge - 1] = (cval - z1) / (z2 - z1);
                val_label_200 = Routine_label_200(workSpace);
                if (val_label_200 == 1) {
                    if (Routine_label_020() && Routine_label_150()) {
                        return;
                    }
                    if (Routine_label_050()) {
                        return;
                    }
                    continue;
                }
                if (val_label_200 == 2) {
                    continue;
                }
                return;
            }
            if (++elle > 1) {
                elle = idir % 2;
                ij[elle] = sign(ij[elle], l1[k - 1]);
                if (Routine_label_150()) {
                    return;
                }
            }
            if (Routine_label_050()) {
                return;
            }
        }
    }

    /*
     *  attach the segment to the existing one or create a new segment
     *
     * iflag == 1 means Continue a contour
     * iflag == 2 means Start a contour at a boundary
     * iflag == 3 means Start a contour not at a boundary
     * iflag == 4 means Finish contour at a boundary
     * iflag == 5 means Finish closed contour not at boundary
     * iflag == 6 means Set pen position
     *
     */
    private void contourSegment(double prevU, double prevV, double u, double v, double z) {
        int i = 0;
        for (i = 0; i < contours.size(); i++) {
            Contour c = contours.get(i);
            if (!c.finished) {
                Point p = c.data.get(c.data.size() - 1);
                if (p.x == prevU && p.y == prevV) {
                    c.data.add(new Point(u, v));
                    if (iflag == 4) {
                        c.finished = true;
                    } else if (iflag == 5) {
                        c.finished = true;
                        c.closed = true;
                    }
                    break;
                }
            }
        }
        if (i == contours.size()) {  // start a new contour
            Contour c = new Contour();
            c.z = z;
            c.data.add(new Point(prevU, prevV));
            c.data.add(new Point(u, v));
            contours.add(c);
        }
    }
}
