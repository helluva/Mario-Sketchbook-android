package com.example.jakewaldner.ario;

import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by cal on 11/4/17.
 */

public class ARioSurfaceView extends SurfaceView {

    public ARioSurfaceView(Context context) {
        super(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.getHolder().addCallback(new ARioSurfaceViewCallback());
    }

    public class ARioSurfaceViewCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            System.out.println("Surface created");
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
}
