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

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
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

    //open CV's camera requires an async call
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    //testMat = new Mat();
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
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        //CONSIDER THIS FOR LATER PERHAPS??
    }

    //THIS IS WHERE THE PROCESSING MEAT GOES
    Mat returnedFrameMat;
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //THIS FRAME IS NOW A MAT OBJECT
        Mat cameraFrame = inputFrame.rgba();
        Log.d("Mat1", "" + cameraFrame);

        //grayscale the frame
        Mat newGrayFrame = new Mat();
        Imgproc.cvtColor(cameraFrame, newGrayFrame, Imgproc.COLOR_RGB2GRAY);

        Mat dilatedFrame = new Mat();
        Imgproc.dilate (newGrayFrame, dilatedFrame, new Mat());

        Mat blurredFrame = new Mat();
        Imgproc.GaussianBlur(dilatedFrame, blurredFrame, new  org.opencv.core.Size(75, 75), 20, 20);

        //this finds all edge points
        Mat finalAlteredFrame = new Mat();
        Imgproc.Canny(blurredFrame, finalAlteredFrame, 0, 200, 30, true);
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


        /*//now that we have an array of contours, we need to determine what contours form rectangles and isolate the largest of these rectangles
        MatOfPoint temp_contour;
        for (int i = 0; i < contours.size(); i++) {
            temp_contour = contours.get(i);
            MatOfPoint2f temp_contour2f = new MatOfPoint2f(temp_contour.toArray());

            //THIS WORKS SLIGHTLY BETTER than the other way to get approxDistance
            double approxDistance = Imgproc.arcLength(temp_contour2f, true) * 0.02;

            MatOfPoint2f approxCurve_temp = new MatOfPoint2f();
            Imgproc.approxPolyDP(temp_contour2f, approxCurve_temp, approxDistance, true);

            Rect rect = Imgproc.boundingRect(temp_contour);




            //TODO freeing the data in the Mats to possibly prevent insufficient memory failure
            temp_contour.release();
            temp_contour2f.release();
            approxCurve_temp.release();
        }*/


        //THIS IS TO CORRECT THE ORIENTATION
        //Mat returnedFrameMatT = returnedFrameMat.t();
        //Core.flip(returnedFrameMat.t(), returnedFrameMatT, 1);
        //Imgproc.resize(returnedFrameMatT, returnedFrameMat, returnedFrameMat.size());

        return returnedFrameMat;
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
