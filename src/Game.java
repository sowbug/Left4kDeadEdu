import java.awt.Graphics;
import java.util.Random;

class Game {
  // Fields that are reset during attract.
  private int score;
  private int hurtTime; // Makes the screen red when player is getting bitten.
  private int bonusTime;

  // Fields that are reset when entering attract.
  private boolean isStarted;
  private int level;
  private int shootDelay;
  private int rushTime;
  private int damage;
  private int ammo;
  private int clips;
  private int tick;

  void addScoreForMonsterDeath() {
    score += level;
  }

  void advanceRushTime(Random random) {
    if (++rushTime >= 150) {
      rushTime = -random.nextInt(2000);
    }
  }

  // Returns true if OK to shoot now.
  boolean advanceShotTimer() {
    return shootDelay-- < 0;
  }

  void advanceTick() {
    tick++;
  }

  void consumeAmmo(int amount) {
    ammo += amount;
  }

  void decayBonusTime() {
    bonusTime = bonusTime * 8 / 9;
  }

  void decayHurt() {
    hurtTime /= 2;
  }

  void drawLevel(Graphics ogr) {
    ogr.drawString("Level " + level, 90, 70);
  }

  int getAmmo() {
    return this.ammo;
  }

  int getBonusTime() {
    return this.bonusTime;
  }

  int getClips() {
    return this.clips;
  }

  int getDamage() {
    return this.damage;
  }

  int getHurtTime() {
    return this.hurtTime;
  }

  int getLevel() {
    return this.level;
  }

  int getRushTime() {
    return this.rushTime;
  }

  int getScore() {
    return this.score;
  }

  int getShootDelay() {
    return this.shootDelay;
  }

  int getTick() {
    return this.tick;
  }

  void inflictNibbleDamage() {
    damage++;
    hurtTime += 20;
  }

  boolean isAmmoAvailable() {
    return getAmmo() < 220;
  }

  boolean isAmmoFull() {
    return getAmmo() <= 20;
  }

  boolean isStarted() {
    return this.isStarted;
  }

  void markGameStarted() {
    score = 0;
    isStarted = true;
    System.out.println("Starting new game...");
  }

  Random randomForLevel() {
    return new Random(4329 + level);
  }

  void reloadGun() {
    shootDelay = 30;
    ammo = 20;
    clips += 10;
  }

  void resetBonusTime() {
    bonusTime = 120;
  }

  void resetClips() {
    clips = 20;
  }

  void resetDamage() {
    damage = 20;
  }

  void restart() {
    isStarted = false;
    level = 0;
    shootDelay = 0;
    rushTime = 150;
    damage = 20;
    ammo = 20;
    clips = 20;
    System.out.println("Entering attract...");
  }

  void setLongShootDelay() {
    shootDelay = 2;
  }

  void setMaxHurt() {
    hurtTime = 255;
  }

  void setShortShootDelay() {
    shootDelay = 1;
  }

  void winLevel() {
    ++level;
    tick = 0;
    System.out.println("Advancing to level " + level + "...");
  }
}