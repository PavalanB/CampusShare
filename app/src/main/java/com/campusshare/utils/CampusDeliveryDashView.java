package com.campusshare.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Campus Delivery Dash - 5 Lane Edition
 * Tap left/right side of screen to change lanes!
 * Collect Gold Coins to earn credits!
 */
public class CampusDeliveryDashView extends View {

    private Paint paint;
    private Random random = new Random();

    // Game State
    private boolean gameStarted = false;
    private boolean gameOver = false;
    private int score = 0;
    private float speedMultiplier = 1.0f;
    private float roadOffset = 0;
    
    private static final float INITIAL_SPEED = 12.0f;
    private static final float MAX_SPEED = 30.0f;
    private static final float SPEED_INCREMENT = 0.2f;

    // Road & Lanes
    private float roadWidth;
    private float roadLeft;
    private float laneWidth;
    private int currentLane = 2; // 0 to 4

    // Player
    private RectF playerRect = new RectF();
    private float playerWidth = 80;
    private float playerHeight = 130;
    private float playerX, playerY;
    private float playerTargetX;

    // Entities
    private List<Entity> entities = new ArrayList<>();
    private List<Decor> decors = new ArrayList<>();
    private long lastEntitySpawnTime = 0;
    private long lastDecorSpawnTime = 0;

    // Visuals
    private int colorGrass = Color.parseColor("#2E7D32");
    private int colorRoad = Color.parseColor("#37474F");
    private int colorAnnaRed = Color.parseColor("#B71C1C");
    private int colorTree = Color.parseColor("#1B5E20");
    private int colorGold = Color.parseColor("#FFD700");

    public interface GameCallback {
        void onScoreUpdate(int score);
        void onGameOver(int finalScore);
    }
    private GameCallback callback;

    public CampusDeliveryDashView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setAntiAlias(true);
    }

    public void setCallback(GameCallback callback) {
        this.callback = callback;
    }

    private static class Entity {
        RectF rect;
        boolean isItem;
        int lane;
        Entity(float x, float y, float w, float h, boolean isItem, int lane) {
            this.rect = new RectF(x, y, x + w, y + h);
            this.isItem = isItem;
            this.lane = lane;
        }
    }

    private static class Decor {
        RectF rect;
        int type;
        Decor(float x, float y, float w, float h, int type) {
            this.rect = new RectF(x, y, x + w, y + h);
            this.type = type;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        roadWidth = w * 0.85f;
        roadLeft = (w - roadWidth) / 2f;
        laneWidth = roadWidth / 5f;
        resetGame();
    }

    public void resetGame() {
        currentLane = 2;
        playerTargetX = roadLeft + currentLane * laneWidth + (laneWidth - playerWidth) / 2f;
        playerX = playerTargetX;
        playerY = getHeight() - playerHeight - 200;
        playerRect.set(playerX, playerY, playerX + playerWidth, playerY + playerHeight);
        entities.clear();
        decors.clear();
        score = 0;
        speedMultiplier = 1.0f;
        roadOffset = 0;
        gameOver = false;
        gameStarted = false;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Grass
        canvas.drawColor(colorGrass);

        // 2. Decor
        for (Decor d : decors) {
            if (d.type == 0) drawTree(canvas, d.rect);
            else drawAnnaBuilding(canvas, d.rect);
        }

        // 3. Road
        paint.setColor(colorRoad);
        canvas.drawRect(roadLeft, 0, roadLeft + roadWidth, getHeight(), paint);
        
        // Lane Markers (4 dashed lines for 5 lanes)
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(4);
        float lineHeight = 80;
        float gap = 60;
        for (int i = 1; i < 5; i++) {
            float lx = roadLeft + i * laneWidth;
            for (float y = -lineHeight + (roadOffset % (lineHeight + gap)); y < getHeight(); y += (lineHeight + gap)) {
                canvas.drawLine(lx, y, lx, y + lineHeight, paint);
            }
        }

        // 4. Entities (Obstacles & Gold Coins)
        for (Entity e : entities) {
            if (e.isItem) drawGoldCoin(canvas, e.rect);
            else drawBarricadeObstacle(canvas, e.rect);
        }

        // 5. Player (Bike)
        smoothMovePlayer();
        drawBike(canvas, playerRect);

        // 6. UI
        paint.setColor(Color.WHITE);
        paint.setTextSize(60);
        paint.setFakeBoldText(true);
        canvas.drawText("Coins: " + score, 50, 100, paint);

        if (gameOver) {
            drawOverlay(canvas, "CRASHED!", "Collected " + score + " Coins\nTap to Restart");
        } else if (!gameStarted) {
            drawOverlay(canvas, "BIKE SIMULATION", "Tap Left/Right to Switch Lanes\nCollect Coins, Avoid Barricades!\n\nTap to Start");
        } else {
            update();
            invalidate();
        }
    }

    private void smoothMovePlayer() {
        playerTargetX = roadLeft + currentLane * laneWidth + (laneWidth - playerWidth) / 2f;
        float dx = playerTargetX - playerX;
        if (Math.abs(dx) > 1) {
            playerX += dx * 0.2f;
        } else {
            playerX = playerTargetX;
        }
        playerRect.offsetTo(playerX, playerY);
    }

    private void drawAnnaBuilding(Canvas canvas, RectF rect) {
        paint.setColor(colorAnnaRed);
        canvas.drawRect(rect, paint);
        paint.setColor(Color.WHITE);
        float winW = rect.width() / 4;
        float winH = rect.height() / 6;
        for (int i=0; i<3; i++) {
            for (int j=0; j<4; j++) {
                canvas.drawRect(rect.left + 10 + i*winW, rect.top + 20 + j*winH, 
                               rect.left + 10 + i*winW + 15, rect.top + 20 + j*winH + 20, paint);
            }
        }
    }

    private void drawTree(Canvas canvas, RectF rect) {
        paint.setColor(Color.parseColor("#5D4037"));
        canvas.drawRect(rect.centerX() - 8, rect.centerY(), rect.centerX() + 8, rect.bottom, paint);
        paint.setColor(colorTree);
        canvas.drawCircle(rect.centerX(), rect.top + rect.height()/3, rect.width()/2, paint);
    }

    private void drawGoldCoin(Canvas canvas, RectF rect) {
        paint.setColor(colorGold);
        canvas.drawOval(rect, paint);
        paint.setColor(Color.parseColor("#FFA000")); // Darker gold for inner circle
        canvas.drawOval(rect.left + 10, rect.top + 10, rect.right - 10, rect.bottom - 10, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(35);
        paint.setFakeBoldText(true);
        canvas.drawText("$", rect.centerX() - 10, rect.centerY() + 12, paint);
    }

    private void drawBarricadeObstacle(Canvas canvas, RectF rect) {
        paint.setColor(Color.YELLOW);
        canvas.drawRect(rect.left, rect.centerY() - 15, rect.right, rect.centerY() + 15, paint);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(8);
        for (float x = rect.left; x < rect.right; x += 30) {
            canvas.drawLine(x, rect.centerY() - 15, x + 15, rect.centerY() + 15, paint);
        }
    }

    private void drawBike(Canvas canvas, RectF rect) {
        // Body
        paint.setColor(Color.parseColor("#1E88E5")); // Changed bike color to blue
        canvas.drawRoundRect(rect.centerX() - 18, rect.top + 10, rect.centerX() + 18, rect.bottom - 10, 15, 15, paint);
        
        // Wheels
        paint.setColor(Color.BLACK);
        canvas.drawOval(rect.centerX() - 12, rect.top, rect.centerX() + 12, rect.top + 40, paint);
        canvas.drawOval(rect.centerX() - 12, rect.bottom - 40, rect.centerX() + 12, rect.bottom, paint);
        
        // Handlebars
        paint.setColor(Color.LTGRAY);
        canvas.drawRect(rect.centerX() - 45, rect.top + 35, rect.centerX() + 45, rect.top + 45, paint);
        
        // Rider
        paint.setColor(Color.WHITE);
        canvas.drawCircle(rect.centerX(), rect.centerY(), 25, paint);
    }

    private void drawOverlay(Canvas canvas, String title, String sub) {
        paint.setColor(Color.argb(180, 0, 0, 0));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(90);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(title, getWidth() / 2f, getHeight() / 2f - 80, paint);
        
        paint.setTextSize(40);
        String[] lines = sub.split("\n");
        float y = getHeight() / 2f + 20;
        for (String line : lines) {
            canvas.drawText(line, getWidth() / 2f, y, paint);
            y += 60;
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void update() {
        float moveSpeed = INITIAL_SPEED * speedMultiplier;
        roadOffset += moveSpeed;

        long now = System.currentTimeMillis();
        if (now - lastEntitySpawnTime > 1500 / speedMultiplier) {
            spawnEntity();
            lastEntitySpawnTime = now;
        }
        if (now - lastDecorSpawnTime > 1000 / speedMultiplier) {
            spawnDecor();
            lastDecorSpawnTime = now;
        }

        for (int i = decors.size() - 1; i >= 0; i--) {
            Decor d = decors.get(i);
            d.rect.offset(0, moveSpeed);
            if (d.rect.top > getHeight()) decors.remove(i);
        }

        for (int i = entities.size() - 1; i >= 0; i--) {
            Entity e = entities.get(i);
            e.rect.offset(0, moveSpeed);

            if (RectF.intersects(playerRect, e.rect)) {
                if (e.isItem) {
                    score++;
                    if (callback != null) callback.onScoreUpdate(score);
                    speedMultiplier += 0.02f;
                    entities.remove(i);
                } else {
                    gameOver = true;
                    if (callback != null) callback.onGameOver(score);
                }
                continue;
            }
            if (e.rect.top > getHeight()) entities.remove(i);
        }
    }

    private void spawnEntity() {
        int lane = random.nextInt(5);
        float x = roadLeft + lane * laneWidth + (laneWidth - 70) / 2f;
        boolean isItem = random.nextFloat() > 0.4f;
        entities.add(new Entity(x, -150, 70, 70, isItem, lane));
    }

    private void spawnDecor() {
        boolean isLeft = random.nextBoolean();
        float x = isLeft ? random.nextFloat() * (roadLeft - 150) : roadLeft + roadWidth + random.nextFloat() * (getWidth() - roadLeft - roadWidth - 150);
        int type = random.nextInt(2);
        decors.add(new Decor(x, -300, 150, type == 0 ? 150 : 250, type));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            performClick();
            if (gameOver) resetGame();
            else if (!gameStarted) gameStarted = true;
            else {
                if (event.getX() < getWidth() / 2f) {
                    if (currentLane > 0) currentLane--;
                } else {
                    if (currentLane < 4) currentLane++;
                }
            }
            invalidate();
        }
        return true;
    }

    @Override
    public boolean performClick() { return super.performClick(); }
}
