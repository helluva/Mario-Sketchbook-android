package com.example.jakewaldner.ario11;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.jakewaldner.ario11.R;

import java.util.Date;

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
        marioY += 15; //gravity

        if (movingRight) marioX += 12;
        if (movingLeft) marioX -= 12;

        if (jumpStartTime != null) {
            long timeSinceJumpStart = System.currentTimeMillis() - jumpStartTime.getTime();
            if (timeSinceJumpStart > jumpDurationMillis) {
                jumpStartTime = null;
            } else {
                double jumpProgress = ((double)timeSinceJumpStart) / ((double)jumpDurationMillis);
                double jumpCurve = -pow(jumpProgress, 2) + 1; // -x^2 + 1
                marioY -= (int)(jumpCurve * 35.0);
            }
        }

        if (canvasHeight != 0 && canvasWidth != 0) {
            marioY = min(max(0, marioY), canvasHeight - marioHeight);
            marioX = min(max(0, marioX), canvasWidth - marioWidth);
        }

    }



    Bitmap sceneBackground = BitmapFactory.decodeResource(getResources(), R.drawable.ario_scene_stub);
    Bitmap marioSprite = BitmapFactory.decodeResource(getResources(), R.drawable.mario_small);

    private void renderCanvas() {
        if (surfaceHolder == null) {
            return;
        }

        Canvas canvas = surfaceHolder.getSurface().lockCanvas(null);
        this.canvasHeight = canvas.getHeight();
        this.canvasWidth = canvas.getWidth();

        canvas.drawBitmap(sceneBackground,
                null,
                new Rect(0, 0, canvasWidth, canvasHeight),
                null);

        canvas.drawBitmap(marioSprite,
                null,
                new Rect(marioX, marioY, marioX + marioHeight, marioY + marioHeight),
                null);

        surfaceHolder.getSurface().unlockCanvasAndPost(canvas);
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
            physicsHandler.postDelayed(physicsRunnable,10);
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


    // Callbacks

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        System.out.println("Surface created");
        this.surfaceHolder = surfaceHolder;

        physicsRunnable.run();
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
