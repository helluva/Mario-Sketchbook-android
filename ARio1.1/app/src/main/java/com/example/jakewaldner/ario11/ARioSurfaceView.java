package com.example.jakewaldner.ario11;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.jakewaldner.ario11.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;

/**
 * Created by cal on 11/4/17.
 */

public class ARioSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    // MARIO

    public int marioX = 150;
    public int marioY = 150;

    public int marioWidth = 150;
    public int marioHeight = (int)((double)marioWidth * (100.0/110.0));

    public boolean movingLeft = false;
    public boolean movingRight = false;

    private Date jumpStartTime = null;
    long jumpDurationMillis = 1000;

    public void jumpIfPossible() {
        if (jumpStartTime == null) {
            jumpStartTime = new Date();
            return;
        }

        long timeSinceJumpStart = System.currentTimeMillis() - jumpStartTime.getTime();
        if (timeSinceJumpStart > jumpDurationMillis) {
            jumpStartTime = new Date();
        }
    }

    private void updatePhysics() {
        if (movingRight) {
            boolean success = commitOffsetIfNoClipping(4, 0, false, true);
            if (!success) { //try to walk over a small hump if mario can't move sideways
                commitOffsetIfNoClipping(4, -10, false, true);
            }
        }

        if (movingLeft) {
            boolean success = commitOffsetIfNoClipping(-4, 0, false, true);
            if (!success) { //try to walk over a small hump if mario can't move sideways
                commitOffsetIfNoClipping(-4, -10, false, true);
            }
        }

        commitOffsetIfNoClipping(0, 4, false, true); //gravity
        commitOffsetIfNoClipping(0, 4, false, true); //gravity
        commitOffsetIfNoClipping(0, 2, false, true); //gravity

        if (jumpStartTime != null) {
            long timeSinceJumpStart = System.currentTimeMillis() - jumpStartTime.getTime();
            if (timeSinceJumpStart > jumpDurationMillis) {
                jumpStartTime = null;
            } else {
                double jumpProgress = ((double)timeSinceJumpStart) / ((double)jumpDurationMillis);
                double jumpCurve = -pow(jumpProgress, 2) + 1; // -x^2 + 1

                commitOffsetIfNoClipping(0, -(int)(jumpCurve * 25),false, false);
            }
        }

        if (canvasHeight != 0 && canvasWidth != 0) {
            marioY = min(max(0, marioY), canvasHeight - marioHeight);
            marioX = min(max(0, marioX), canvasWidth - marioWidth);
        }

    }

    private boolean commitOffsetIfNoClipping(int xOffset, int yOffset, boolean onTop, boolean onBottom) {
        if (!marioClipsWithContoursWhenOffsetBy(xOffset, yOffset, onTop, onBottom)) {
            marioX += xOffset;
            marioY += yOffset;

            System.out.println("OFFSET: (" + xOffset + "," + yOffset + ")");
            return true;
        }

        return false;
    }

    private boolean marioClipsWithContoursWhenOffsetBy(int xOffset, int yOffset, boolean onTop, boolean onBottom) {
        int originX = marioX + xOffset;
        int originY = marioY + yOffset;

        return (onTop && pointClipsWithContours(originX, originY))
                || (onTop && pointClipsWithContours(originX + (int)((double)marioWidth * 0.5), originY))
                || (onTop && pointClipsWithContours(originX + marioWidth, originY))
                || (onBottom && pointClipsWithContours(originX, originY + marioHeight))
                || (onBottom && pointClipsWithContours(originX + (int)((double)marioWidth * 0.5), originY + marioHeight))
                || (onBottom && pointClipsWithContours(originX + marioWidth, originY + marioHeight));
    }

    private boolean pointClipsWithContours(int x, int y) {
        if (allContours == null) {
            return false;
        }

        for (MatOfPoint intContour : allContours) {
            MatOfPoint2f floatContour = new MatOfPoint2f();
            intContour.convertTo(floatContour, CvType.CV_32FC2);

            if (Imgproc.pointPolygonTest(floatContour, new Point(x, y), true) >= -1) {
                return true;
            }
        }

        return false;
    }



    Bitmap uncroppedBackground = BitmapFactory.decodeResource(getResources(), R.drawable.ario_scene_uncropped_stub);
    Bitmap marioSprite = BitmapFactory.decodeResource(getResources(), R.drawable.mario_small);

    Bitmap contourBitmap = null;
    List<MatOfPoint> allContours = null;


    private void renderCanvas() {
        if (surfaceHolder == null) {
            return;
        }

        Canvas canvas = surfaceHolder.getSurface().lockCanvas(null);
        this.canvasHeight = canvas.getHeight();
        this.canvasWidth = canvas.getWidth();

        canvas.drawColor(Color.BLACK);

        //skew in 3d
        /*Camera cam = new Camera();
        cam.translate(0, 200, 200);
        cam.rotateX(25);

        Matrix m = new Matrix();
        cam.getMatrix(m);

        int CenterX = canvasWidth / 2;
        int CenterY = canvasHeight / 2;
        m.preTranslate(-CenterX, -CenterY); //This is the key to getting the correct viewing perspective
        m.postTranslate(CenterX, CenterY);
        canvas.setMatrix(m);*/


        canvas.drawBitmap((contourBitmap == null ? uncroppedBackground : contourBitmap),
                null,
                new Rect(0, 0, canvasWidth, canvasHeight),
                null);

        canvas.drawBitmap(marioSprite,
                null,
                new Rect(marioX, marioY, marioX + marioHeight, marioY + marioHeight),
                null);

        surfaceHolder.getSurface().unlockCanvasAndPost(canvas);
    }

    public Bitmap generateSceneBitmapFromUncroppedImage() {
        Bitmap scaledUncropped = Bitmap.createScaledBitmap(uncroppedBackground, canvasWidth, canvasHeight, false);

        Mat src = new Mat();
        Utils.bitmapToMat(scaledUncropped, src);
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGB2GRAY);
        Imgproc.blur(src, src, new Size(3, 3));

        Mat edgesMat = new Mat();
        Imgproc.Canny(src, edgesMat, 0, 200);

        //find the largest rectangle
        Mat mHierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(edgesMat, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        MatOfPoint2f largestRectangle = null;

        for (MatOfPoint intContour : contours) {
            MatOfPoint2f floatContour = new MatOfPoint2f();
            intContour.convertTo(floatContour, CvType.CV_32FC2);

            double arcLength = Imgproc.arcLength(floatContour, true);
            MatOfPoint2f approximateContour = new MatOfPoint2f();
            Imgproc.approxPolyDP(floatContour, approximateContour, 0.02 * arcLength, true);

            System.out.println(approximateContour.toList().size());

            if (approximateContour.toList().size() == 4) {
                double area = Imgproc.contourArea(floatContour);
                if (largestRectangle == null || area > Imgproc.contourArea(largestRectangle)) {
                    largestRectangle = approximateContour;
                }
            }
        }

        ArrayList<MatOfPoint> contourToDraw = new ArrayList<>();

        //MatOfPoint intContour = new MatOfPoint();
        //largestRectangle.convertTo(intContour, CvType.CV_32S);
        //contourToDraw.add(intContour);
        //Imgproc.drawContours(src, contourToDraw, 0, new Scalar(255, 0, 0, 255), 10);

        System.out.println(largestRectangle.toArray()[0] +"" + largestRectangle.toArray()[1] +""+ largestRectangle.toArray()[2] + ""+largestRectangle.toArray()[3]);

        MatOfPoint2f destinationMat = new MatOfPoint2f(
                new Point(canvasWidth - 1, 1),
                new Point(1, 1),
                new Point(1, canvasHeight - 1),
                new Point(canvasWidth - 1, canvasHeight - 1)
        );

        Mat croppedMat = new Mat();
        Imgproc.warpPerspective(src, croppedMat,
                Imgproc.getPerspectiveTransform(largestRectangle, destinationMat),
                new Size(canvasWidth, canvasHeight));

        Bitmap tempBmp1 = Bitmap.createBitmap(canvasWidth, canvasHeight, uncroppedBackground.getConfig());
        Utils.matToBitmap(croppedMat, tempBmp1);
        return tempBmp1;
    }

    public void generateContourBitmap(Bitmap croppedBitmap) {

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, canvasWidth, canvasHeight, false);

        Mat src = new Mat();
        Utils.bitmapToMat(scaledBitmap, src);
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGB2GRAY);
        Imgproc.dilate (src, src, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));
        Imgproc.blur(src, src, new Size(3, 3));

        //this finds all edge points

        Mat edgesMat = new Mat();
        Imgproc.Canny(src, edgesMat, 0, 200);

        //this finds contours, connected edge points
        Mat mHierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(edgesMat, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        Mat roughContoursMat = src.clone();

        System.out.println("INITIALLY loaded " + contours.size() + " contours");
        // find contours:
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            Imgproc.drawContours(roughContoursMat, contours, contourIdx, new Scalar(0, 0, 255), 40);
        }

        Imgproc.blur(src, roughContoursMat, new Size(3, 3));

        Mat edgesMat2 = new Mat();
        Imgproc.Canny(roughContoursMat, edgesMat2, 0, 200);

        Mat mHierarchy2 = new Mat();
        List<MatOfPoint> contours2 = new ArrayList<>();
        Imgproc.findContours(edgesMat2, contours2, mHierarchy2, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        System.out.println("then loaded " + contours2.size() + " contours");

        // find contours:
        for (int contourIdx = 0; contourIdx < contours2.size(); contourIdx++) {
            Imgproc.drawContours(src, contours2, contourIdx, new Scalar(0, 150, 255, 150), 5);
        }

        allContours = contours2;

        Bitmap tempBmp1 = Bitmap.createBitmap(canvasWidth, canvasHeight, scaledBitmap.getConfig());
        Utils.matToBitmap(src, tempBmp1);
        contourBitmap = tempBmp1;
    }


    // Surface view setup

    private SurfaceHolder surfaceHolder = null;
    private Handler physicsHandler = new Handler();

    private int canvasHeight;
    private int canvasWidth;

    private Runnable physicsRunnable = new Runnable() {
        @Override
        public void run() {
            renderCanvas();
            updatePhysics();

            //repeat
            physicsHandler.postDelayed(physicsRunnable,3);
        }
    };

    public ARioSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.getHolder().addCallback(this);
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this.getContext()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Bitmap croppedBitmap = generateSceneBitmapFromUncroppedImage();
                    generateContourBitmap(croppedBitmap);
                    physicsRunnable.run();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    // Callbacks

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        System.out.println("Surface created");
        this.surfaceHolder = surfaceHolder;
        renderCanvas();

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_13, this.getContext(), mLoaderCallback);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        System.out.println("Surface changed");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        System.out.println("Surface destroyed");
    }

}
