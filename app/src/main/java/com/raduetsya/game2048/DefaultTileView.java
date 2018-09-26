package com.raduetsya.game2048;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Align;

/**
 * Created by raduetsya on 3/18/14.
 */

public class DefaultTileView implements TileView {

    int roundAngleSize;
    Paint tilePaint = new Paint();
    RectF tempRect = new RectF();
    Rect bounds = new Rect();
    static final int MAX_SIZE_INCREASE = 15;
    int fontSize;
    int desiredFontSize_forBounds = 0;


    int[] tileColorArr = new int[]{
            R.color.tileColor0, R.color.tileColor1, R.color.tileColor2, R.color.tileColor3,
            R.color.tileColor4, R.color.tileColor5, R.color.tileColor6, R.color.tileColor7,
            R.color.tileColor8, R.color.tileColor9, R.color.tileColor10, R.color.tileColor11
    };
    int[] tileTextColorArr = new int[]{
            R.color.textColor0, R.color.textColor1, R.color.textColor2, R.color.textColor3,
            R.color.textColor4, R.color.textColor5, R.color.textColor6, R.color.textColor7,
            R.color.textColor8, R.color.textColor9, R.color.textColor10, R.color.textColor11
    };

    public DefaultTileView(Context context, int roundAngleSize) {
        this.roundAngleSize = roundAngleSize;
        for (int i=0; i<tileColorArr.length; i++) {
            tileColorArr[i] = context.getResources().getColor(tileColorArr[i]);
            tileTextColorArr[i] = context.getResources().getColor(tileTextColorArr[i]);
        }
    }

    @Override
    public void draw(Canvas c, RectF rect, int rang, float howCloseToDissapear) {
        if (rang == 0) return;

        int realRang = rang;
        if (rang > 11) rang = 11;

        // draw tile
        tilePaint.setAntiAlias(true);
        tilePaint.setColor( tileColorArr[rang] );

        tempRect.set(rect);
        if (howCloseToDissapear != 0) {
            int newSize = (int)(Math.pow((1 - howCloseToDissapear),3) * MAX_SIZE_INCREASE);
            tempRect.inset(-1*newSize, -1*newSize);
        }

        c.drawRoundRect(tempRect, roundAngleSize, roundAngleSize, tilePaint);

        // draw text
        String text = "" + (int)(Math.pow(2,realRang));

        tilePaint.setColor(tileTextColorArr[rang]);
        tilePaint.setTextAlign(Align.CENTER);
        if ( desiredFontSize_forBounds != tempRect.width()*0.8 ) {
            // http://stackoverflow.com/questions/12166476/android-canvas-drawtext-set-font-size-from-width
            final float testTextSize = 48f;
            tilePaint.setTextSize(testTextSize);
            tilePaint.getTextBounds("2048", 0, 4, bounds);
            fontSize = (int) ((testTextSize * (tempRect.width()*0.8)) / bounds.width());
            desiredFontSize_forBounds = (int) (tempRect.width()*0.8);
        }
        tilePaint.setTextSize(fontSize);

        tilePaint.getTextBounds(text, 0, text.length(), bounds);
        float centerX = tempRect.centerX();// - bounds.width()/2;
        float centerY = tempRect.centerY() + bounds.height()/2;

        c.drawText(text, centerX, centerY, tilePaint);

    }
}