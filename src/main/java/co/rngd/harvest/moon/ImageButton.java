package co.rngd.harvest.moon;

import com.badlogic.gdx.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.*;

public class ImageButton {
  public static int NORMAL = 0, HOVER = 1, PRESSED = 2, DISABLED = 3;
  private final TextureRegion image[] = new TextureRegion[4];

  private float x, y, width = Float.NaN, height = Float.NaN;
  private boolean pressed = false, enabled = true, visible = true;
  private boolean hover = false, wasPressed = false;

  private ImageButton() { }

  public static ImageButton create(Texture image) { return create(new TextureRegion(image)); }

  public static ImageButton create(TextureRegion image) { return create(image, null, null, null); }

  public static ImageButton create(TextureRegion normalImage, TextureRegion hoverImage, TextureRegion pressedImage, TextureRegion disabledImage) {
    ImageButton result = new ImageButton();
    result.image[0] = normalImage;
    result.image[1] = hoverImage;
    result.image[2] = pressedImage;
    result.image[3] = disabledImage;
    return result;
  }

  public void setImage(int state, Texture toImage) { setImage(state, new TextureRegion(toImage)); }
  public void setImage(int state, TextureRegion toImage) { image[state] = toImage; }
  public void setImages(TextureRegion normalImage, TextureRegion hoverImage, TextureRegion pressedImage, TextureRegion disabledImage) {
    image[0] = normalImage;
    image[1] = hoverImage;
    image[2] = pressedImage;
    image[3] = disabledImage;
  }

  public boolean isVisible() { return visible; }
  public void setVisible(boolean value) { visible = value; }
  public void show() { setVisible(true); }
  public void hide() { setVisible(false); }

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean value) { enabled = value; }
  public void enable() { setEnabled(true); }
  public void disable() { setEnabled(false); }

  public void setPosition(float x, float y) { this.x = x; this.y = y; }
  public void setSize(float width, float height) { this.width = width; this.height = height; }

  public void update() {
    if (!visible) {
      hover = false;
      pressed = false;
      wasPressed = false;
      return;
    }

    float touchX = Gdx.input.getX(), touchY = Gdx.graphics.getHeight() - Gdx.input.getY();
    boolean button = Gdx.input.isButtonPressed(Input.Buttons.LEFT);

    float minX = x >= 0 ? x : Gdx.graphics.getWidth() + x,  maxX,
          minY = y >= 0 ? y : Gdx.graphics.getHeight() + y, maxY;
    float w = Float.isNaN(width)  ? getActiveTextureRegion().getRegionWidth()  : width,
          h = Float.isNaN(height) ? getActiveTextureRegion().getRegionHeight() : height;
    if (w > 0) maxX = minX + w; else { maxX = minX; minX = maxX + w; }
    if (h > 0) maxY = minY + h; else { maxY = minY; minY = maxY + h; }

    boolean inRegion = (touchX >= minX && touchX < maxX) && (touchY >= minY && touchY < maxY);

    if (!inRegion) {
      hover = false;
      pressed = false;
    }
    else {
      hover = true;
      if (button) pressed = true;
      else {
        if (pressed) wasPressed = true;
        pressed = false;
      }
    }
  }

  private TextureRegion getActiveTextureRegion() {
    if (!enabled) return image[DISABLED] != null ? image[DISABLED] : image[NORMAL];
    if (pressed && image[PRESSED] != null) return image[PRESSED];
    if (pressed && image[HOVER] != null) return image[HOVER];
    if (hover && image[HOVER] != null) return image[HOVER];
    return image[NORMAL];
  }

  public void draw(SpriteBatch batch) {
    if (!visible) return;
    TextureRegion image = getActiveTextureRegion();

    float posX = x >= 0 ? x : Gdx.graphics.getWidth() + x,
          posY = y >= 0 ? y : Gdx.graphics.getHeight() + y;
    if (Float.isNaN(width) || Float.isNaN(height)) batch.draw(image, posX, posY);
    else batch.draw(image, posX, posY, width, height);
  }

  public boolean wasPressed() {
    boolean result = wasPressed;
    wasPressed = false;
    return result;
  }
}
