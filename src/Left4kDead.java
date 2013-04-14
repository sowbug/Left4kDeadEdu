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
  private static final int MAX_MONSTERS = 320;
  private static final int VIEWPORT_WIDTH = 240;
  private static final int VIEWPORT_WIDTH_HALF = VIEWPORT_WIDTH / 2;

  private static final int VIEWPORT_HEIGHT = 240;
  private static final int VIEWPORT_HEIGHT_HALF = VIEWPORT_HEIGHT / 2;

  private static final int SCREEN_WIDTH = VIEWPORT_WIDTH * 2;

  private static final int SCREEN_HEIGHT = VIEWPORT_HEIGHT * 2;

  private static final long serialVersionUID = 2099860140043826270L;

  Random random;
  private Map map;
  private final Game game;
  private final UserInput userInput;

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
    game = new Game();
  }

  public void run() {
    while (true) {
      game.restart();
      runGameLoop();
    }
  }

  private void runGameLoop() {
    while (true) {
      game.winLevel();
      Point endRoomTopLeft = new Point(0, 0);
      Point endRoomBottomRight = new Point(0, 0);

      // Make the levels random but repeatable.
      random = game.randomForLevel();

      map = new Map(1024, 1024);

      Point startPoint = new Point(0, 0);
      map.generate(random, startPoint, endRoomTopLeft, endRoomBottomRight);

      Monster[] monsters = new Monster[MAX_MONSTERS];
      for (int i = 0; i < MAX_MONSTERS; ++i) {
        monsters[i] = new Monster(i);
      }

      // Place the player (monsterData[0-15]) in the center of the start room.
      monsters[0].setAsPlayer(startPoint);

      long lastTime = System.nanoTime();

      Viewport viewport = new Viewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT,
          SCREEN_WIDTH, SCREEN_HEIGHT, getGraphics());

      double playerDir = 0;

      while (true) {
        if (game.isStarted()) {
          game.advanceTick();
          game.advanceRushTime(random);

          int mouse = userInput.mouseEvent;
          playerDir = Math.atan2(mouse / VIEWPORT_WIDTH - VIEWPORT_WIDTH_HALF,
              mouse % VIEWPORT_HEIGHT - VIEWPORT_HEIGHT_HALF);

          double shootDir = playerDir
              + (random.nextInt(100) - random.nextInt(100)) / 100.0 * 0.2;
          double cos = Math.cos(-shootDir);
          double sin = Math.sin(-shootDir);

          Point camera = monsters[0].position;

          viewport.prepareFrame(game, map, camera, playerDir);

          resetClosestHitDistance(cos, sin, camera);
          processMonsters(viewport, game.getTick(), monsters, playerDir, cos,
              sin, camera);

          if (didPlayerPressFire()) {
            boolean wasMonsterHit = closestHit > 0;
            closestHitDistance = viewport.handleShot(game, userInput,
                wasMonsterHit, playerDir, cos, sin, closestHitDistance);
            if (wasMonsterHit) {
              monsters[closestHit].markDamaged();
              monsters[closestHit].markEnraged();
            }
          }

          if (game.getDamage() >= 220) {
            userInput.setTriggerPressed(false);
            game.setMaxHurt();
            return;
          }
          if (userInput.isReloadPressed() && !game.isAmmoFull()
              && game.getClips() < 220) {
            game.reloadGun();
          }

          if (isPlayerInEndRoom(endRoomTopLeft, endRoomBottomRight, camera)) {
            System.out.println("You made it!");
            break;
          }
        }

        game.decayBonusTime();
        game.decayHurt();

        viewport.completeFrame(game, userInput);

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

  private void processMonsters(Viewport viewport, int tick, Monster[] monsters,
      double playerDir, double cos, double sin, Point camera) {
    for (int i = 0; i < 256 + 16; ++i) {
      processMonster(viewport, tick, monsters[i], playerDir, cos, sin, camera);
    }
  }

  private boolean didPlayerPressFire() {
    return game.advanceShotTimer() && userInput.isTriggerPressed();
  }

  private void updateClosestHit(int index, int distance) {
    if (distance < closestHitDistance) {
      closestHit = index;
      closestHitDistance = distance;
    }
  }

  private void processMonster(Viewport viewport, int tick, Monster monster,
      double playerDir, double cos, double sin, Point camera) {
    int xPos = monster.position.x;
    int yPos = monster.position.y;

    if (!monster.isActive()) {
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
          && (monster.isEarly() || game.getRushTime() > 0 || (monster
              .isSpecial() && tick == 1))) {
        monster.place(xPos, yPos, game.getRushTime(),
            map.getElement(xPos, yPos));
        // Mark the map as having a monster here.
        map.setMonsterHead(xPos, yPos);
      } else {
        return;
      }
    } else {
      Point distance = new Point(camera.x - xPos, camera.y - yPos);

      if (monster.isSpecial()) {
        if (isTouchingPlayer(distance)) {
          killMonster(monster);
          return;
        }
      } else {
        if (isOutOfView(distance)) {
          // Not a special monster. If it wandered too far from the player,
          // or more likely the player wandered too far from it, kill it.
          // Basically, this keeps the player reasonably surrounded with
          // monsters waiting to come to life without wasting too many
          // resources on idle ones.
          killMonster(monster);
          return;
        }
      }
    }

    viewport.drawMonster(tick, monster, playerDir, camera, xPos);

    boolean moved = false;

    if (monster.hasTakenDamage()) {
      monster.processDamage(map, game, playerDir, xPos, yPos);
      return;
    }

    Point distanceToPlayer = new Point(camera.x - xPos, camera.y - yPos);

    if (!monster.isSpecial()) {
      // Calculate distance to player.
      double rx = -(cos * distanceToPlayer.x - sin * distanceToPlayer.y);
      double ry = cos * distanceToPlayer.y + sin * distanceToPlayer.x;

      // Is this monster near the player?
      if (monster.isMouthTouchingPlayer(rx, ry)) {
        game.inflictNibbleDamage();
      }

      if (monster.canSeePlayer(rx, ry) && random.nextInt(10) == 0) {
        monster.agitate();
      }

      // Mark which monster so far is closest to the player.
      if (rx > 0 && ry > -8 && ry < 8) {
        updateClosestHit(monster.getIndex(), (int) rx);
      }

      for (int i = 0; i < 2; i++) {
        Boolean shouldSkip = new Boolean(false);
        moved = doOneMovementIteration(monster, moved, i, shouldSkip,
            distanceToPlayer);
        if (shouldSkip)
          return;
      }
      if (moved) {
        monster.advanceSpriteFrame();
      }
    }
  }

  private boolean isTooCloseToSpawn(Point distance) {
    return distance.x * distance.x + distance.y * distance.y < 180 * 180;
  }

  private boolean isOutOfView(Point distance) {
    return distance.x * distance.x + distance.y * distance.y > 340 * 340;
  }

  private boolean isTouchingPlayer(Point distance) {
    return distance.x * distance.x + distance.y * distance.y < 8 * 8;
  }

  private void killMonster(Monster monster) {
    // Replace the map pixel.
    map.setElement(monster.position.x, monster.position.y,
        monster.getSavedMapPixel());

    // Mark monster inactive.
    monster.markInactive();

    if (monster.isSpecial()) {
      game.resetBonusTime();

      // 50-50 chance of resetting damage or giving ammo.
      if ((monster.getIndex() & 1) == 0) {
        game.resetDamage();
      } else {
        game.resetClips();
      }
    }
  }

  private boolean doOneMovementIteration(Monster monster, boolean moved,
      int iteration, Boolean shouldSkip, Point distanceToPlayer) {
    Point movement = new Point(0, 0);

    if (monster.isPlayer()) {
      userInput.handleKeyboardInput(movement);
    } else {
      // Not agitated enough. Don't do anything.
      if (!monster.isSomewhatEnraged()) {
        shouldSkip = true;
        return false;
      }

      monster.wanderToward(distanceToPlayer);

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
      monster.setDirection((((int) (dir / (Math.PI * 2) * 16 + 4.5 + 16)) & 15));
    }

    // I think this is a way to move fast but not go through walls.
    // Start by moving a small amount, test for wall hit, if successful
    // try moving more.
    movement.y *= iteration;
    movement.x *= 1 - iteration;

    if (didMove(movement)) {
      // Restore the map pixel during collision detection.
      map.setElement(monster.position.x, monster.position.y,
          monster.getSavedMapPixel());

      // Did the monster bonk into a wall?
      for (int xx = monster.position.x + movement.x - 3; xx <= monster.position.x
          + movement.x + 3; xx++) {
        for (int yy = monster.position.y + movement.y - 3; yy <= monster.position.y
            + movement.y + 3; yy++) {
          if (map.isWall(xx, yy)) {
            // Yes. We're not moving. Put back our pixel.
            map.setMonsterHead(monster.position.x, monster.position.y);
            // Try wandering in a different direction.
            monster.pickWanderDirection();
            return moved;
          }
        }
      }

      // Move the monster.
      moved = true;
      monster.move(map, movement);
    }

    return moved;
  }

  private boolean didMove(Point movement) {
    return movement.x != 0 || movement.y != 0;
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
