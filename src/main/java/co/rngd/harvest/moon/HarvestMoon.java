package co.rngd.harvest.moon;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class HarvestMoon extends Game {
  private ModelBatch modelBatch;

  public static void main(String... args) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		new Lwjgl3Application(new HarvestMoon(), config);
  }

  @Override public void create() {
    modelBatch = new ModelBatch();
    GameplayScreen gameplayScreen = new GameplayScreen(modelBatch);
    GameMap map = new GameMap(30, 30);
    gameplayScreen.setMap(map);
    setScreen(gameplayScreen);
  }
}
