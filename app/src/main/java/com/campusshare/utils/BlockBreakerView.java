package com.campusshare.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class BlockBreakerView extends View {

    private Paint paint;
    private float paddleWidth = 200;
    private float paddleHeight = 40;
    private float paddleX;
    private float paddleY;

    private float ballX, ballY;
    private float ballRadius = 20;
    private float ballSpeedX = 10;
    private float ballSpeedY = -10;

    private int rows = 5;
    private int cols = 6;
    private List<RectF> blocks;
    
    private boolean gameStarted = false;
    private boolean gameOver = false;
    private int score = 0;

    public BlockBreakerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setAntiAlias(true);
        blocks = new ArrayList<>();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        resetGame();
    }

    private void resetGame() {
        paddleX = getWidth() / 2 - paddleWidth / 2;
        paddleY = getHeight() - 200;
        ballX = getWidth() / 2;
        ballY = paddleY - ballRadius - 5;
        ballSpeedX = 12;
        ballSpeedY = -12;
        score = 0;
        gameOver = false;
        gameStarted = false;

        blocks.clear();
        float blockWidth = (float) getWidth() / cols;
        float blockHeight = 60;
        float topOffset = 200; // Added more space for score
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                blocks.add(new RectF(j * blockWidth + 5, i * blockHeight + topOffset, (j + 1) * blockWidth - 5, (i + 1) * blockHeight + topOffset - 5));
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Background
        canvas.drawColor(Color.parseColor("#121212"));

        // Paddle
        paint.setColor(Color.parseColor("#1A237E"));
        canvas.drawRoundRect(paddleX, paddleY, paddleX + paddleWidth, paddleY + paddleHeight, 20, 20, paint);

        // Ball
        paint.setColor(Color.parseColor("#FFD600"));
        canvas.drawCircle(ballX, ballY, ballRadius, paint);

        // Blocks
        for (RectF block : blocks) {
            paint.setColor(Color.parseColor("#D32F2F"));
            canvas.drawRoundRect(block, 10, 10, paint);
        }

        // Score
        paint.setColor(Color.WHITE);
        paint.setTextSize(50);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Score: " + score, 70, 100, paint);

        if (gameOver) {
            paint.setTextSize(80);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("GAME OVER", getWidth() / 2, getHeight() / 2, paint);
            paint.setTextSize(40);
            canvas.drawText("Tap to Restart", getWidth() / 2, getHeight() / 2 + 80, paint);
        } else if (!gameStarted) {
            paint.setTextSize(60);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Tap to Start", getWidth() / 2, getHeight() / 2, paint);
        } else {
            update();
            invalidate();
        }
    }

    private void update() {
        ballX += ballSpeedX;
        ballY += ballSpeedY;

        // Wall collisions
        if (ballX - ballRadius < 0 || ballX + ballRadius > getWidth()) {
            ballSpeedX = -ballSpeedX;
        }
        if (ballY - ballRadius < 0) {
            ballSpeedY = -Math.abs(ballSpeedY);
            ballSpeedY = -ballSpeedY;
        }

        // Paddle collision
        if (ballY + ballRadius >= paddleY && ballY + ballRadius <= paddleY + paddleHeight) {
            if (ballX >= paddleX && ballX <= paddleX + paddleWidth) {
                ballSpeedY = -Math.abs(ballSpeedY);
                float hitPos = (ballX - (paddleX + paddleWidth / 2)) / (paddleWidth / 2);
                ballSpeedX = hitPos * 15;
            }
        }

        // Block collisions
        for (int i = 0; i < blocks.size(); i++) {
            if (RectF.intersects(blocks.get(i), new RectF(ballX - ballRadius, ballY - ballRadius, ballX + ballRadius, ballY + ballRadius))) {
                blocks.remove(i);
                ballSpeedY = -ballSpeedY;
                score += 10;
                break;
            }
        }

        // Fall out
        if (ballY > getHeight()) {
            gameOver = true;
        }

        // Win condition
        if (blocks.isEmpty()) {
            gameOver = true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                performClick();
                if (gameOver) {
                    resetGame();
                } else if (!gameStarted) {
                    gameStarted = true;
                }
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (gameStarted && !gameOver) {
                    paddleX = x - paddleWidth / 2;
                    if (paddleX < 0) paddleX = 0;
                    if (paddleX + paddleWidth > getWidth()) paddleX = getWidth() - paddleWidth;
                }
                invalidate();
                break;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
