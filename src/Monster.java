import java.util.Random;

class Monster {
  private static final Random random = new Random();

  private final int index;
  Point position;
  private int direction;
  private int frame;
  private int wanderDirection;
  private int rage;
  private int damage;
  private int activity;
  private int savedMapPixel;

  public Monster(int index) {
    this.index = index;
    position = new Point(0, 0);
  }

  public void advanceSpriteFrame() {
    frame++;
  }

  void agitate() {
    rage++;
  }

  boolean canSeePlayer(double rx, double ry) {
    return rx > -32 && rx < 220 && ry > -32 && ry < 32;
  }

  int getDirection() {
    return direction;
  }

  int getFrame() {
    return this.frame;
  }

  int getIndex() {
    return index;
  }

  int getSavedMapPixel() {
    return savedMapPixel;
  }

  boolean hasTakenDamage() {
    return damage > 0;
  }

  boolean isActive() {
    return activity != 0;
  }

  boolean isEarly() {
    return getIndex() <= 128;
  }

  boolean isMouthTouchingPlayer(double rx, double ry) {
    return rx > -6 && rx < 6 && ry > -6 && ry < 6 && !isPlayer();
  }

  boolean isPlayer() {
    return getIndex() == 0;
  }

  boolean isSomewhatEnraged() {
    return rage >= 8;
  }

  boolean isSpecial() {
    return getIndex() >= 255;
  }

  void markActive() {
    activity = 1;
  }

  void markDamaged() {
    damage = 1;
  }

  void markEnraged() {
    rage = 127;
  }

  void markInactive() {
    activity = 0;
  }

  void move(Map map, Point movement) {
    // Restore prior pixel.
    map.setElement(position.x, position.y, getSavedMapPixel());

    // New position.
    position.x += movement.x;
    position.y += movement.y;

    // Remember new pixel's contents.
    setSavedMapPixel(map.getElement(position.x, position.y));

    // Put ourselves here.
    map.setMonsterHead(position.x, position.y);
  }

  void pickWanderDirection() {
    wanderDirection = random.nextInt(25);
  }

  void place(int x, int y, int rushTime, int pixelToSave) {
    position.x = x;
    position.y = y;

    // Remember this map pixel.
    setSavedMapPixel(pixelToSave);

    // Mark monster as idle or attacking.
    rage = (rushTime > 0 || random.nextInt(3) == 0) ? 127 : 0;

    // Mark monster active.
    activity = 1;

    // Distribute the monsters' initial direction.
    setDirection(getIndex() & 15);
  }

  void processDamage(Map map, Game game, double playerDir, int xPos, int yPos) {
    // Add to monster's cumulative damage and reset temporary damage.
    activity += random.nextInt(3) + 1;
    damage = 0;

    double rot = 0.25; // How far around the blood spreads, radians
    int amount = 8; // How much blood
    double poww = 32; // How far to spread the blood

    // Is this monster sufficiently messed up to die?
    if (activity >= 2 + game.getLevel()) {
      rot = Math.PI * 2; // All the way around
      amount = 60; // lots of blood
      poww = 16;
      map.setElement(xPos, yPos, 0xa00000); // Red
      activity = 0; // Kill monster
      game.addScoreForMonsterDeath();
    }

    // Draw blood.
    for (int i = 0; i < amount; i++) {
      double pow = (random.nextInt(100) * random.nextInt(100)) * poww / 10000
          + 4;
      double dir = (random.nextInt(100) - random.nextInt(100)) / 100.0 * rot;
      double xdd = (Math.cos(playerDir + dir) * pow) + random.nextInt(4)
          - random.nextInt(4);
      double ydd = (Math.sin(playerDir + dir) * pow) + random.nextInt(4)
          - random.nextInt(4);
      int col = (random.nextInt(128) + 120);
      bloodLoop: for (int j = 2; j < pow; j++) {
        int xd = (int) (xPos + xdd * j / pow);
        int yd = (int) (yPos + ydd * j / pow);

        // If the blood encounters a wall, stop spraying.
        if (map.isAnyWallSafe(xd, yd)) {
          break bloodLoop;
        }

        // Occasionally splat some blood and darken it.
        if (random.nextInt(2) != 0) {
          map.setElementSafe(xd, yd, col << 16);
          col = col * 8 / 9;
        }
      }
    }
  }

  void setAsPlayer(Point startPoint) {
    position = startPoint;
    setSavedMapPixel(0x808080);
    markActive();
  }

  void setDirection(int direction) {
    this.direction = direction;
  }

  private void setSavedMapPixel(int savedMapPixel) {
    this.savedMapPixel = savedMapPixel;
  }

  void wanderToward(Point distanceToPlayer) {
    if (wanderDirection != 12) {
      distanceToPlayer.x = (wanderDirection) % 5 - 2;
      distanceToPlayer.y = (wanderDirection) / 5 - 2;
      if (random.nextInt(10) == 0) {
        wanderDirection = 12;
      }
    }
  }
}