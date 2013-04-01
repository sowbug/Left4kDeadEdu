class Map {
  private static final int PIXEL_MASK_WALL = 0xff0000;
  private static final int PIXEL_MONSTER_HEAD = 0xFFFFFE;
  private static final int PIXEL_INNER_WALL = 0xFFFFFF;

  private final int width, height;
  private final int[] elements;

  public Map(int width, int height) {
    this.width = width;
    this.height = height;
    elements = new int[width * height];
  }

  public int getElement(int x, int y) {
    return elements[x + y * width];
  }

  private int getElementSafe(int x, int y) {
    return elements[(x + y * width) & (width * height - 1)];
  }

  public void setElement(int x, int y, int pixel) {
    elements[x + y * width] = pixel;
  }

  public void setElementSafe(int x, int y, int pixel) {
    elements[(x + y * width) & (width * height - 1)] = pixel;
  }

  public void maskElement(int x, int y, int pixelMask) {
    elements[x + y * width] &= pixelMask;
  }

  public boolean isWall(int x, int y) {
    return getElement(x, y) >= 0xfffffe;
  }

  public boolean isWallSafe(int x, int y) {
    return getElementSafe(x, y) == 0xffffff;
  }

  public boolean isMonsterSafe(int x, int y) {
    // 0xffffff is the color of character clothes.
    return getElementSafe(x, y) == 0xffffff;
  }

  public boolean isMonsterHead(int x, int y) {
    return getElement(x, y) >= PIXEL_MONSTER_HEAD;
  }

  public void setMonsterHead(int x, int y) {
    setElement(x, y, PIXEL_MONSTER_HEAD);
  }

  public void setInnerWall(int x, int y) {
    setElement(x, y, PIXEL_INNER_WALL);
  }

  public boolean isAnyWall(int x, int y) {
    return getElement(x, y) >= PIXEL_MASK_WALL;
  }

  public boolean isAnyWallSafe(int x, int y) {
    return getElementSafe(x, y) >= PIXEL_MASK_WALL;
  }

  public void copyView(Point camera, int viewWidth, int viewHeight, int[] pixels) {
    for (int y = 0; y < viewHeight; y++) {
      int xm = camera.x - (viewWidth >> 1);
      int ym = y + camera.y - (viewHeight >> 1);
      for (int x = 0; x < viewWidth; x++) {
        pixels[x + y * viewWidth] = getElementSafe(xm + x, ym);
      }
    }
  }
}