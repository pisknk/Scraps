package com.playpass.scraps;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;

public class AnimatedGradientDrawable extends Drawable {
    private Paint paint;
    private int[] colors;
    private float angle = 0;
    private ValueAnimator animator;

    public AnimatedGradientDrawable() {
        paint = new Paint();
        colors = new int[]{
            0xFFFF6B6B,  // coral
            0xFF4ECDC4,  // teal
            0xFF45B7D1   // blue
        };
        setupAnimator();
    }

    private void setupAnimator() {
        animator = ValueAnimator.ofFloat(0, 360);
        animator.setDuration(3000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            angle = (float) animation.getAnimatedValue();
            invalidateSelf();
        });
    }

    @Override
    public void draw(Canvas canvas) {
        float centerX = getBounds().width() / 2f;
        float centerY = getBounds().height() / 2f;
        float radius = (float) Math.sqrt(centerX * centerX + centerY * centerY);
        
        float angleRad = (float) Math.toRadians(angle);
        float startX = centerX - radius * (float) Math.cos(angleRad);
        float startY = centerY - radius * (float) Math.sin(angleRad);
        float endX = centerX + radius * (float) Math.cos(angleRad);
        float endY = centerY + radius * (float) Math.sin(angleRad);

        LinearGradient gradient = new LinearGradient(
            startX, startY, endX, endY,
            colors, null, Shader.TileMode.CLAMP
        );
        paint.setShader(gradient);
        canvas.drawRect(getBounds(), paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(android.graphics.ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return android.graphics.PixelFormat.TRANSLUCENT;
    }

    public void start() {
        animator.start();
    }

    public void stop() {
        animator.cancel();
    }
} 