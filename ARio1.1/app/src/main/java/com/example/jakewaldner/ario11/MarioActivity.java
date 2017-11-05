package com.example.jakewaldner.ario11;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
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

        byte[] byteArray = getIntent().getByteArrayExtra("croppedRect");
        Bitmap uncroppedBackground = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        arioSurface.uncroppedBackground = uncroppedBackground;

        Button goToCodesListButton = (Button) this.findViewById(R.id.return_button);
        goToCodesListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MarioActivity.this, CameraViewActivity.class);
                MarioActivity.this.startActivity(i);
            }
        });

        Button leftButton = (Button) findViewById(R.id.leftButton);
        leftButton.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (arioSurface == null) {
                    System.out.println("no surface");
                    return true;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN || motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
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

                System.out.println(motionEvent.getAction());

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN || motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.newstage_button) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
