import java.awt.Graphics;
import java.util.Random;


class Game {
  // Fields that are reset during attract.
  int score;
  int hurtTime; // Makes the screen red when player is getting bitten.
  int bonusTime;

  // Fields that are reset when entering attract.
  boolean gameStarted;
  int level;
  int shootDelay;
  int rushTime;
  int damage;
  int ammo;
  int clips;

  public Game() {
  }

  public void restart() {
    gameStarted = false;
    level = 0;
    shootDelay = 0;
    rushTime = 150;
    damage = 20;
    ammo = 20;
    clips = 20;
    System.out.println("Entering attract...");
  }

  public void winLevel() {
    ++level;
    System.out.println("Advancing to level " + level + "...");
  }

  public void drawLevel(Graphics ogr) {
    ogr.drawString("Level " + level, 90, 70);
  }

  public void addScoreForMonsterDeath() {
    score += level;
  }

  public void markGameStarted() {
    score = 0;
    gameStarted = true;
    System.out.println("Starting new game...");
  }

  public void advanceRushTime(Random random) {
    if (++rushTime >= 150) {
      rushTime = -random.nextInt(2000);
    }
  }
}