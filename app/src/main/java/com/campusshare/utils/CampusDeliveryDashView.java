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
 * Campus Delivery Dash - Anna University Edition
 * Featuring landmarks like the Main Building (Red Building) and trees.
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
    
    private static final float INITIAL_SPEED = 1.0f;
    private static final float MAX_SPEED = 2.4f;
    private static final float SPEED_INCREMENT = 0.004f;

    // Player
    private RectF playerRect = new RectF();
    private float playerWidth = 90;
    private float playerHeight = 150;
    private float playerX, playerY;
    private float playerTilt = 0;

    // Entities
    private List<Entity> entities = new ArrayList<>();
    private List<Decor> decors = new ArrayList<>();
    private long lastEntitySpawnTime = 0;
    private long lastDecorSpawnTime = 0;

    // Anna University specific visuals
    private int colorGrass = Color.parseColor("#689F38");
    private int colorRoad = Color.parseColor("#37474F");
    private int colorAnnaRed = Color.parseColor("#B71C1C"); // Iconic Red Building color
    private int colorTree = Color.parseColor("#2E7D32");

    public CampusDeliveryDashView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setAntiAlias(true);
    }

    private static class Entity {
        RectF rect;
        boolean isItem;
        int type; // 0: Box/Book, 1: Laptop/Envelope
        Entity(float x, float y, float w, float h, boolean isItem, int type) {
            this.rect = new RectF(x, y, x + w, y + h);
            this.isItem = isItem;
            this.type = type;
        }
    }

    private static class Decor {
        RectF rect;
        int type; // 0: Tree, 1: Building (Anna Uni Red Building)
        float side; // Negative for left, Positive for right
        Decor(float x, float y, float w, float h, int type, float side) {
            this.rect = new RectF(x, y, x + w, y + h);
            this.type = type;
            this.side = side;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        resetGame();
    }

    private void resetGame() {
        playerX = getWidth() / 2f - playerWidth / 2f;
        playerY = getHeight() - playerHeight - 180;
        playerRect.set(playerX, playerY, playerX + playerWidth, playerY + playerHeight);
        entities.clear();
        decors.clear();
        score = 0;
        speedMultiplier = INITIAL_SPEED;
        roadOffset = 0;
        gameOver = false;
        gameStarted = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float roadWidth = getWidth() * 0.6f;
        float roadLeft = (getWidth() - roadWidth) / 2f;

        // 1. Grass & Side Areas
        canvas.drawColor(colorGrass);

        // 2. Draw Decor (Buildings & Trees)
        for (Decor d : decors) {
            if (d.type == 0) drawTree(canvas, d.rect);
            else drawAnnaBuilding(canvas, d.rect);
        }

        // 3. Road
        paint.setColor(colorRoad);
        canvas.drawRect(roadLeft, 0, roadLeft + roadWidth, getHeight(), paint);
        
        // Road Markers
        paint.setColor(Color.WHITE);
        float lineHeight = 100;
        float gap = 100;
        for (float y = -lineHeight + (roadOffset % (lineHeight + gap)); y < getHeight(); y += (lineHeight + gap)) {
            canvas.drawRect(getWidth() / 2f - 6, y, getWidth() / 2f + 6, y + lineHeight, paint);
        }

        // 4. Entities
        for (Entity e : entities) {
            if (e.isItem) {
                if (e.type == 0) drawBookItem(canvas, e.rect);
                else drawLaptopItem(canvas, e.rect);
            } else {
                if (e.type == 0) drawPotholeObstacle(canvas, e.rect);
                else drawBarricadeObstacle(canvas, e.rect);
            }
        }

        // 5. Player
        drawBike(canvas, playerRect, playerTilt);

        // 6. UI
        paint.setColor(Color.WHITE);
        paint.setTextSize(55);
        paint.setFakeBoldText(true);
        canvas.drawText("Score: " + score, 40, 80, paint);

        if (gameOver) {
            drawOverlay(canvas, "WASTED", "Anna Uni Traffic is Tough!\nScore: " + score + "\nTap to Respawn");
        } else if (!gameStarted) {
            drawOverlay(canvas, "CAMPUS DASH", "Anna University Edition\nSwipe to Deliver items!\n\nTap to Start");
        } else {
            update();
            invalidate();
        }
    }

    private void drawAnnaBuilding(Canvas canvas, RectF rect) {
        paint.setColor(colorAnnaRed);
        canvas.drawRect(rect, paint);
        paint.setColor(Color.parseColor("#880E4F"));
        Path roof = new Path();
        roof.moveTo(rect.left - 10, rect.top);
        roof.lineTo(rect.right + 10, rect.top);
        roof.lineTo(rect.centerX(), rect.top - 40);
        roof.close();
        canvas.drawPath(roof, paint);
        paint.setColor(Color.WHITE);
        float winW = rect.width() / 4;
        float winH = rect.height() / 5;
        for (int i=0; i<3; i++) {
            for (int j=0; j<3; j++) {
                canvas.drawRect(rect.left + 10 + i*winW, rect.top + 20 + j*winH, 
                               rect.left + 10 + i*winW + 15, rect.top + 20 + j*winH + 20, paint);
            }
        }
    }

    private void drawTree(Canvas canvas, RectF rect) {
        paint.setColor(Color.parseColor("#5D4037"));
        canvas.drawRect(rect.centerX() - 10, rect.centerY(), rect.centerX() + 10, rect.bottom, paint);
        paint.setColor(colorTree);
        canvas.drawCircle(rect.centerX(), rect.top + rect.height()/3, rect.width()/2, paint);
    }

    private void drawBookItem(Canvas canvas, RectF rect) {
        paint.setColor(Color.WHITE);
        canvas.drawRect(rect, paint);
        paint.setColor(Color.BLUE);
        canvas.drawRect(rect.left, rect.top, rect.left + 10, rect.bottom, paint);
        paint.setTextSize(20);
        canvas.drawText("ENG", rect.left + 15, rect.centerY(), paint);
    }

    private void drawLaptopItem(Canvas canvas, RectF rect) {
        paint.setColor(Color.parseColor("#424242"));
        canvas.drawRoundRect(rect, 5, 5, paint);
        paint.setColor(Color.CYAN);
        canvas.drawRect(rect.left + 10, rect.top + 10, rect.right - 10, rect.centerY(), paint);
    }

    private void drawPotholeObstacle(Canvas canvas, RectF rect) {
        paint.setColor(Color.BLACK);
        canvas.drawOval(rect, paint);
        paint.setColor(Color.parseColor("#212121"));
        canvas.drawOval(rect.left + 5, rect.top + 5, rect.right - 5, rect.bottom - 5, paint);
    }

    private void drawBarricadeObstacle(Canvas canvas, RectF rect) {
        paint.setColor(Color.YELLOW);
        canvas.drawRect(rect.left, rect.centerY() - 10, rect.right, rect.centerY() + 10, paint);
        paint.setColor(Color.BLACK);
        for (int i=0; i<4; i++) {
            canvas.drawRect(rect.left + i*20, rect.centerY() - 10, rect.left + i*20 + 10, rect.centerY() + 10, paint);
        }
    }

    private void drawBike(Canvas canvas, RectF rect, float tilt) {
        canvas.save();
        canvas.rotate(tilt, rect.centerX(), rect.bottom);
        
        // Bike Body
        paint.setColor(Color.BLUE);
        canvas.drawRoundRect(rect.centerX() - 15, rect.top + 20, rect.centerX() + 15, rect.bottom - 20, 10, 10, paint);
        
        // Front Wheel
        paint.setColor(Color.BLACK);
        canvas.drawRoundRect(rect.centerX() - 10, rect.top, rect.centerX() + 10, rect.top + 40, 5, 5, paint);
        
        // Back Wheel
        canvas.drawRoundRect(rect.centerX() - 10, rect.bottom - 40, rect.centerX() + 10, rect.bottom, 5, 5, paint);
        
        // Handlebars
        paint.setColor(Color.GRAY);
        canvas.drawRect(rect.centerX() - 40, rect.top + 35, rect.centerX() + 40, rect.top + 45, paint);
        paint.setColor(Color.BLACK);
        canvas.drawCircle(rect.centerX() - 40, rect.top + 40, 8, paint);
        canvas.drawCircle(rect.centerX() + 40, rect.top + 40, 8, paint);
        
        // Seat
        paint.setColor(Color.BLACK);
        canvas.drawRoundRect(rect.centerX() - 12, rect.centerY() - 10, rect.centerX() + 12, rect.centerY() + 25, 8, 8, paint);
        
        // Rider (Helmet)
        paint.setColor(Color.DKGRAY);
        canvas.drawCircle(rect.centerX(), rect.centerY() - 5, 22, paint);
        paint.setColor(Color.BLACK);
        canvas.drawArc(rect.centerX() - 22, rect.centerY() - 27, rect.centerX() + 22, rect.centerY() + 17, 200, 140, true, paint);

        canvas.restore();
    }

    private void drawOverlay(Canvas canvas, String title, String sub) {
        paint.setColor(Color.argb(200, 0, 0, 0));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(100);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(title, getWidth() / 2f, getHeight() / 2f - 60, paint);
        
        paint.setTextSize(45);
        String[] lines = sub.split("\n");
        float y = getHeight() / 2f + 40;
        for (String line : lines) {
            canvas.drawText(line, getWidth() / 2f, y, paint);
            y += 60;
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void update() {
        float moveSpeed = 20 * speedMultiplier;
        roadOffset += moveSpeed;

        long now = System.currentTimeMillis();
        if (now - lastEntitySpawnTime > 1300 / speedMultiplier) {
            spawnEntity();
            lastEntitySpawnTime = now;
        }
        if (now - lastDecorSpawnTime > 800 / speedMultiplier) {
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

            RectF hitBox = new RectF(e.rect);
            hitBox.inset(15, 15);
            if (RectF.intersects(playerRect, hitBox)) {
                if (e.isItem) {
                    score += 10;
                    if (speedMultiplier < MAX_SPEED) speedMultiplier += SPEED_INCREMENT;
                    entities.remove(i);
                } else {
                    gameOver = true;
                }
                continue;
            }
            if (e.rect.top > getHeight()) entities.remove(i);
        }
        playerTilt *= 0.85f;
    }

    private void spawnEntity() {
        float roadWidth = getWidth() * 0.6f;
        float roadLeft = (getWidth() - roadWidth) / 2f;
        float x = roadLeft + random.nextFloat() * (roadWidth - 80);
        entities.add(new Entity(x, -100, 80, 80, random.nextFloat() > 0.5f, random.nextInt(2)));
    }

    private void spawnDecor() {
        boolean left = random.nextBoolean();
        float roadWidth = getWidth() * 0.6f;
        float x = left ? random.nextFloat() * (getWidth()-roadWidth)/2 - 100 : getWidth() - (getWidth()-roadWidth)/2 + random.nextFloat()*100;
        int type = random.nextInt(2);
        float w = type == 0 ? 80 : 150;
        float h = type == 0 ? 80 : 200;
        decors.add(new Decor(x, -250, w, h, type, left ? -1 : 1));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            performClick();
            if (gameOver) resetGame();
            else if (!gameStarted) gameStarted = true;
        }
        if (gameStarted && !gameOver && event.getAction() == MotionEvent.ACTION_MOVE) {
            float oldX = playerX;
            playerX = event.getX() - playerWidth / 2;
            playerY = event.getY() - playerHeight / 2;
            
            float roadWidth = getWidth() * 0.6f;
            float roadLeft = (getWidth() - roadWidth) / 2f;
            if (playerX < roadLeft) playerX = roadLeft;
            if (playerX > roadLeft + roadWidth - playerWidth) playerX = roadLeft + roadWidth - playerWidth;
            if (playerY < 50) playerY = 50;
            if (playerY > getHeight() - playerHeight - 50) playerY = getHeight() - playerHeight - 50;
            
            playerTilt = (playerX - oldX) * 2.5f;
            playerRect.offsetTo(playerX, playerY);
        }
        invalidate();
        return true;
    }

    @Override
    public boolean performClick() { return super.performClick(); }
}
