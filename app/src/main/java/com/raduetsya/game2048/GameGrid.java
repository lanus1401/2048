package com.raduetsya.game2048;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.SharedPreferences;

/**
 * Created by raduetsya on 3/14/14.
 */

public class GameGrid {

    private static final int MAX_RANG_TO_ADD = 2;

    Random rand = new Random();

    public enum DIRECTION { UP, DOWN, LEFT, RIGHT, NONE }

    private class Tile {
        public int rang;

        public int[] canMove = new int[4];
        public boolean[] canMerge = new boolean[4];

        public int getCanMove(DIRECTION dir) {
            return canMove[ getDir(dir) ];
        }

        public void setCanMove(DIRECTION dir, int value) {
            canMove[ getDir(dir) ] = value;
        }

        public boolean getCanMerge(DIRECTION dir) {
            return canMerge[ getDir(dir) ];
        }

        public void setCanMerge(DIRECTION dir, boolean value) {
            canMerge[getDir(dir)] = value;
        }

        private int getDir(DIRECTION dir) throws IllegalArgumentException {
            switch (dir) {
                case UP: return 0;
                case DOWN: return 1;
                case LEFT: return 2;
                case RIGHT: return 3;
                case NONE: break;
            }
            throw new IllegalArgumentException();
        }


    }

    public class Action {
        public static final int NOTHING     = 0;
        public static final int MOVE        = 1;
        public static final int CREATE      = 2;
        public static final int MERGE       = 3;

        public int type;
        public int oldX, oldY;
        public int newX, newY;
        public int rang;

        public Action(int type, int rang, int oldX, int oldY, int newX, int newY) {
            this.type = type;
            this.rang = rang;
            this.oldX = oldX;
            this.oldY = oldY;
            this.newX = newX;
            this.newY = newY;
        }
    }

    Tile[][] data;
    int sizeX, sizeY;

    int scoreCurrent = 0;
    public int getScore() { return scoreCurrent; }
    int scoreHigh = 0;
    public int getHighscore() { return scoreHigh; }
    public void updateHighscore() {
        if (scoreCurrent > scoreHigh) { scoreHigh = scoreCurrent; }
    }

    public enum GAMESTATE { PLAY, GAMEOVER, WIN };
    GAMESTATE gameState = GAMESTATE.PLAY;


    public GameGrid(int gridSizeX, int gridSizeY) {

        /******************************
         *
         *  data[1][2] = { x=1; y=2 }
         *
         *  x->sizeX = RIGHT
         *  y->sizeY = DOWN
         *
         *  for (y=0; y<sizeY; y++)
         *      for (x=0; x<sizeX; x++)
         *          print data[x][y] // one line
         *      print "\n" // end of line
         *
         *****************************/

        sizeX = gridSizeX;
        sizeY = gridSizeY;

        data = createData(sizeX, sizeY);
    }

    Tile[][] createData(int w, int h) {
        Tile[][] newData = new Tile[w][];
        for (int x=0; x<sizeX; x++) {
            newData[x] = new Tile[h];
            for (int y=0; y<sizeY; y++) newData[x][y] = new Tile();
        }
        return newData;
    }

    public void doNewGame(List<Action> actionHistory) {
        updateHighscore();
        for(int i=0; i<sizeX; i++)
            for (int j=0; j<sizeY; j++) {
                //if ((i*sizeY+j) <= 11) data[i][j].rang = i*sizeY + j;
                data[i][j].rang = 0;
            }

        gameState = GAMESTATE.PLAY;
        //data[0][0].rang = 10;
        //data[0][1].rang = 10;
        addNewTileToRandomCell(actionHistory);
        addNewTileToRandomCell(actionHistory);
        //addNewTileToRandomCell(actionHistory);

        updateMovingAbilities();
        scoreCurrent = 0;
    }

    public void doCheat() {
        for(int i=0; i<sizeX; i++)
            for (int j=0; j<sizeY; j++)
                if (data[i][j].rang <= 2)
                    data[i][j].rang = 0;
        updateMovingAbilities();
        scoreCurrent = 0;

    }

    public int get(int x, int y) {
        return data[x][y].rang;
    }

    public void doMove(DIRECTION dir, List<Action> actionHistory) {
        updateState();
        if (gameState != GAMESTATE.PLAY) return;

        boolean canDoMove = false;

        Tile[][] newData = createData(sizeX, sizeY);
        List<int[]> increaseRang = new ArrayList<int[]>();

        for(int x=0; x<sizeX; x++) {
            for(int y=0; y<sizeY; y++) {
                if (data[x][y].rang == 0) continue;
                int newX = x; int newY = y;
                switch (dir) {
                    case UP:
                        newY -= data[x][y].getCanMove(DIRECTION.UP);
                        break;
                    case DOWN:
                        newY += data[x][y].getCanMove(DIRECTION.DOWN);
                        break;
                    case LEFT:
                        newX -= data[x][y].getCanMove(DIRECTION.LEFT);
                        break;
                    case RIGHT:
                        newX += data[x][y].getCanMove(DIRECTION.RIGHT);
                        break;
                    case NONE: break;
                }
                newData[newX][newY] = data[x][y];
                if ( data[x][y].getCanMerge(dir) ) {
                    increaseRang.add(new int[] { newX, newY, (data[x][y].rang+1) });
                }
                if (actionHistory != null) {
                    actionHistory.add( new Action(Action.MOVE, data[x][y].rang, x, y, newX, newY) );
                }
                if (x != newX || y != newY)
                    canDoMove = true;
            }
        }

        for (int[] tile : increaseRang) {

            scoreCurrent += Math.pow(2,tile[2]);

            if (tile[2] == 11)
                gameState = GAMESTATE.WIN;
            newData[tile[0]][tile[1]].rang = tile[2];
            if (actionHistory != null) {
                actionHistory.add( new Action(Action.CREATE, tile[2], tile[0], tile[1], tile[0], tile[1]) );
            }
        }

        if (canDoMove) {
            data = newData;

            addNewTileToRandomCell(actionHistory);
            updateMovingAbilities();
        }
    }

    public boolean isAbleToMove(int x, int y, DIRECTION dir) {
        return (data[x][y].getCanMove(dir) != 0 || data[x][y].getCanMerge(dir));
    }

    public GAMESTATE getGameState() {
        return gameState;
    }

    void addNewTileToRandomCell(List<Action> actionHistory) {
        if (gameState != GAMESTATE.PLAY) return;

        int x,y;
        do {
            x = rand.nextInt(sizeX);
            y = rand.nextInt(sizeY);
        } while ( data[x][y].rang != 0);
        data[x][y].rang = rand.nextInt(MAX_RANG_TO_ADD)+1;

        if (actionHistory != null) {
            actionHistory.add( new Action(Action.CREATE, data[x][y].rang, x, y, x, y) );
        }
    }

    // this vars need to do interaction b/w updateMovingAbilities() and updateTile()
    int foundedSpace;
    int lastRangForMerge;

    private void updateTile(int x, int y, DIRECTION dir) {
        if (data[x][y].rang == 0)
            foundedSpace++;
        else {
            data[x][y].setCanMove(dir, foundedSpace);

            if (data[x][y].rang == lastRangForMerge) {
                data[x][y].setCanMerge(dir, true);
                lastRangForMerge = 0;
                foundedSpace++;
                data[x][y].setCanMove(dir, foundedSpace);
            } else {
                lastRangForMerge = data[x][y].rang;
                data[x][y].setCanMerge(dir, false);
            }
        }
    }

    private void updateMovingAbilities() {
        int x,y;

        for(y=0; y<sizeY; y++) {
            foundedSpace = 0;
            lastRangForMerge = 0;
            for (x=sizeX-1; x>=0; x--) {
                updateTile(x,y,DIRECTION.RIGHT);
            }
        }
        for(y=0; y<sizeY; y++) {
            foundedSpace = 0;
            lastRangForMerge = 0;
            for (x=0; x<sizeX; x++) {
                updateTile(x,y,DIRECTION.LEFT);
            }
        }
        for(x=0; x<sizeX; x++) {
            foundedSpace = 0;
            lastRangForMerge = 0;
            for (y=0; y<sizeY; y++) {
                updateTile(x,y,DIRECTION.UP);
            }
        }
        for(x=0; x<sizeX; x++) {
            foundedSpace = 0;
            lastRangForMerge = 0;
            for (y=sizeY-1; y>=0; y--) {
                updateTile(x,y,DIRECTION.DOWN);
            }
        }
        updateState();
    }

    void updateState() {
        boolean nowhereToMove = true;
        gameState = GAMESTATE.PLAY;
        for(int x=0; x<sizeX; x++) {
            for (int y=0; y<sizeY; y++) {
                if (data[x][y].rang == 11)
                    gameState = GAMESTATE.WIN;
                if (nowhereToMove)
                    for(int m=0; m<data[x][y].canMove.length; m++)
                        if (data[x][y].canMove[m] != 0)
                            nowhereToMove = false;
            }
        }
        if (nowhereToMove && gameState != GAMESTATE.WIN)
            gameState = GAMESTATE.GAMEOVER;
    }

    /* SERIALIZER */

    public void saveState(SharedPreferences.Editor bundle) {
        bundle.putInt("GRID_WIDTH", sizeX);
        bundle.putInt("GRID_HEIGHT", sizeY);
        for (int x=0; x<sizeX; x++) {
            for (int y=0; y<sizeY; y++) {
                bundle.putInt("GRID_DATA_"+x+"_"+y, data[x][y].rang);
            }
        }
        bundle.putInt("CURRENT_SCORE", scoreCurrent);
        bundle.putInt("HIGH_SCORE", scoreHigh);

    }

    public void restoreState(SharedPreferences bundle) {
        sizeX = bundle.getInt("GRID_WIDTH", 4);
        sizeY = bundle.getInt("GRID_HEIGHT", 4);
        data = createData(sizeX, sizeY);
        for (int x=0; x<sizeX; x++) {
            for (int y=0; y<sizeY; y++) {
                data[x][y].rang = bundle.getInt("GRID_DATA_"+x+"_"+y, 0);
            }
        }
        scoreCurrent = bundle.getInt("CURRENT_SCORE", 0);
        scoreHigh = bundle.getInt("HIGH_SCORE", 0);
        updateMovingAbilities();
    }

}