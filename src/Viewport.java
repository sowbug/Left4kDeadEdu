import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Random;

class Viewport {
  private final int width;
  private final int width_half;
  private final int height;
  private final int height_half;

  private final BufferedImage image;
  private final Graphics ogr;
  private final Graphics sg;

  private final int[] pixels;
  private final int[] lightmap;
  private final int[] brightness;
  private final int[] sprites;

  private final Random random = new Random();
  private int screenWidth;
  private int screenHeight;

  Viewport(int width, int height, int screenWidth, int screenHeight,
      Graphics graphics) {
    this.width = width;
    this.width_half = width / 2;
    this.height = height;
    this.height_half = height / 2;

    this.screenWidth = screenWidth;
    this.screenHeight = screenHeight;

    image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    ogr = image.getGraphics();
    sg = graphics;

    pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    lightmap = new int[width * height];

    brightness = new int[512];
    generateBrightness();

    sprites = new int[18 * 4 * 16 * 12 * 12];
    generateSprites();
  }

  /**
   * Generates a bunch of top-down sprites using surprisingly compact code.
   */
  private void generateSprites() {
    final int PIXEL_ZOMBIE_SKIN = 0xa0ff90;
    final int PIXEL_SKIN = 0xFF9993;

    int pix = 0;
    for (int i = 0; i < 18; i++) {
      int skin = PIXEL_SKIN;
      int clothes = 0xFFffff;

      if (i > 0) {
        skin = PIXEL_ZOMBIE_SKIN;
        clothes = (random.nextInt(0x1000000) & 0x7f7f7f);
      }
      for (int t = 0; t < 4; t++) {
        for (int d = 0; d < 16; d++) {
          double dir = d * Math.PI * 2 / 16.0;

          if (t == 1)
            dir += 0.5 * Math.PI * 2 / 16.0;
          if (t == 3)
            dir -= 0.5 * Math.PI * 2 / 16.0;

          // if (i == 17)
          // {
          // dir = d * Math.PI * 2 / 64;
          // }

          double cos = Math.cos(dir);
          double sin = Math.sin(dir);

          for (int y = 0; y < 12; y++) {
            int col = 0x000000;
            for (int x = 0; x < 12; x++) {
              int xPix = (int) (cos * (x - 6) + sin * (y - 6) + 6.5);
              int yPix = (int) (cos * (y - 6) - sin * (x - 6) + 6.5);

              if (i == 17) {
                if (xPix > 3 && xPix < 9 && yPix > 3 && yPix < 9) {
                  col = 0xff0000 + (t & 1) * 0xff00;
                }
              } else {
                if (t == 1 && xPix > 1 && xPix < 4 && yPix > 3 && yPix < 8)
                  col = skin;
                if (t == 3 && xPix > 8 && xPix < 11 && yPix > 3 && yPix < 8)
                  col = skin;

                if (xPix > 1 && xPix < 11 && yPix > 5 && yPix < 8) {
                  col = clothes;
                }
                if (xPix > 4 && xPix < 8 && yPix > 4 && yPix < 8) {
                  col = skin;
                }
              }
              sprites[pix++] = col;

              // If we just drew a pixel, make the next one an almost-black
              // pixel, and if it's already an almost-black one, make it
              // transparent (full black). (This is all honored only if the
              // next pixel isn't actually set to something else.) This takes
              // advantage of the left-to-right scanning of the sprite
              // generation to create a slight shadow effect on each sprite.
              if (col > 1) {
                col = 1;
              } else {
                col = 0;
              }
            }
          }
        }
      }
    }
  }

  private void generateBrightness() {
    double offs = 30;
    for (int i = 0; i < 512; i++) {
      brightness[i] = (int) (255.0 * offs / (i + offs));
      if (i < 4)
        brightness[i] = brightness[i] * i / 4;
    }
  }

  private void calculateLightmap(Map map, int tick, double playerDir,
      Point camera) {
    for (int i = 0; i < width * 4; i++) {
      // Calculate a point along the outer wall of the view.
      int xt = i % width - width_half;
      int yt = (i / height % 2) * (height - 1) - height_half;

      if (i >= width * 2) {
        int tmp = xt;
        xt = yt;
        yt = tmp;
      }

      // Figure out how far the current beam is from the player's view.
      // In radians, not degrees, but same idea -- if the player is looking
      // 180 degrees south, and this beam is pointing 270 degrees west,
      // then the answer is 90 degrees (in radians). This is for creating a
      // flashlight effect in front of the player.
      //
      // Clamp to a circle (2 x pi).
      double dd = Math.atan2(yt, xt) - playerDir;
      if (dd < -Math.PI)
        dd += Math.PI * 2;
      if (dd >= Math.PI)
        dd -= Math.PI * 2;

      // This calculation is weird because of the 1- and the *255. It seems
      // arbitrary. Maybe it is. brr is probably supposed to stand for
      // something like "brightness times radius squared."
      int brr = (int) ((1 - dd * dd) * 255);

      int dist = width_half;
      if (brr < 0) {
        // Cut off the flashlight past a certain angle, but for better
        // playability leave a small halo going all the way around the player.
        brr = 0;
        dist = 32;
      }
      // At the very start of the level, fade in the light gradually.
      if (tick < 60)
        brr = brr * tick / 60;

      int j = 0;
      for (; j < dist; j++) {
        // Loop through the beam's pixels one fraction of the total distance
        // each iteration. This is very slightly inefficient because in some
        // cases we'll calculate the same pixel twice.
        int xx = xt * j / width_half + width_half;
        int yy = yt * j / height_half + height_half;
        int xm = xx + camera.x - width_half;
        int ym = yy + camera.y - height_half;

        // Stop the light if it hits a wall.
        if (map.isWallSafe(xm, ym))
          break;

        // Do an approximate distance calculation. I'm not sure why this
        // couldn't have been built into the brightness table, which would let
        // us easily index using j.
        int xd = (xx - width_half) * 256 / width_half;
        int yd = (yy - height_half) * 256 / height_half;
        int ddd = (xd * xd + yd * yd) / 256;
        int br = brightness[ddd] * brr / 255;

        // Draw the halo around the player.
        if (ddd < 16) {
          int tmp = 128 * (16 - ddd) / 16;
          br = br + tmp * (255 - br) / 255;
        }

        // Fill in the lightmap entry.
        lightmap[xx + yy * width] = br;
      }
    }
  }

  private void drawNoiseAndHUD(Game game) {
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int noise = random.nextInt(16) * random.nextInt(16) / 16;
        if (!game.isStarted())
          noise *= 4;

        int c = pixels[x + y * width];
        int l = lightmap[x + y * width];
        lightmap[x + y * width] = 0;
        int r = ((c >> 16) & 0xff) * l / 255 + noise;
        int g = ((c >> 8) & 0xff) * l / 255 + noise;
        int b = ((c) & 0xff) * l / 255 + noise;

        r = r * (255 - game.getHurtTime()) / 255 + game.getHurtTime();
        g = g * (255 - game.getBonusTime()) / 255 + game.getBonusTime();
        pixels[x + y * width] = r << 16 | g << 8 | b;
      }
      if (y % 2 == 0 && (y >= game.getDamage() && y < 220)) {
        for (int x = 232; x < 238; x++) {
          pixels[y * width + x] = 0x800000;
        }
      }
      if (y % 2 == 0 && (y >= game.getAmmo() && y < 220)) {
        for (int x = 224; x < 230; x++) {
          pixels[y * width + x] = 0x808000;
        }
      }
      if (y % 10 < 9 && (y >= game.getClips() && y < 220)) {
        for (int x = 221; x < 222; x++) {
          pixels[y * width + 221] = 0xffff00;
        }
      }
    }
  }

  private boolean isWithinView(int xm, int ym) {
    return xm > 0 && ym > 0 && xm < width && ym < height;
  }

  void drawBulletTrace(double cos, double sin, int closestHitDist) {
    int glow = 0;
    for (int j = closestHitDist; j >= 0; j--) {
      // Calculate pixel position.
      int xm = +(int) (cos * j) + width_half;
      int ym = -(int) (sin * j) + height_half;

      // Are we still within the view?
      if (isWithinView(xm, ym)) {

        // Every so often, draw a white dot and renew the glow. This gives a
        // cool randomized effect that looks like spitting sparks.
        if (random.nextInt(20) == 0 || j == closestHitDist) {
          pixels[xm + ym * width] = 0xffffff;
          glow = 200;
        }

        // Either way, brighten up the path according to the current glow.
        lightmap[xm + ym * width] += glow * (255 - lightmap[xm + ym * width])
            / 255;
      }

      // Fade the glow.
      glow = glow * 20 / 21;
    }
  }

  void drawBulletDebris(double playerDir, boolean hitMonster, Point hitPoint) {
    for (int i = 0; i < 10; i++) {
      double pow = random.nextInt(100) * random.nextInt(100) * 8.0 / 10000;
      double dir = (random.nextInt(100) - random.nextInt(100)) / 100.0;
      int xd = (int) (hitPoint.x - Math.cos(playerDir + dir) * pow)
          + random.nextInt(4) - random.nextInt(4);
      int yd = (int) (hitPoint.y - Math.sin(playerDir + dir) * pow)
          + random.nextInt(4) - random.nextInt(4);
      if (xd >= 0 && yd >= 0 && xd < width && yd < height) {
        if (hitMonster) {
          // Blood
          pixels[xd + yd * width] = 0xff0000;
        } else {
          // Wall
          pixels[xd + yd * width] = 0xcacaca;
        }
      }
    }
  }

  void drawImpactFlash(Point hitPoint) {
    for (int x = -12; x <= 12; x++) {
      for (int y = -12; y <= 12; y++) {
        Point offsetPoint = new Point(hitPoint.x + x, hitPoint.y + y);
        if (offsetPoint.x >= 0 && offsetPoint.y >= 0 && offsetPoint.x < width
            && offsetPoint.y < height) {
          lightmap[offsetPoint.x + offsetPoint.y * width] += 2000
              / (x * x + y * y + 10)
              * (255 - lightmap[offsetPoint.x + offsetPoint.y * width]) / 255;
        }
      }
    }
  }

  void drawMonster(int tick, Monster monster, double playerDir, Point camera,
      int xPos) {
    // Monster is active. Calculate position relative to player.
    int xm = xPos - camera.x + width_half;
    int ym = monster.position.y - camera.y + height_half;

    // Get monster's direction. This is just for figuring out which sprite
    // to draw.
    int d = monster.getDirection();
    if (monster.isPlayer()) {
      // or if this is the player, convert radian direction.
      d = (((int) (playerDir / (Math.PI * 2) * 16 + 4.5 + 16)) & 15);
    }

    d += ((monster.getFrame() / 4) & 3) * 16;

    // If non-special monster, convert to actual sprite pixel offset.
    int p = (0 * 16 + d) * 144;
    if (!monster.isPlayer()) {
      p += ((monster.getIndex() & 15) + 1) * 144 * 16 * 4;
    }

    // Special non-player monster: cycle through special sprite, either
    // red or yellow, spinning.
    if (monster.isSpecial()) {
      p = (17 * 4 * 16 + ((monster.getIndex() & 1) * 16 + (tick & 15))) * 144;
    }

    // Render the monster.
    for (int y = ym - 6; y < ym + 6; y++) {
      for (int x = xm - 6; x < xm + 6; x++) {
        int c = sprites[p++];
        if (c > 0 && x >= 0 && y >= 0 && x < width && y < height) {
          pixels[x + y * width] = c;
        }
      }
    }
  }

  private void copyView(Map map, Point camera) {
    for (int y = 0; y < height; y++) {
      int xm = camera.x - (width >> 1);
      int ym = y + camera.y - (height >> 1);
      for (int x = 0; x < width; x++) {
        pixels[x + y * width] = map.getElementSafe(xm + x, ym);
      }
    }
  }

  private void drawStatusText(Game game, UserInput userInput) {
    ogr.drawString("" + game.getScore(), 4, 232);
    if (!game.isStarted()) {
      ogr.drawString("Left 4k Dead", 80, 70);
      if (userInput.isTriggerPressed() && game.getHurtTime() == 0) {
        game.markGameStarted();
        userInput.setTriggerPressed(false);
      }
    } else if (game.getTick() < 60) {
      game.drawLevel(ogr);
    }
  }

  private void drawToScreen(int screen_width, int screen_height) {
    sg.drawImage(image, 0, 0, screen_width, screen_height, 0, 0, width, height,
        null);
  }

  void completeFrame(Game game, UserInput userInput) {
    drawNoiseAndHUD(game);
    drawStatusText(game, userInput);
    drawToScreen(screenWidth, screenHeight);
  }

  void prepareFrame(Game game, Map map, Point camera, double playerDir) {
    calculateLightmap(map, game.getTick(), playerDir, camera);
    copyView(map, camera);
  }

  int handleShot(Game game, UserInput userInput, boolean wasMonsterHit,
      double playerDir, double cos, double sin, int closestHitDistance) {
    // Is the ammo used up?
    if (!game.isAmmoAvailable()) {
      // Yes.
      game.setLongShootDelay();
      // Require trigger release.
      userInput.setTriggerPressed(false);
    } else {
      // No.
      game.setShortShootDelay();
      // Use up bullets.
      game.consumeAmmo(4);
    }

    drawBulletTrace(cos, sin, closestHitDistance);

    // Did the bullet hit within view?
    if (closestHitDistance < width_half) {
      closestHitDistance -= 3;
      Point hitPoint = new Point((int) (width_half + cos * closestHitDistance),
          (int) (height_half - sin * closestHitDistance));

      drawImpactFlash(hitPoint);
      drawBulletDebris(playerDir, wasMonsterHit, hitPoint);
    }

    return closestHitDistance;
  }

}
