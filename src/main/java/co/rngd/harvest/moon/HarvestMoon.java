package co.rngd.harvest.moon;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class HarvestMoon extends Game {
  private ModelBatch modelBatch;
  private SpriteBatch spriteBatch;

  public static void main(String... args) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		new Lwjgl3Application(new HarvestMoon(), config);
  }

  @Override public void create() {
    modelBatch = new ModelBatch();
    spriteBatch = new SpriteBatch();
    GameplayScreen gameplayScreen = new GameplayScreen(modelBatch, spriteBatch);
    GameMap map = new GameMap(30, 30);
    gameplayScreen.setMap(map);
    setScreen(gameplayScreen);
  }

  @Override public void dispose() {
    modelBatch.dispose();
    spriteBatch.dispose();
  }
}
