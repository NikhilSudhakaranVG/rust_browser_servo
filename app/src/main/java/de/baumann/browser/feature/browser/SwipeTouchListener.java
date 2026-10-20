package de.baumann.browser.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SwipeTouchListener implements View.OnTouchListener {

    private final GestureDetector gestureDetector;

    public SwipeTouchListener(@NonNull Context ctx) {
        this.gestureDetector = new GestureDetector(ctx, new GestureListener());
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event == null) return false;
        return gestureDetector.onTouchEvent(event);
    }
    public void onSwipeRight() {}
    public void onSwipeLeft() {}
    public void onSwipeTop() {}
    public void onSwipeBottom() {}

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            // WICHTIG: Bleibt false, damit Klicks & Long-Clicks in der Activity landen!
            return false;
        }

        @Override
        public boolean onFling(@Nullable MotionEvent e1, @Nullable MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                        return true;
                    }
                }
                else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom();
                    } else {
                        onSwipeTop();
                    }
                    return true;
                }
            } catch (Exception e) {
                Log.e("FOSS Browser", "Gesten-Fehler: " + e.getMessage());
            }
            return false;
        }
    }
}