import java.util.Random;

class Monster {
  final int index;
  Point position;
  int direction;
  int frame;
  int wanderDirection;
  int rage;
  int damage;
  int activity;
  int savedMapPixel;

  public Monster(int index) {
    this.index = index;
    position = new Point(0, 0);
  }

  private static final Random random = new Random();

  boolean isActive() {
    return activity != 0;
  }

  public void advanceSpriteFrame() {
    frame++;
  }

  boolean hasTakenDamage() {
    return damage > 0;
  }

  void agitate() {
    rage++;
  }

  boolean isMouthTouchingPlayer(double rx, double ry) {
    return rx > -6 && rx < 6 && ry > -6 && ry < 6 && !isPlayer();
  }

  boolean isPlayer() {
    return index == 0;
  }

  boolean isSpecial() {
    return index >= 255;
  }

  void place(int x, int y, int rushTime, int pixelToSave) {
    position.x = x;
    position.y = y;

    // Remember this map pixel.
    savedMapPixel = pixelToSave;

    // Mark monster as idle or attacking.
    rage = (rushTime > 0 || random.nextInt(3) == 0) ? 127 : 0;

    // Mark monster active.
    activity = 1;

    // Distribute the monsters' initial direction.
    direction = index & 15;
  }

  boolean isEarly() {
    return index <= 128;
  }

  void markInactive() {
    activity = 0;
  }

  void move(Map map, Point movement) {
    // Restore prior pixel.
    map.setElement(position.x, position.y, savedMapPixel);

    // New position.
    position.x += movement.x;
    position.y += movement.y;

    // Remember new pixel's contents.
    savedMapPixel = map.getElement(position.x, position.y);

    // Put ourselves here.
    map.setMonsterHead(position.x, position.y);
  }

}