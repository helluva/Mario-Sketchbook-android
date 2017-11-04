package com.example.jakewaldner.ario;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.jakewaldner.ario11.R;

/**
 * Created by cal on 11/4/17.
 */

public class ARioSurfaceView extends SurfaceView {

    public ARioSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
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
            Canvas canvas = surfaceHolder.getSurface().lockCanvas(null);
            Bitmap scene = BitmapFactory.decodeResource(getResources(), R.drawable.ario_scene_stub);
            canvas.drawBitmap(scene, null, new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), null);
            surfaceHolder.getSurface().unlockCanvasAndPost(canvas);
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
