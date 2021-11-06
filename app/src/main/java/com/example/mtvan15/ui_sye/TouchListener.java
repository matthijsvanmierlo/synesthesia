package com.example.mtvan15.ui_sye;

import android.view.MotionEvent;
import android.view.View;

public class TouchListener implements View.OnTouchListener {
    float x = -1;
    float y = -1;

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        this.x = motionEvent.getX();
        this.y = motionEvent.getY();

        return true;
    }

    public float[] getCoordinates(){
        return new float[]{this.x, this.y};
    }
}
