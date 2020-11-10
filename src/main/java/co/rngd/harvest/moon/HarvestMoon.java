package co.rngd.harvest.moon;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.backends.lwjgl3.*;

public class HarvestMoon extends Game {
  private ModelBatch modelBatch;
  private SpriteBatch spriteBatch;
  private TextureCache textureCache;

  public static void main(String... args) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		new Lwjgl3Application(new HarvestMoon(), config);
  }

  @Override public void create() {
    modelBatch = new ModelBatch();
    spriteBatch = new SpriteBatch();
    textureCache = new TextureCache();

    GameplayScreen gameplayScreen = new GameplayScreen(modelBatch, spriteBatch, textureCache);

    GameMap map = new GameMap(60, 60, 5);
    map.crater(30, 30, 20, 10, 9, 3);
    map.updateControlPoints();
    gameplayScreen.setMap(map);
    setScreen(gameplayScreen);
  }

  @Override public void dispose() {
    modelBatch.dispose();
    spriteBatch.dispose();
    textureCache.dispose();
  }
}
