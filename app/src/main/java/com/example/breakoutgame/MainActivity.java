package com.example.breakoutgame;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.content.Context;
import java.io.IOException;
public class MainActivity extends Activity {
    BreakoutView breakoutView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        breakoutView = new BreakoutView(this);
        setContentView(breakoutView);
    }
    class BreakoutView extends SurfaceView implements Runnable{

        Thread gameThread = null;
        SurfaceHolder ourHolder;
        volatile boolean playing;
        boolean paused = true;

        Canvas canvas;
        Paint paint;
        float x;
        float y;
        long fps;
        private long timeThisFrame;
        int screenX;
        int screenY;


        Paddle paddle;
        Ball ball;

        Brick[] bricks = new Brick[200];
        int numBricks = 0;

        SoundPool soundPool;
        int beep1ID = -1;
        int beep2ID = -1;
        int beep3ID = -1;
        int loseLifeID = -1;
        int explodeID = -1;
        // The score
        int score = 0;
        // Lives
        int lives = 3;


        public BreakoutView (Context context){
            super(context);
            ourHolder = getHolder();
            paint = new Paint();

            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenX = size.x;
            screenY = size.y;
            paddle = new Paddle(screenX, screenY);
            ball = new Ball(screenX, screenY-30);
            this.soundPool = new SoundPool(10,3, 0);
            try {
                AssetManager assetManager = context.getAssets();

                this.beep1ID = soundPool.load(assetManager.openFd("beep1.ogg"), 0);
                this.beep2ID = soundPool.load(assetManager.openFd("beep2.ogg"), 0);
                this.beep3ID = soundPool.load(assetManager.openFd("beep3.ogg"), 0);
                this.loseLifeID = soundPool.load(assetManager.openFd("loseLife.ogg"), 0);
                this.explodeID = this.soundPool.load(assetManager.openFd("explode.ogg"), 0);
                Log.e("error", "failed to load sound files");
            } catch (IOException e) {
                e.printStackTrace();
            }
            createBricksAndRestart();

        }

        public void createBricksAndRestart() {
// Put the ball back to the start
            ball.reset(screenX, screenY);
            paddle.finalize();
            paddle = new Paddle(screenX, screenY);
            int brickWidth = screenX / 8;
            int brickHeight = screenY / 10;
            numBricks = 0;
            for (int column = 0; column < 8; column++) {
                for (int row = 0; row < 3; row++) {
                    bricks[numBricks] = new Brick(row, column, brickWidth, brickHeight);
                    numBricks++;
                }

            }
// if game over reset scores and lives
            if (lives == 0) {
                score = 0;
                lives = 3;
            }
            else if(score == 100){
                score = 0;
                lives = 3;
            }
        }

        @Override
        public void run(){
            while(playing){
                long startFrameTime = System.currentTimeMillis();
                if(!paused){
                    update();
                }
                draw();
                timeThisFrame = System.currentTimeMillis() - startFrameTime;
                if(timeThisFrame >= 1){
                    fps = 1000/timeThisFrame;
                }
            }
        }
        public void update(){
            paddle.update(fps);
            ball.update(fps);
// Check for ball colliding with a brick
            for (int i = 0; i < numBricks; i++) {
                if (bricks[i].getVisibility()) {
                    if (RectF.intersects(bricks[i].getRect(), ball.getRect())) {
                        bricks[i].setInvisible();
                        ball.reverseYVelocity();
                        score = score + 10;
                        soundPool.play(explodeID, 1, 1, 0, 0, 1);
                    }
                }

            }
// Check for ball colliding with paddle
            if (RectF.intersects(paddle.getRect(), ball.getRect())) {
                ball.setRandomXVelocity();
                ball.reverseYVelocity();
                ball.clearObstacleY(paddle.getRect().top - 2);
                soundPool.play(beep1ID, 1, 1, 0, 0, 1);
            }
// Bounce the ball back when it hits the bottom of screen
            if (ball.getRect().bottom > screenY) {
                ball.reverseYVelocity();
                ball.clearObstacleY(screenY - 2);
// Lose a life
                lives--;
                soundPool.play(loseLifeID, 1, 1, 0, 0, 1);
                if (lives == 0) {
                    paused = true;
                    //createBricksAndRestart();

                }
            }
// Bounce the ball back when it hits the top of screen
            if (ball.getRect().top < 0)
            {
                ball.reverseYVelocity();}
// If the ball hits left wall bounce
            if (ball.getRect().left < 0)
            {
                ball.reverseXVelocity();
                ball.clearObstacleX(2);
                soundPool.play(beep3ID, 1, 1, 0, 0, 1);
            }
// If the ball hits right wall bounce
            if (ball.getRect().right > screenX - 10) {
                ball.reverseXVelocity();
                ball.clearObstacleX(screenX - 22);
                soundPool.play(beep3ID, 1, 1, 0, 0, 1);
            }
// Pause if cleared screen
            if (score == 100)
            {
                paused = true;


            }
        }

        public void draw() {
// Make sure our drawing surface is valid or we crash
            if (ourHolder.getSurface().isValid()) {
// Lock the canvas ready to draw
                canvas = ourHolder.lockCanvas();
// Draw the background color
                canvas.drawColor(Color.argb(255, 26, 128, 182));
// Choose the brush color for drawing
                paint.setColor(Color.argb(255, 255, 255, 255));
// Draw the paddle
                canvas.drawRect(paddle.getRect(), paint);
// Draw the ball
                paint.setColor(Color.argb(255, 215, 255, 255));
                canvas.drawRect(ball.getRect(), paint);
// Change the brush color for drawing
                paint.setColor(Color.rgb(255, 0, 255));
// Draw the bricks if visible
                for (int i = 0; i < numBricks; i++) {
                    if (bricks[i].getVisibility()) {
                        canvas.drawRect(bricks[i].getRect(), paint);
                    }
                }
// Choose the brush color for drawing
                paint.setColor(Color.argb(255, 255, 255, 255));
// Draw the score
                paint.setTextSize(40);
                canvas.drawText("Score: " + score + " Lives: " + lives, 10, 50, paint);
// Has the player cleared the screen?
                if (score ==  100) {
                    paint.setTextSize(90);
                    canvas.drawText("YOU HAVE WON!", 10, screenY / 2, paint);
                }
// Has the player lost?
                if (lives == 0) {
                    paint.setTextSize(90);
                    canvas.drawText("YOU HAVE LOST!", 10, screenY / 2, paint);

                }
// Draw everything to the screen
                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        public void pause() {
            playing = false;
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Log.e("Error:", "joining thread");
            }
        }
        public void resume() {
            playing = true;
            gameThread = new Thread(this);
            gameThread.start();
        }


        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                // Player has touched the screen
                case MotionEvent.ACTION_DOWN:
                    if(paused) {
                        paused = false;
                        createBricksAndRestart();
                    }
                    if (motionEvent.getX() > screenX / 2) {
                        paddle.setMovementState(paddle.RIGHT);

                    }

                    else
                    {
                        paddle.setMovementState(paddle.LEFT);
                    }

                    break;

                // Player has removed finger from screen
                case MotionEvent.ACTION_UP:

                    paddle.setMovementState(paddle.STOPPED);
                    break;
            }

            return true;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        breakoutView.resume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        breakoutView.pause();
    }
}




