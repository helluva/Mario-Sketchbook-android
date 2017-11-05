package com.example.jakewaldner.ario11;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;

public class MarioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mario);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ARioSurfaceView arioSurface = (ARioSurfaceView) findViewById(R.id.ario_surface);

        Button leftButton = (Button) findViewById(R.id.leftButton);
        leftButton.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (arioSurface == null) {
                    System.out.println("no surface");
                    return true;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    arioSurface.movingLeft = true;
                } else {
                    arioSurface.movingLeft = false;
                }

                return true;
            }

        });

        Button rightButton = (Button) findViewById(R.id.rightButton);
        rightButton.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (arioSurface == null) {
                    System.out.println("no surface");
                    return true;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    arioSurface.movingRight = true;
                } else {
                    arioSurface.movingRight = false;
                }

                return true;
            }

        });

        Button jumpButton = (Button) findViewById(R.id.jumpButton);
        jumpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (arioSurface == null) {
                    System.out.println("no surface");
                    return;
                }

                arioSurface.jumpIfPossible();
            }
        });
    }

}
