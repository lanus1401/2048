package com.raduetsya.game2048;

import java.util.ArrayList;
import java.util.List;

import com.raduetsya.game2048.GameGrid.DIRECTION;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Align;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by raduetsya on 3/14/14.
 */


public class GameView extends View {

    RectF mainRect;
    Paint mainRectPaint = new Paint();
    int mainRectColor;
    int mainRectRoundAngles;

    Paint resultPaint = new Paint();
    Rect resultRect = new Rect();

    int backgroundColor;
    int gridRectColor;
    RectF gridTempRect = new RectF();
    int gridMargins;
    int gridSpacing;
    RectF gridLeftTopRect = new RectF();
    Paint gridPaint = new Paint();

    private static final int MINIMAL_RANGE = 30;
    Point fancyGridOffset = new Point();
    DIRECTION lastDir = DIRECTION.NONE;
    double FPS = 1/30.;

    Point gestureCenter = new Point();
    public Point gestOffset = new Point();
    int offsetSize = 0;

    public List<GameGrid.Action> animList = null;
    Handler animHandler = new Handler();
    int animStartTime = (int) System.currentTimeMillis();
    Point animLastOffset = new Point();
    final static int ANIM_DURATION = 150;
    final static int ANIM_CREATE_DELAY = 80;
    final static int ANIM_CREATE_DURATION = 200;
    final static int ANIM_FRAMEDELAY = 33; // 1000/30

    int animFadeGameover = 0;

    boolean animStop = false;
    Runnable animationInvalidator = new Runnable() {
        @Override
        public void run() {
            invalidate();
            if ( !(animStop == true && animList == null) )
                animHandler.postDelayed(this, ANIM_FRAMEDELAY);
        }
    };

    GameGrid model;
    TileView tileView;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.com_raduetsya_game2048_GameView,
                0, 0);

        try {
            backgroundColor = Color.parseColor( a.getString(R.styleable.com_raduetsya_game2048_GameView_backgroundColor ));

            mainRectColor = Color.parseColor( a.getString(R.styleable.com_raduetsya_game2048_GameView_foregroundColor) );
            mainRectRoundAngles = a.getInteger(R.styleable.com_raduetsya_game2048_GameView_roundAngles, 0);

            gridRectColor = Color.parseColor( a.getString(R.styleable.com_raduetsya_game2048_GameView_gridColor) );
            gridSpacing = a.getInteger(R.styleable.com_raduetsya_game2048_GameView_gridSpacing, 0);
            gridMargins = a.getInteger(R.styleable.com_raduetsya_game2048_GameView_gridMargins, 1);

            tileView = new DefaultTileView(context, a.getInteger(R.styleable.com_raduetsya_game2048_GameView_roundTileAngles, 0));
        } finally {
            a.recycle();
        }

        mainRectPaint = new Paint();
        mainRectPaint.setColor(mainRectColor);
        mainRectPaint.setAntiAlias(true);

        gridPaint = new Paint();
        gridPaint.setColor( gridRectColor );
        gridPaint.setAntiAlias(true);
    }

    public void setModel(GameGrid model) {
        this.model = model;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (model == null) return;

        if (w <= h)
            mainRect = new RectF(0, h/2-w/2, w, h/2+w/2);
        else
            mainRect = new RectF(w/2-h/2, 0, w/2+h/2, h);

        int gridRectSizeX = (int)((mainRect.width() - gridMargins*2 - (model.sizeX-1)*gridSpacing)/model.sizeX);
        int gridRectSizeY = (int)((mainRect.height() - gridMargins*2 - (model.sizeY-1)*gridSpacing)/model.sizeY);

        gridLeftTopRect.set(
                mainRect.left + gridMargins,
                mainRect.top + gridMargins,
                mainRect.left + gridMargins + gridRectSizeX,
                mainRect.top + gridMargins + gridRectSizeY
        );
    }

    boolean anim(Canvas canvas) {

        double time = ((int) System.currentTimeMillis() - animStartTime)/(double)(ANIM_DURATION);
        double createTime = ((int) System.currentTimeMillis() - animStartTime - ANIM_CREATE_DELAY)/
                (double)(ANIM_CREATE_DELAY + ANIM_CREATE_DURATION);
        if (createTime < 0.0) createTime = 0.0;
        if (createTime > 1.0) {
            animList = null;
            return false;
        }
        if (time > 1.0) time = 1.0;
        time = Math.pow(time, 0.33);

        for (int zIndex=0; zIndex<3; zIndex++) {
            for (GameGrid.Action iter : animList) {

                double posX = iter.oldX + (iter.newX - iter.oldX) * time;
                double posY = iter.oldY + (iter.newY - iter.oldY) * time;
                gridTempRect.offsetTo( (float)(gridLeftTopRect.left + posX*(gridLeftTopRect.width()  + gridSpacing)),
                        (float)(gridLeftTopRect.top +  posY*(gridLeftTopRect.height() + gridSpacing)));
                if ( (iter.oldX != iter.newX) || (iter.oldY != iter.newY)) {
                    gridTempRect.offset( (float)(animLastOffset.x*(1-time)), (float)(animLastOffset.y*(1-time)) );
                }

                // draw
                if (iter.type == GameGrid.Action.MOVE && zIndex == 0) {
                    tileView.draw(canvas, gridTempRect, iter.rang, 0.0f);
                }
                else if (iter.type == GameGrid.Action.MERGE && zIndex == 1) {
                    tileView.draw(canvas, gridTempRect, iter.rang+1, (float)(1-time));
                }
                else if (iter.type == GameGrid.Action.CREATE && zIndex == 2) {
                    if (createTime != 0)
                        tileView.draw(canvas, gridTempRect, iter.rang, (float)createTime);
                }
            }
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (model == null) return;

        canvas.drawColor(backgroundColor);

        //mainRectPaint.setColor(Color.rgb(model.rand.nextInt(255), model.rand.nextInt(255), model.rand.nextInt(255)));
        canvas.drawRoundRect( mainRect,
                mainRectRoundAngles, mainRectRoundAngles,
                mainRectPaint);

        gridTempRect.set(gridLeftTopRect);

        updateFancyGridOffset();

        for(int zIndex = 0; zIndex < 3; zIndex++) {
            // zIndex = 0: grid background
            // zIndex = 1: stand still tiles
            // zIndex = 2: moving tiles

            if (zIndex > 0 && animList != null) {
                if (anim(canvas))
                    break;
            }

            for (int i=0; i<model.sizeX; i++) {
                for (int j=0; j<model.sizeY; j++) {


                    gridTempRect.offsetTo(
                            (float)(gridLeftTopRect.left + i*(gridLeftTopRect.width()  + gridSpacing)),
                            (float)(gridLeftTopRect.top +  j*(gridLeftTopRect.height() + gridSpacing)));

                    if (zIndex == 0) {
                        canvas.drawRoundRect( gridTempRect,
                                mainRectRoundAngles, mainRectRoundAngles,
                                gridPaint);
                    }

                    if (lastDir != DIRECTION.NONE) {
                        if (zIndex == 1 && !model.isAbleToMove(i, j, lastDir )) {
                            tileView.draw(canvas, gridTempRect, model.get(i,j), 0.0f);
                        }

                        if (zIndex == 2 && model.isAbleToMove(i, j, lastDir)) {
                            gridTempRect.offset( fancyGridOffset.x, fancyGridOffset.y );
                            tileView.draw(canvas, gridTempRect, model.get(i,j), 0.0f);
                        }
                    } else {
                        tileView.draw(canvas, gridTempRect, model.get(i,j), 0.0f);
                    }

                } // for j
            } // for i
        } // for zIndex

        // TODO: make this function better:
        // win/lose check should be in GameGrid
        // fontSize should be calculated in onSizeChanged
        // make text color change support in colors.xml
        // maybe move text in another View
        // separate functions for each draw section

        if (model.getGameState() != GameGrid.GAMESTATE.PLAY) {
            resultPaint.setColor(backgroundColor);
            if (animFadeGameover > 240) {
                animFadeGameover = 240;
                animStop = true;
            } else {
                animFadeGameover += 4; // 1000ms / 240
            }

            if (animFadeGameover > 10) {
                resultPaint.setAlpha(animFadeGameover);
                canvas.drawRect(mainRect, resultPaint);

                resultPaint.setColor(Color.WHITE); // TODO: make color
                resultPaint.setAlpha(animFadeGameover);
                resultPaint.setTextAlign(Align.CENTER);

                resultPaint.setTextSize(30); // TODO: make pixel-independent
                resultPaint.getTextBounds("A", 0, 1, resultRect);
                int fontHeight = (int)(resultRect.height()*1.5);

                if (model.getGameState() == GameGrid.GAMESTATE.GAMEOVER)
                    canvas.drawText("GAME OVER", mainRect.centerX(), mainRect.centerY()+fontHeight*-1, resultPaint);
                else
                    canvas.drawText("2048! YOU WIN!", mainRect.centerX(), mainRect.centerY()+fontHeight*-1, resultPaint);

                canvas.drawText("Score: " + model.getScore(), mainRect.centerX(), mainRect.centerY()+fontHeight*1, resultPaint);
                if (model.getScore() > model.getHighscore()) {
                    canvas.drawText("New record!", mainRect.centerX(), mainRect.centerY()+fontHeight*2, resultPaint);
                    canvas.drawText("Last record: " + model.getHighscore(), mainRect.centerX(), mainRect.centerY()+fontHeight*3, resultPaint);
                } else {
                    canvas.drawText("Highscore: " + model.getHighscore(), mainRect.centerX(), mainRect.centerY()+fontHeight*2, resultPaint);
                }
            }
        } else {
            animFadeGameover = 0;
        }

        resultPaint.setColor(Color.WHITE);
        resultPaint.setTextAlign(Align.LEFT);
        resultPaint.setTextSize(30); // TODO: make pixel-independent
        resultPaint.getTextBounds("A", 0, 1, resultRect);
        int fontHeight = (int)(resultRect.height()*1.5);
        canvas.drawText("Score: " + model.getScore(), 30, 30+fontHeight*1, resultPaint);
        canvas.drawText("Highscore: " + model.getHighscore(), 30, 30+fontHeight*2, resultPaint);

        // pretty ugly function. such draw wow ugly
    }

    public void startAnim(List<GameGrid.Action> animList) {
        this.animList = animList;
        animStartTime = (int) System.currentTimeMillis();
        animStop = false;
        animHandler.postDelayed(animationInvalidator, ANIM_FRAMEDELAY);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if (model == null) return false;


        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (model.getGameState() != GameGrid.GAMESTATE.PLAY) {
                startAnim(null);
            } else {
                gestureCenter.set((int) ev.getX(), (int) ev.getY());
                gestOffset.set(0,0);
                fancyGridOffset = new Point(gestureCenter);
                lastDir = DIRECTION.NONE;
                animList = null;
                animStop = true;
            }
            return true;
        }

        else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            if (model.getGameState() == GameGrid.GAMESTATE.PLAY) {
                gestOffset.set(
                        (int)ev.getX() - gestureCenter.x,
                        (int)ev.getY() - gestureCenter.y );

                invalidate();
            }
            return true;
        }

        else if (ev.getAction() == MotionEvent.ACTION_UP) {
            if (offsetSize > MINIMAL_RANGE) {
                List<GameGrid.Action> anims = new ArrayList<GameGrid.Action>();
                model.doMove(lastDir, anims);
                animLastOffset.set(fancyGridOffset.x, fancyGridOffset.y);
                startAnim(anims);
            }
            gestOffset.set(0, 0);
            lastDir = DIRECTION.NONE;
            invalidate();
            return true;
        }

        return super.onTouchEvent(ev);
    }


    private void updateFancyGridOffset() {

        /*************
         *
         * Scheme:
         * if offset < MINIMAL_RANGE then it's deadzone
         * lastDir - first recognized direction after ACTION_DOWN touch event or opposite
         *
         * offset can't be higher than 0.68 grid cell width/height
         *
         */

        int offsetX = gestOffset.x;
        int offsetY = gestOffset.y;

        if (Math.abs(offsetX - offsetY) > MINIMAL_RANGE) {
            // set first direction
            if ( Math.abs(gestOffset.x) > Math.abs(gestOffset.y) ) {
                if (gestOffset.x > 0)   lastDir = DIRECTION.RIGHT;
                else                    lastDir = DIRECTION.LEFT;
            } else {
                if (gestOffset.y > 0)   lastDir = DIRECTION.DOWN;
                else                    lastDir = DIRECTION.UP;
            }

        }

        // keep last direction until ACTION_UP touch event is present
        if (lastDir == DIRECTION.DOWN || lastDir == DIRECTION.UP) {
            offsetX = 0;
            if (offsetY > 0) lastDir = DIRECTION.DOWN;
            else             lastDir = DIRECTION.UP;
        } else {
            offsetY = 0;
            if (offsetX > 0) lastDir = DIRECTION.RIGHT;
            else             lastDir = DIRECTION.LEFT;
        }


        // offset can't be higher than 1;
        int maxX = (int)((gridLeftTopRect.width() + gridMargins)*0.68);
        int maxY = (int)((gridLeftTopRect.height() + gridMargins)*0.68);
        if (Math.abs(offsetX) > maxX)
            offsetX = maxX * (int)Math.signum(offsetX);
        if (Math.abs(offsetY) > maxY)
            offsetY = maxY * (int)Math.signum(offsetY);

        // smooth deadzone snap with **3
        offsetSize = Math.max(Math.abs(offsetX), Math.abs(offsetY));
        if (offsetSize < MINIMAL_RANGE) {
            offsetX = (int)(MINIMAL_RANGE * Math.pow(offsetX/(double)MINIMAL_RANGE, 3));
            offsetY = (int)(MINIMAL_RANGE * Math.pow(offsetY/(double)MINIMAL_RANGE, 3));
            offsetSize = Math.abs(offsetX + offsetY);
        }

        fancyGridOffset.set(offsetX, offsetY);

    }

}