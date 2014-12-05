package com.codli.acrobot.acrobot;

/**
 * Created by sidor on 12/4/14.
 */
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;



import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.os.Handler;

public class DrawView extends View implements OnTouchListener {
    private static final String TAG = "DrawView";
    private static final int update_interval_ms = 25;

    Point point = null;
    Paint paint = new Paint();

    float t1 = 0.1f;
    float t2 = 0.1f;
    float t1_d = 0.0f;
    float t2_d = 0.0f;
    float L1 = 1.0f;
    float L2 = 2.0f;
    float G = 9.8f;
    float M1 = 1.0f;
    float M2 = 1.0f;
    float u = 0.0f;

    float MAX_U = 10.0f;
    float MIN_U = -10.0f;

    public DrawView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);

        this.setOnTouchListener(this);
        invalidate();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        final DrawView self = this;
        final Handler h = new Handler();
        Runnable update = new Runnable() {
            @Override
            public void run() {
                self.update_state();
                self.invalidate();
                h.postDelayed(this, self.update_interval_ms);
            }
        };


        update.run();

    }

    long last_update = 0;


    private void update_state() {
        final int RESOLUTION = 1000;
        final float FRICTION = 0.1f;
        float delta_update_ms = 0.0f;
        if (System.currentTimeMillis() - last_update > 1000.0) {
            // time travel or initialization.
            delta_update_ms = update_interval_ms;
        } else {
            delta_update_ms = System.currentTimeMillis() - last_update;
        }
        last_update = System.currentTimeMillis();


        float acc_per_sec = 10.0f;
        float decay_speed = 5.0f;

        float du = delta_update_ms/1000.0f * acc_per_sec;
        float w = this.getWidth();
        float h = this.getHeight();
        if (point != null) {
            if (point.x < this.getWidth()/2) {
                u = Math.max(MIN_U, u - du);
            } else {
                u = Math.min(MAX_U, u + du);
            }
            if (point.y > 0.9f * h) {
                float progress = ((float)point.x/w - 0.1f)/0.8f;
                progress = Math.min(1.0f, Math.max(0.0f, progress));
                u = MIN_U + progress * (MAX_U - MIN_U);
            }
        } else {
            u -= Math.signum(u) * Math.min(Math.abs(u), decay_speed * delta_update_ms / 1000.0f);
        }

        for (int i=0; i<RESOLUTION; ++i) {
            float[] d = new float[4];

            /*0 t1
              1 t1_d
              2 t2
              3 t2_d
                    */
            d[0] = t1_d;
            d[2] = t2_d;


            float del_ = t2 - t1;
            float den1 = (M1 + M2) * L1 - M2 * L1 * (float) Math.cos(del_) * (float) Math.cos(del_);
            d[1] = (M2 * L1 * t1_d * t1_d * (float) Math.sin(del_) * (float) Math.cos(del_)
                    + M2 * G * (float) Math.sin(t2) * (float) Math.cos(del_) + M2 * L2 * t2_d * t2_d * (float) Math.sin(del_)
                    - (M1 + M2) * G * (float) Math.sin(t1)) / den1;

            float den2 = (L2 / L1) * den1;
            d[3] = (-M2 * L2 * t2_d * t2_d * (float) Math.sin(del_) * (float) Math.cos(del_)
                    + (M1 + M2) * G * (float) Math.sin(t1) * (float) Math.cos(del_)
                    - (M1 + M2) * L1 * t1_d * t1_d * (float) Math.sin(del_)
                    - (M1 + M2) * G * (float) Math.sin(t2)) / den2;
            float dt = (float) delta_update_ms / 1000.0f/RESOLUTION;

            // friction ?
            d[1] -= FRICTION * t1_d;
            d[3] -= FRICTION * t2_d;

            t1 += d[0] * dt;
            t1_d += d[1] * dt;
            t2 += d[2] * dt;
            t2_d += (d[3]+u) * dt;
        }

    }

    @Override
    public void onDraw(Canvas canvas) {

        float x1 = L1*(float)Math.sin(t1);
        float y1 = L1*(float)Math.cos(t1);

        float x2 = L2*(float)Math.sin(t2) + x1;
        float y2 = L2*(float)Math.cos(t2) + y1;
        float h = canvas.getHeight();
        float w = canvas.getWidth();
        float c_x = w/2.0f;
        float c_y = h/2.0f;
        float s = 0.8f/(L1+L2) * (float)Math.min(c_x, c_y);
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);
        paint.setStyle(Paint.Style.FILL);

        canvas.drawCircle(c_x + s*x1, c_y+s*y1, 15, paint);
        canvas.drawCircle(c_x + s*x2, c_y+s*y2, 15, paint);
        canvas.drawLine(c_x, c_y, c_x + s * x1, c_y + s * y1, paint);
        canvas.drawLine(c_x + s*x1,c_y+s*y1, c_x + s*x2, c_y+s*y2, paint);

        paint.setColor(Color.RED);
        canvas.drawCircle(c_x, c_y, 15, paint);

        paint.setColor(Color.BLACK);

        paint.setTextSize(w/20);
        DecimalFormat df = new DecimalFormat("#.0");

        canvas.drawText("u = " + df.format(u), w/20+10, w/20+10, paint);

        paint.setColor(Color.BLUE);
        float where_u = (u-MIN_U)/(MAX_U - MIN_U);


        canvas.drawLine(w*0.1f, h*0.95f, w*0.9f, h*0.95f, paint);
        paint.setColor(Color.RED);
        canvas.drawCircle(w*(0.1f + 0.8f*where_u), h*0.95f, 15, paint);



    }

    public boolean onTouch(View view, MotionEvent event) {

        Point point = new Point();
        point.x = event.getX();
        point.y = event.getY();

        if (event.getAction() == event.ACTION_UP || event.getAction() == event.ACTION_CANCEL || event.getAction() == event.ACTION_OUTSIDE) {
            this.point = null;
        } else {
            this.point = point;
        }
        invalidate();
        return true;
    }
}

class Point {
    float x, y;

    @Override
    public String toString() {
        return x + ", " + y;
    }
}