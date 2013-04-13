import java.util.Random;

class Map {
  private static final int PIXEL_MASK_WALL = 0xff0000;
  private static final int PIXEL_MONSTER_HEAD = 0xFFFFFE;
  private static final int PIXEL_INNER_WALL = 0xFFFFFF;
  private static final int PIXEL_BORDER_WALL = 0xFF8052;
  private static final int PIXEL_OUTER_WALL = 0xFFFEFE;
  private static final int PIXEL_MASK_END_ROOM = 0xff0000;

  private final int width, height;
  private final int[] elements;

  Map(int width, int height) {
    this.width = width;
    this.height = height;
    elements = new int[width * height];
  }

  int getElement(int x, int y) {
    return elements[x + y * width];
  }

  int getElementSafe(int x, int y) {
    return elements[(x + y * width) & (width * height - 1)];
  }

  void setElement(int x, int y, int pixel) {
    elements[x + y * width] = pixel;
  }

  void setElementSafe(int x, int y, int pixel) {
    elements[(x + y * width) & (width * height - 1)] = pixel;
  }

  void maskEndRoom(int x, int y) {
    // Give the end room a red tint.
    elements[x + y * width] &= PIXEL_MASK_END_ROOM;
  }

  boolean isWall(int x, int y) {
    return getElement(x, y) >= 0xfffffe;
  }

  boolean isWallSafe(int x, int y) {
    return getElementSafe(x, y) == 0xffffff;
  }

  boolean isMonsterSafe(int x, int y) {
    // 0xffffff is the color of character clothes.
    return getElementSafe(x, y) == 0xffffff;
  }

  boolean isMonsterHead(int x, int y) {
    return getElement(x, y) >= PIXEL_MONSTER_HEAD;
  }

  void setMonsterHead(int x, int y) {
    setElement(x, y, PIXEL_MONSTER_HEAD);
  }

  void setInnerWall(int x, int y) {
    setElement(x, y, PIXEL_INNER_WALL);
  }

  void setBorderWall(int x, int y) {
    setElement(x, y, PIXEL_BORDER_WALL);
  }

  void setOuterWall(int x, int y) {
    setElement(x, y, PIXEL_OUTER_WALL);
  }

  boolean isAnyWall(int x, int y) {
    return getElement(x, y) >= PIXEL_MASK_WALL;
  }

  boolean isAnyWallSafe(int x, int y) {
    return getElementSafe(x, y) >= PIXEL_MASK_WALL;
  }

  void generate(Random random, Point startPoint, Point endRoomTopLeft,
      Point endRoomBottomRight) {
    final int ROOM_COUNT = 70;

    // Draw the floor of the level with an uneven green color.
    // Put a wall around the perimeter.
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int br = random.nextInt(32) + 112;
        setElement(x, y, (br / 3) << 16 | (br) << 8);
        if (x < 4 || y < 4 || x >= (width - 4) || y >= (height - 4)) {
          setOuterWall(x, y);
        }
      }
    }

    // Create 70 rooms. Put the player in the 69th, and make the 70th red.
    for (int i = 0; i < ROOM_COUNT; i++) {
      boolean isStartRoom = i == ROOM_COUNT - 2;
      boolean isEndRoom = i == ROOM_COUNT - 1;

      // Create a room that's possibly as big as the level, whose coordinates
      // are clamped to the nearest multiple of 16.
      int w = random.nextInt(8) + 2;
      int h = random.nextInt(8) + 2;
      int xm = random.nextInt(64 - w - 2) + 1;
      int ym = random.nextInt(64 - h - 2) + 1;

      w *= 16;
      h *= 16;

      w += 5;
      h += 5;
      xm *= 16;
      ym *= 16;

      if (isStartRoom) {
        startPoint.x = xm + w / 2;
        startPoint.y = ym + h / 2;
      }

      if (isEndRoom) {
        endRoomTopLeft.x = xm + 5;
        endRoomTopLeft.y = ym + 5;
        endRoomBottomRight.x = xm + w - 5;
        endRoomBottomRight.y = ym + w - 5;
      }

      for (int y = ym; y < ym + h; y++) {
        for (int x = xm; x < xm + w; x++) {

          // This seems to calculate the thickness of the wall.
          int d = x - xm;
          if (xm + w - x - 1 < d)
            d = xm + w - x - 1;
          if (y - ym < d)
            d = y - ym;
          if (ym + h - y - 1 < d)
            d = ym + h - y - 1;

          // Are we inside the wall, and thus in the room?
          if (d > 4) {
            // Yes, we are. Draw the floor.

            // Vary the color of the floor.
            int br = random.nextInt(16) + 112;

            // Floor diagonal
            if (((x + y) & 3) == 0) {
              br += 16;
            }

            // Grayish concrete floor
            setElement(x, y, (br * 3 / 3) << 16 | (br * 4 / 4) << 8
                | (br * 4 / 4));
          } else {
            // No, we're not. Draw the orange wall border.
            setBorderWall(x, y);
          }

          if (isEndRoom) {
            maskEndRoom(x, y);
          }
        }
      }

      // Put two exits in the room.
      for (int j = 0; j < 2; j++) {
        int xGap = random.nextInt(w - 24) + xm + 5;
        int yGap = random.nextInt(h - 24) + ym + 5;
        int ww = 5;
        int hh = 5;

        xGap = xGap / 16 * 16 + 5;
        yGap = yGap / 16 * 16 + 5;
        if (random.nextInt(2) == 0) {
          xGap = xm + (w - 5) * random.nextInt(2);
          hh = 11;
        } else {
          ww = 11;
          yGap = ym + (h - 5) * random.nextInt(2);
        }
        for (int y = yGap; y < yGap + hh; y++) {
          for (int x = xGap; x < xGap + ww; x++) {
            // A slightly darker color represents the exit.
            int br = random.nextInt(32) + 112 - 64;
            setElement(x, y, (br * 3 / 3) << 16 | (br * 4 / 4) << 8
                | (br * 4 / 4));
          }
        }
      }
    }

    // Paint the inside of each wall white. This is for wall-collision
    // detection.
    for (int y = 1; y < height - 1; y++) {
      inloop: for (int x = 1; x < width - 1; x++) {
        for (int xx = x - 1; xx <= x + 1; xx++) {
          for (int yy = y - 1; yy <= y + 1; yy++) {
            if (!isAnyWall(xx, yy)) {
              continue inloop;
            }
          }
        }
        setInnerWall(x, y);
      }
    }
  }

}