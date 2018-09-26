package com.raduetsya.game2048;

import java.util.ArrayList;
import java.util.List;

import android.support.v7.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    GameGrid gameGridModel = new GameGrid(4,4);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {

        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        GameView view = (GameView)(findViewById(R.id.game_view));
        view.setModel(gameGridModel);
        view.invalidate();

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        gameGridModel.restoreState(preferences);
        if (preferences.getBoolean("NEW_GAME", true) == true)
            gameGridModel.doNewGame(null);

    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();

        editor.putBoolean("NEW_GAME", false);
        gameGridModel.saveState(editor);

        editor.commit();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        GameView view = (GameView)(findViewById(R.id.game_view));
        if (id == R.id.action_newgame) {
            List<GameGrid.Action> animList = new ArrayList<GameGrid.Action>();
            gameGridModel.doNewGame(animList);
            view.startAnim(animList);
        }
        if (id == R.id.action_cheat) {
            gameGridModel.doCheat();
            view.invalidate();
        }
        return super.onOptionsItemSelected(item);
    }

}