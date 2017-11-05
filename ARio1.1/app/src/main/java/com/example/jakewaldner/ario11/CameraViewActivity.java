package com.example.jakewaldner.ario11;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by jakewaldner on 11/4/17.
 */

//CvCameraListener2 is just specific to the "onInputFrame" method taking in a cameraView frame as opposed to a Mat
public class CameraViewActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    Context context = this;
    private static final String TAG = "AndroidCameraApi";

    //this is to ensure that openCV is async loaded
    Mat testMat;
    private CameraBridgeViewBase mOpenCvCameraView;

//vars for the stage bounds
    Rect maxRect;
    int frameCounter = 1;
    LinkedList<Integer> sumMaxRectX1 = new LinkedList<>();
    LinkedList<Integer> sumMaxRectY1 = new LinkedList<>();
    LinkedList<Integer> sumMaxRectX2 = new LinkedList<>();
    LinkedList<Integer> sumMaxRectY2 = new LinkedList<>();
    //make arrays!!!!!!!!


    int maxRectArea = 0;
    boolean maxRectFound = false;
    int maxRectX;
    int maxRectY;
    int maxRectWidth;
    int maxRectHeight;

    //open CV's camera requires an async call
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    testMat = new Mat();
                    //an online example instantiated all Mats here

                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 100);
        }

        setContentView(R.layout.activity_cameraview);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        Button cropButton = (Button) this.findViewById(R.id.crop_button);
        cropButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cameraFrame != null && maxRect != null) {
                    System.out.println(maxRect.x);
                    System.out.println(maxRect.y);
                    System.out.println(maxRect.width);
                    System.out.println(maxRect.height);
                    try {
                        Mat imageCroppedMat = new Mat(cameraFrame, maxRect);
                        imageCroppedMat.release();
                    } catch(CvException e) {
                        Toast.makeText(CameraViewActivity.this, "Whoops, didn't get that! Try again.",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        //CONSIDER THIS FOR LATER PERHAPS??
    }

    //THIS IS WHERE THE PROCESSING MEAT GOES
    Mat returnedFrameMat;
    Mat cameraFrame;
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //THIS FRAME IS NOW A MAT OBJECT
        cameraFrame = inputFrame.rgba();
        Log.d("Mat1", "" + cameraFrame);

        //grayscale the frame
        Mat newGrayFrame = new Mat();
        Imgproc.cvtColor(cameraFrame, newGrayFrame, Imgproc.COLOR_RGB2GRAY);

        Mat dilatedFrame = new Mat();
        Imgproc.dilate (newGrayFrame, dilatedFrame, new Mat());

        Mat blurredFrame = new Mat();
        Imgproc.GaussianBlur(dilatedFrame, blurredFrame, new  org.opencv.core.Size(1, 1), 2, 2);

        //this finds all edge points
        Mat finalAlteredFrame = new Mat();
        Imgproc.Canny(blurredFrame, finalAlteredFrame, 0, 200, 3, true);
        Log.d("Mat2", "" + finalAlteredFrame);

        //this finds contours, connected edge points
        //find the contours (more specific edges :/)
        Mat mHierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(finalAlteredFrame, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        //freeing the data to prevent insufficient memory failure
        if (returnedFrameMat != null) {
            returnedFrameMat.release();
        }

        //this is so the temp rect stage border is overlaid on the original frame (this will be displayed)
        returnedFrameMat = cameraFrame.clone();


        //now that we have an array of contours, we need to determine what contours form rectangles and isolate the largest of these rectangles
        MatOfPoint temp_contour;
        for (int i = 0; i < contours.size(); i++) {
            temp_contour = contours.get(i);
            MatOfPoint2f temp_contour2f = new MatOfPoint2f(temp_contour.toArray());

            //THIS WORKS SLIGHTLY BETTER than the other way to get approxDistance
            double approxDistance = Imgproc.arcLength(temp_contour2f, true) * 0.02;

            MatOfPoint2f approxCurve_temp = new MatOfPoint2f();
            Imgproc.approxPolyDP(temp_contour2f, approxCurve_temp, approxDistance, true);

            Rect rect = Imgproc.boundingRect(temp_contour);

            if (maxRectArea < rect.width * rect.height) {
                maxRect = rect;

                maxRectX = rect.x;
                maxRectY = rect.y;
                maxRectWidth = rect.width;
                maxRectHeight = rect.height;

                maxRectArea = rect.width * rect.height;
                maxRectFound = true;
            }




            //TODO freeing the data in the Mats to possibly prevent insufficient memory failure
            temp_contour.release();
            temp_contour2f.release();
            approxCurve_temp.release();
        }


        //THIS IS TO CORRECT THE ORIENTATION DO NOT NEED ANYMORE
        //Mat returnedFrameMatT = returnedFrameMat.t();
        //Core.flip(returnedFrameMat.t(), returnedFrameMatT, 1);
        //Imgproc.resize(returnedFrameMatT, returnedFrameMat, returnedFrameMat.size());

        /*public int sumify(int rectXOrY) {

    }*/

        //COULD put this all in a function
        if(maxRectFound && verifySizes(maxRect)) {
            sumMaxRectX1.add(maxRectX);
            if (sumMaxRectX1.size() > 15) {
                //this keeps the average coord pool down
                sumMaxRectX1.remove();
            }
            int currMaxRectX1Sum = 0;
            for (int i = 0; i < sumMaxRectX1.size(); i++) {
                currMaxRectX1Sum = currMaxRectX1Sum + sumMaxRectX1.get(i);
            }

            sumMaxRectY1.add(maxRectY);
            if (sumMaxRectY1.size() > 15) {
                //this keeps the average coord pool down
                sumMaxRectY1.remove();
            }
            int currMaxRectY1Sum = 0;
            for (int i = 0; i < sumMaxRectY1.size(); i++) {
                currMaxRectY1Sum = currMaxRectY1Sum + sumMaxRectY1.get(i);
            }

            Point averageP1 = new Point(currMaxRectX1Sum / frameCounter, currMaxRectY1Sum / frameCounter);


            sumMaxRectX2.add(maxRectX + maxRectWidth);
            if (sumMaxRectX2.size() > 15) {
                //this keeps the average coord pool down
                sumMaxRectX2.remove();
            }
            int currMaxRectX2Sum = 0;
            for (int i = 0; i < sumMaxRectX2.size(); i++) {
                currMaxRectX2Sum = currMaxRectX2Sum + sumMaxRectX2.get(i);
            }

            sumMaxRectY2.add(maxRectY + maxRectHeight);
            if (sumMaxRectY2.size() > 15) {
                //this keeps the average coord pool down
                sumMaxRectY2.remove();
            }
            int currMaxRectY2Sum = 0;
            for (int i = 0; i < sumMaxRectX2.size(); i++) {
                currMaxRectY2Sum = currMaxRectY2Sum + sumMaxRectY2.get(i);
            }

            //sumMaxRectX2 = (sumMaxRectX2 + (maxRectX + maxRectWidth));
            //sumMaxRectY2 = (sumMaxRectY2 + (maxRectY + maxRectHeight));

            Point averageP2 = new Point(currMaxRectX2Sum / frameCounter, currMaxRectY2Sum / frameCounter);

            /*if (frameCounter < 15) {
                Core.rectangle(returnedFrameMat, new Point(maxRectX, maxRectY), new Point(maxRectX + maxRectWidth, maxRectY + maxRectHeight), new Scalar(255, 0, 0, 255), 3);

            } else {
                Core.rectangle(returnedFrameMat, averageP1, averageP2, new Scalar(255, 0, 0, 255), 3);
            }*/

            Core.rectangle(returnedFrameMat, averageP1, averageP2, new Scalar(255, 0, 0, 255), 3);

            //can just check one of them since they all increase the same rate
            if (sumMaxRectX1.size() < 15) {
                frameCounter++;
            } else {
                frameCounter = 15;
            }
        }
        //reset the boolean
        maxRectFound = false;
        maxRectArea = 0;


        //Imgproc.rectangle(returnedFrameMat, new Point(finalAlteredFrame.cols()/3, finalAlteredFrame.rows()/15), new Point((finalAlteredFrame.cols()/3) * 2, (finalAlteredFrame.rows()/15) * 14), new Scalar(255, 255, 0, 255), 5);


        blurredFrame.release();
        cameraFrame.release();
        newGrayFrame.release();
        dilatedFrame.release();
        finalAlteredFrame.release();
        mHierarchy.release();


        //just in case from async call
        testMat.release();

        //this ensures when a crop is made on a frame with no maxRect, it doesn't crash
        maxRect = null;

        return returnedFrameMat;
    }

    public boolean verifySizes(Rect rect) {
        double error = 0.35;
        //aspect ratio
        double aspect = 0.75;
        //Set a min and max area. All other patches are discarded
        double min = 100*aspect*100; // minimum area
        double max =800*aspect*800; // maximum area
        //Get only patches that match to the ratio.
        double rmin = aspect-aspect*error;
        double rmax = aspect+aspect*error;

        double area = rect.size().height * rect.size().width;
        //double r = rect.size().width / rect.size().height;
        //if(r < 1) {
        double r = rect.size().height / rect.size().width;
        //}

        System.out.println("ratio: " + r);

        if((area < min || area > max) ||(r < rmin || r > rmax)){
            if (area > max) {
                System.out.println("area problem");
            }
            //System.out.println("Drake in Canada");
            return false;
        } else {
            System.out.println("abcdefg");
            return true;
        }
    }

    @Override
    public void onCameraViewStopped() {
        // TODO Auto-generated method stub
        //MIGHT WANT TO TRY THAT MAT.RELSEASE THING FROM STACKO HERE TO POSSIBLY IMPR0VE FRAMERATE?
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");

        //checked if OpenCV library have been loaded and initialized from within current application package or not
        //THIS IS FOR THE MAT ERROR
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_13, CameraViewActivity.this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        //stopBackgroundThread();
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
}
