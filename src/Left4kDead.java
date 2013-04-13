/**
 * Comment free (yay) source code for Left 4k Dead by Markus Persson
 * Please don't reuse any of this code in other projects.
 * http://www.mojang.com/notch/j4k/l4kd/
 */
import java.awt.AWTEvent;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.Random;

public class Left4kDead extends Frame {
  private static final int VIEWPORT_WIDTH = 240;
  private static final int VIEWPORT_WIDTH_HALF = VIEWPORT_WIDTH / 2;

  private static final int VIEWPORT_HEIGHT = 240;
  private static final int VIEWPORT_HEIGHT_HALF = VIEWPORT_HEIGHT / 2;

  private static final int SCREEN_WIDTH = VIEWPORT_WIDTH * 2;

  private static final int SCREEN_HEIGHT = VIEWPORT_HEIGHT * 2;

  private static final long serialVersionUID = 2099860140043826270L;

  private static final int MDO_X = 0;
  private static final int MDO_Y = 1;
  private static final int MDO_DIRECTION = 2;
  private static final int MDO_SPRITE_FRAME = 3;
  private static final int MDO_WANDER_DIRECTION = 8;
  private static final int MDO_RAGE_LEVEL = 9;
  private static final int MDO_DAMAGE_TAKEN = 10;
  private static final int MDO_ACTIVITY_LEVEL = 11;
  private static final int MDO_SAVED_MAP_PIXEL = 15;

  private Random random;
  private Map map;
  private Game game;
  private UserInput userInput;

  private int closestHit;
  private int closestHitDistance;

  public static void main(String[] args) {
    Left4kDead left4kDead = new Left4kDead();
    left4kDead.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
    left4kDead.setVisible(true);
    left4kDead.setLayout(new FlowLayout());
    left4kDead.run();
  }

  public void windowClosing(WindowEvent e) {
    dispose();
    System.exit(0);
  }

  public void windowOpened(WindowEvent e) {
  }

  public void windowIconified(WindowEvent e) {
  }

  public void windowClosed(WindowEvent e) {
  }

  public void windowDeiconified(WindowEvent e) {
  }

  public void windowActivated(WindowEvent e) {
  }

  public void windowDeactivated(WindowEvent e) {
  }

  public Left4kDead() {
    enableEvents(AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK
        | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    userInput = new UserInput();
  }

  public void run() {
    random = new Random();

    game = new Game();

    while (true) {
      game.restart();
      playUntilPlayerDies();
    }
  }

  private void playUntilPlayerDies() {
    while (true) {
      int tick = 0;
      game.winLevel();
      Point endRoomTopLeft = new Point(0, 0);
      Point endRoomBottomRight = new Point(0, 0);

      // Make the levels random but repeatable.
      random = new Random(4329 + game.level);

      map = new Map(1024, 1024);

      Point startPoint = new Point(0, 0);
      map.generate(random, startPoint, endRoomTopLeft, endRoomBottomRight);

      int[] monsterData = new int[320 * 16];

      // Place the player (monsterData[0-15]) in the center of the start room.
      monsterData[MDO_X] = startPoint.x;
      monsterData[MDO_Y] = startPoint.y;
      monsterData[MDO_SAVED_MAP_PIXEL] = 0x808080;
      monsterData[MDO_ACTIVITY_LEVEL] = 1;

      long lastTime = System.nanoTime();

      Viewport viewport = new Viewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT,
          getGraphics());

      double playerDir = 0;

      while (true) {
        if (game.gameStarted) {
          tick++;
          game.advanceRushTime(random);

          int mouse = userInput.mouseEvent;
          playerDir = Math.atan2(mouse / VIEWPORT_WIDTH - VIEWPORT_WIDTH_HALF,
              mouse % VIEWPORT_HEIGHT - VIEWPORT_HEIGHT_HALF);

          double shootDir = playerDir
              + (random.nextInt(100) - random.nextInt(100)) / 100.0 * 0.2;
          double cos = Math.cos(-shootDir);
          double sin = Math.sin(-shootDir);

          Point camera = new Point(monsterData[MDO_X], monsterData[MDO_Y]);

          viewport.generateLightmap(map, tick, playerDir, camera);
          viewport.copyView(map, camera);

          resetClosestHitDistance(cos, sin, camera);
          processMonsters(viewport, tick, monsterData, playerDir, cos, sin,
              camera);

          if (didPlayerPressFire()) {
            boolean wasMonsterHit = closestHit > 0;
            doShot(viewport, wasMonsterHit, playerDir, cos, sin);
            if (wasMonsterHit) {
              monsterData[closestHit * 16 + MDO_DAMAGE_TAKEN] = 1;
              monsterData[closestHit * 16 + MDO_RAGE_LEVEL] = 127;
            }
          }

          if (game.damage >= 220) {
            userInput.setTriggerPressed(false);
            game.hurtTime = 255;
            return;
          }
          if (userInput.isReloadPressed() && game.ammo > 20 && game.clips < 220) {
            game.shootDelay = 30;
            game.ammo = 20;
            game.clips += 10;
          }

          if (isPlayerInEndRoom(endRoomTopLeft, endRoomBottomRight, camera)) {
            System.out.println("You made it!");
            break;
          }
        }

        game.bonusTime = game.bonusTime * 8 / 9;
        game.hurtTime /= 2;

        viewport.drawNoiseAndHUD(game);

        viewport.drawStatusText(tick, game, userInput);
        viewport.drawToScreen(SCREEN_WIDTH, SCREEN_HEIGHT);

        do {
          Thread.yield();
        } while (System.nanoTime() - lastTime < 0);
        if (!isActive())
          return;

        lastTime += (1000000000 / 30);
      }
    }
  }

  private boolean isPlayerInEndRoom(Point endRoomTopLeft,
      Point endRoomBottomRight, Point camera) {
    return camera.x > endRoomTopLeft.x && camera.x < endRoomBottomRight.x
        && camera.y > endRoomTopLeft.y && camera.y < endRoomBottomRight.y;
  }

  private void resetClosestHitDistance(double cos, double sin, Point camera) {
    int distance = 0;
    for (int j = 0; j < VIEWPORT_WIDTH + 10; j++) {
      int xm = camera.x + (int) (cos * j / 2);
      int ym = camera.y - (int) (sin * j / 2);

      if (map.isMonsterSafe(xm, ym))
        break;
      distance = j / 2;
    }
    closestHit = 0;
    closestHitDistance = distance;
  }

  private void processMonsters(Viewport viewport, int tick, int[] monsterData,
      double playerDir, double cos, double sin, Point camera) {
    for (int monsterIndex = 0; monsterIndex < 256 + 16; monsterIndex++) {
      processMonster(viewport, tick, monsterData, playerDir, cos, sin, camera,
          monsterIndex);
    }
  }

  private boolean didPlayerPressFire() {
    return game.shootDelay-- < 0 && userInput.isTriggerPressed();
  }

  private void updateClosestHit(int index, int distance) {
    if (distance < closestHitDistance) {
      closestHit = index;
      closestHitDistance = distance;
    }
  }

  private void processMonster(Viewport viewport, int tick, int[] monsterData,
      double playerDir, double cos, double sin, Point camera, int monsterIndex) {
    int xPos = monsterData[monsterIndex * 16 + MDO_X];
    int yPos = monsterData[monsterIndex * 16 + MDO_Y];

    if (!isMonsterActive(monsterData, monsterIndex)) {
      // Try to activate it.

      // Pick a random spot to put it.
      xPos = (random.nextInt(62) + 1) * 16 + 8;
      yPos = (random.nextInt(62) + 1) * 16 + 8;

      Point distance = new Point(camera.x - xPos, camera.y - yPos);
      if (isTooCloseToSpawn(distance)) {
        // Too close. Not fair. So put the monster inside a wall. I don't
        // understand why this isn't just a continue;
        xPos = 1;
        yPos = 1;
      }

      // Are all these true?
      // 1. The monster is not on a wall or other monster, AND
      // 2. Any of these is true: a. It's an early-numbered monster, OR b.
      // It's rush time, OR c. It's the first tick of the game and it's one
      // of the last 16 monsters.
      if (!map.isMonsterHead(xPos, yPos)
          && (monsterIndex <= 128 || game.rushTime > 0 || (isSpecialMonster(monsterIndex) && tick == 1))) {
        placeNewMonster(monsterData, monsterIndex, xPos, yPos);
      } else {
        return;
      }
    } else {
      Point distance = new Point(camera.x - xPos, camera.y - yPos);

      if (isSpecialMonster(monsterIndex)) {
        if (isTouchingPlayer(distance)) {
          killSpecialMonster(monsterData, monsterIndex, xPos, yPos);
          return;
        }
      } else if (isOutOfView(distance)) {
        recycleOutOfViewMonster(monsterData, monsterIndex, xPos, yPos);
        return;
      }
    }

    viewport.drawMonster(tick, monsterData, playerDir, camera, monsterIndex,
        xPos);

    boolean moved = false;

    if (hasMonsterTakenDamage(monsterData, monsterIndex)) {
      processMonsterDamage(monsterData, playerDir, monsterIndex, xPos, yPos);
      return;
    }

    Point distanceToPlayer = new Point(camera.x - xPos, camera.y - yPos);

    if (!isSpecialMonster(monsterIndex)) {
      // Calculate distance to player.
      double rx = -(cos * distanceToPlayer.x - sin * distanceToPlayer.y);
      double ry = cos * distanceToPlayer.y + sin * distanceToPlayer.x;

      // Is this monster near the player?
      if (isMonsterMouthTouchingPlayer(monsterIndex, rx, ry)) {
        inflictNibbleDamage();
      }

      if (canMonsterSeePlayer(rx, ry) && random.nextInt(10) == 0) {
        agitateMonster(monsterData, monsterIndex);
      }

      // Mark which monster so far is closest to the player.
      if (rx > 0 && ry > -8 && ry < 8) {
        updateClosestHit(monsterIndex, (int) rx);
      }

      for (int i = 0; i < 2; i++) {
        Boolean shouldSkip = new Boolean(false);
        moved = doDirLoop(monsterData, monsterIndex, moved, i, shouldSkip,
            distanceToPlayer);
        if (shouldSkip)
          return;
      }
      if (moved) {
        // Shuffle to next frame in sprite.
        monsterData[monsterIndex * 16 + MDO_SPRITE_FRAME]++;
      }
    }
    return;
  }

  private boolean hasMonsterTakenDamage(int[] monsterData, int monsterIndex) {
    return monsterData[monsterIndex * 16 + MDO_DAMAGE_TAKEN] > 0;
  }

  private boolean isMonsterMouthTouchingPlayer(int monsterIndex, double rx,
      double ry) {
    return rx > -6 && rx < 6 && ry > -6 && ry < 6 && monsterIndex > 0;
  }

  private void agitateMonster(int[] monsterData, int monsterIndex) {
    monsterData[monsterIndex * 16 + MDO_RAGE_LEVEL]++;
  }

  private boolean canMonsterSeePlayer(double rx, double ry) {
    return rx > -32 && rx < 220 && ry > -32 && ry < 32;
  }

  private void inflictNibbleDamage() {
    game.damage++;
    game.hurtTime += 20;
  }

  private boolean isMonsterActive(int[] monsterData, int monsterIndex) {
    return monsterData[monsterIndex * 16 + MDO_ACTIVITY_LEVEL] != 0;
  }

  private boolean isTooCloseToSpawn(Point distance) {
    return distance.x * distance.x + distance.y * distance.y < 180 * 180;
  }

  private void recycleOutOfViewMonster(int[] monsterData, int monsterIndex,
      int xPos, int yPos) {
    // Not a special monster. If it wandered too far from the player,
    // or more likely the player wandered too far from it, kill it.
    // Basically, this keeps the player reasonably surrounded with
    // monsters waiting to come to life without wasting too many
    // resources on idle ones.
    map.setElement(xPos, yPos, monsterData[monsterIndex * 16
        + MDO_SAVED_MAP_PIXEL]);
    monsterData[monsterIndex * 16 + MDO_ACTIVITY_LEVEL] = 0;

  }

  private boolean isOutOfView(Point distance) {
    return distance.x * distance.x + distance.y * distance.y > 340 * 340;
  }

  private boolean isTouchingPlayer(Point distance) {
    return distance.x * distance.x + distance.y * distance.y < 8 * 8;
  }

  private void killSpecialMonster(int[] monsterData, int monsterIndex,
      int xPos, int yPos) {
    // Yes. Kill it.

    // Replace the map pixel.
    map.setElement(xPos, yPos, monsterData[monsterIndex * 16
        + MDO_SAVED_MAP_PIXEL]);
    // Mark monster inactive.
    monsterData[monsterIndex * 16 + MDO_ACTIVITY_LEVEL] = 0;
    game.bonusTime = 120;

    // 50-50 chance of resetting damage or giving ammo.
    if ((monsterIndex & 1) == 0) {
      game.damage = 20;
    } else {
      game.clips = 20;
    }
  }

  private boolean isSpecialMonster(int monsterIndex) {
    return monsterIndex >= 255;
  }

  private boolean doDirLoop(int[] monsterData, int monsterIndex, boolean moved,
      int i, Boolean shouldSkip, Point distanceToPlayer) {
    Point position = new Point(monsterData[monsterIndex * 16 + MDO_X],
        monsterData[monsterIndex * 16 + MDO_Y]);
    Point movement = new Point(0, 0);

    if (isPlayer(monsterIndex)) {
      userInput.handleKeyboardInput(movement);
    } else {
      // Not agitated enough. Don't do anything.
      if (monsterData[monsterIndex * 16 + MDO_RAGE_LEVEL] < 8) {
        shouldSkip = true;
        return false;
      }

      // Unsure. Seems to be some kind of wandering algorithm.
      if (monsterData[monsterIndex * 16 + MDO_WANDER_DIRECTION] != 12) {
        distanceToPlayer.x = (monsterData[monsterIndex * 16
            + MDO_WANDER_DIRECTION]) % 5 - 2;
        distanceToPlayer.y = (monsterData[monsterIndex * 16
            + MDO_WANDER_DIRECTION]) / 5 - 2;
        if (random.nextInt(10) == 0) {
          monsterData[monsterIndex * 16 + MDO_WANDER_DIRECTION] = 12;
        }
      }

      // Move generally toward the player.
      double xxd = Math.sqrt(distanceToPlayer.x * distanceToPlayer.x);
      double yyd = Math.sqrt(distanceToPlayer.y * distanceToPlayer.y);
      if (random.nextInt(1024) / 1024.0 < yyd / xxd) {
        if (distanceToPlayer.y < 0)
          movement.y--;
        if (distanceToPlayer.y > 0)
          movement.y++;
      }
      if (random.nextInt(1024) / 1024.0 < xxd / yyd) {
        if (distanceToPlayer.x < 0)
          movement.x--;
        if (distanceToPlayer.x > 0)
          movement.x++;
      }

      // Mark that the monster moved so we can update pixels later.
      moved = true;

      // Pick the right sprite frame depending on direction.
      double dir = Math.atan2(distanceToPlayer.y, distanceToPlayer.x);
      monsterData[monsterIndex * 16 + MDO_DIRECTION] = (((int) (dir
          / (Math.PI * 2) * 16 + 4.5 + 16)) & 15);
    }

    // I think this is a way to move fast but not go through walls.
    // Start by moving a small amount, test for wall hit, if successful
    // try moving more.
    movement.y *= i;
    movement.x *= 1 - i;

    if (didMove(movement)) {
      // Restore the map pixel.
      map.setElement(position.x, position.y, monsterData[monsterIndex * 16
          + MDO_SAVED_MAP_PIXEL]);

      // Did the monster bonk into a wall?
      for (int xx = position.x + movement.x - 3; xx <= position.x + movement.x
          + 3; xx++) {
        for (int yy = position.y + movement.y - 3; yy <= position.y
            + movement.y + 3; yy++) {
          if (map.isWall(xx, yy)) {
            // Yes. Put back the pixel.
            map.setElement(position.x, position.y, 0xfffffe);
            // Try wandering in a different direction.
            monsterData[monsterIndex * 16 + MDO_WANDER_DIRECTION] = random
                .nextInt(25);
            return moved;
          }
        }
      }

      // Move the monster.
      moved = true;
      monsterData[monsterIndex * 16 + MDO_X] += movement.x;
      monsterData[monsterIndex * 16 + MDO_Y] += movement.y;

      // Save the pixel.
      monsterData[monsterIndex * 16 + MDO_SAVED_MAP_PIXEL] = map.getElement(
          position.x + movement.x, position.y + movement.y);

      // Draw the monster's head.
      map.setElement(position.x + movement.x, position.y + movement.y, 0xfffffe);
    }

    return moved;
  }

  private boolean didMove(Point movement) {
    return movement.x != 0 || movement.y != 0;
  }

  private boolean isPlayer(int monsterIndex) {
    return monsterIndex == 0;
  }

  private void doShot(Viewport viewport, boolean wasMonsterHit,
      double playerDir, double cos, double sin) {
    // Is the ammo used up?
    if (game.ammo >= 220) {
      // Yes. Longer delay.
      game.shootDelay = 2;
      // Require trigger release.
      userInput.setTriggerPressed(false);
    } else {
      // Fast fire.
      game.shootDelay = 1;
      // Use up bullets.
      game.ammo += 4;
    }

    viewport.drawBulletTrace(cos, sin, closestHitDistance);

    // Did the bullet hit within view?
    if (closestHitDistance < VIEWPORT_WIDTH_HALF) {
      closestHitDistance -= 3;
      Point hitPoint = new Point((int) (VIEWPORT_WIDTH_HALF + cos
          * closestHitDistance), (int) (VIEWPORT_HEIGHT_HALF - sin
          * closestHitDistance));

      viewport.drawImpactFlash(hitPoint);
      viewport.drawBulletDebris(playerDir, wasMonsterHit, hitPoint);
    }
  }

  private void placeNewMonster(int[] monsterData, int m, int xPos, int yPos) {
    // Yes. Place the monster here.
    monsterData[m * 16 + MDO_X] = xPos;
    monsterData[m * 16 + MDO_Y] = yPos;

    // Remember this map pixel.
    monsterData[m * 16 + MDO_SAVED_MAP_PIXEL] = map.getElement(xPos, yPos);

    // Mark the map as having a monster here.
    map.setMonsterHead(xPos, yPos);

    // Mark monster as idle or attacking.
    monsterData[m * 16 + MDO_RAGE_LEVEL] = (game.rushTime > 0 || random
        .nextInt(3) == 0) ? 127 : 0;

    // Mark monster active.
    monsterData[m * 16 + MDO_ACTIVITY_LEVEL] = 1;

    // Distribute the monsters' initial direction.
    monsterData[m * 16 + MDO_DIRECTION] = m & 15;
  }

  private void processMonsterDamage(int[] monsterData, double playerDir, int m,
      int xPos, int yPos) {
    // Yes.
    // Add to monster's cumulative damage and reset temp damage.
    monsterData[m * 16 + MDO_ACTIVITY_LEVEL] += random.nextInt(3) + 1;
    monsterData[m * 16 + MDO_DAMAGE_TAKEN] = 0;

    double rot = 0.25; // How far around the blood spreads, radians
    int amount = 8; // How much blood
    double poww = 32; // How far to spread the blood

    // Is this monster sufficiently messed up to die?
    if (monsterData[m * 16 + MDO_ACTIVITY_LEVEL] >= 2 + game.level) {
      rot = Math.PI * 2; // All the way around
      amount = 60; // lots of blood
      poww = 16;
      map.setElement(xPos, yPos, 0xa00000); // Red
      monsterData[m * 16 + MDO_ACTIVITY_LEVEL] = 0; // Kill monster
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
        if (map.isAnyWallSafe(xd, yd))
          break bloodLoop;

        // Occasionally splat some blood and darken it.
        if (random.nextInt(2) != 0) {
          map.setElementSafe(xd, yd, col << 16);
          col = col * 8 / 9;
        }
      }
    }
  }

  /**
   * Scan key event and turn into a bitmap.
   */
  public void processEvent(AWTEvent e) {
    boolean down = false;
    switch (e.getID()) {
    case KeyEvent.KEY_PRESSED:
      down = true;
    case KeyEvent.KEY_RELEASED:
      userInput.setIsPressed(((KeyEvent) e).getKeyCode(), down);
      break;
    case MouseEvent.MOUSE_PRESSED:
      down = true;
    case MouseEvent.MOUSE_RELEASED:
      userInput.setTriggerPressed(down);
    case MouseEvent.MOUSE_MOVED:
    case MouseEvent.MOUSE_DRAGGED:
      userInput.mouseEvent = ((MouseEvent) e).getX() / 2
          + ((MouseEvent) e).getY() / 2 * VIEWPORT_HEIGHT;
    }
  }
}
