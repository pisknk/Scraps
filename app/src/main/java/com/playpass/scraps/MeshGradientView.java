package com.playpass.scraps;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import java.util.Random;

public class MeshGradientView extends View {
    private Paint paint;
    private int[] colors;
    private float[] positions;
    private PointF[][] controlPoints;
    private float[][] colorValues;
    private ValueAnimator animator;
    private Random random;
    private static final int GRID_SIZE = 3;

    public MeshGradientView(Context context) {
        super(context);
        init();
    }

    public MeshGradientView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MeshGradientView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        random = new Random();
        
        // initialize with new color palette
        colors = new int[] {
            Color.parseColor("#6520a8"), // deep purple
            Color.parseColor("#beaef1"), // light purple
            Color.parseColor("#8a7bd4"), // medium purple
            Color.parseColor("#9508a1"), // magenta
            Color.parseColor("#745ac4"), // violet
            Color.parseColor("#40238f"), // dark purple
            Color.parseColor("#7c94d1"), // periwinkle
            Color.parseColor("#6520a8")  // deep purple again for smooth transition
        };
        
        // initialize control points and color values
        controlPoints = new PointF[GRID_SIZE + 1][GRID_SIZE + 1];
        colorValues = new float[GRID_SIZE + 1][GRID_SIZE + 1];
        
        for (int i = 0; i <= GRID_SIZE; i++) {
            for (int j = 0; j <= GRID_SIZE; j++) {
                controlPoints[i][j] = new PointF();
                colorValues[i][j] = random.nextFloat();
            }
        }
        
        setupAnimator();
    }

    private void setupAnimator() {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(40000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            updateControlPoints(value);
            invalidate();
        });
    }

    private void updateControlPoints(float value) {
        float width = getWidth();
        float height = getHeight();
        // oversize mesh by 20% in both dimensions
        float meshWidth = width * 1.2f;
        float meshHeight = height * 1.2f;
        float offsetXMesh = (width - meshWidth) / 2f;
        float offsetYMesh = (height - meshHeight) / 2f;
        float cellWidth = meshWidth / GRID_SIZE;
        float cellHeight = meshHeight / GRID_SIZE;
        
        for (int i = 0; i <= GRID_SIZE; i++) {
            for (int j = 0; j <= GRID_SIZE; j++) {
                // base position
                float baseX = j * cellWidth + offsetXMesh;
                float baseY = i * cellHeight + offsetYMesh;
                
                // add smooth movement
                float offsetX = (float) Math.sin(value * Math.PI * 2 + i * 0.5f + j * 0.3f) * cellWidth * 0.2f;
                float offsetY = (float) Math.cos(value * Math.PI * 2 + i * 0.3f + j * 0.5f) * cellHeight * 0.2f;
                
                controlPoints[i][j].x = baseX + offsetX;
                controlPoints[i][j].y = baseY + offsetY;
                
                // update color values
                colorValues[i][j] = (float) (Math.sin(value * Math.PI * 2 + i * 0.2f + j * 0.2f) * 0.5f + 0.5f);
            }
        }
    }

    private int interpolateColor(float t) {
        int index = (int) (t * (colors.length - 1));
        float localT = t * (colors.length - 1) - index;
        
        int color1 = colors[index];
        int color2 = colors[index + 1];
        
        return interpolateColor(color1, color2, localT);
    }

    private int interpolateColor(int color1, int color2, float t) {
        int a1 = Color.alpha(color1);
        int r1 = Color.red(color1);
        int g1 = Color.green(color1);
        int b1 = Color.blue(color1);
        
        int a2 = Color.alpha(color2);
        int r2 = Color.red(color2);
        int g2 = Color.green(color2);
        int b2 = Color.blue(color2);
        
        return Color.argb(
            (int) (a1 + (a2 - a1) * t),
            (int) (r1 + (r2 - r1) * t),
            (int) (g1 + (g2 - g1) * t),
            (int) (b1 + (b2 - b1) * t)
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        float cellWidth = getWidth() / (float) GRID_SIZE;
        float cellHeight = getHeight() / (float) GRID_SIZE;
        
        // draw mesh cells
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                // get the four corners
                PointF p00 = controlPoints[i][j];
                PointF p01 = controlPoints[i][j + 1];
                PointF p11 = controlPoints[i + 1][j + 1];
                PointF p10 = controlPoints[i + 1][j];
                
                // calculate average color value for this cell
                float avgValue = (colorValues[i][j] + colorValues[i][j + 1] + 
                                colorValues[i + 1][j + 1] + colorValues[i + 1][j]) / 4f;
                paint.setColor(interpolateColor(avgValue));
                paint.setAlpha(200);
                
                // create a path for a sharp-edged quadrilateral
                Path path = new Path();
                path.moveTo(p00.x, p00.y);
                path.lineTo(p01.x, p01.y);
                path.lineTo(p11.x, p11.y);
                path.lineTo(p10.x, p10.y);
                path.close();
                
                canvas.drawPath(path, paint);
            }
        }
    }

    public void startAnimation() {
        if (animator != null && !animator.isRunning()) {
            animator.start();
        }
    }

    public void stopAnimation() {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }
} 