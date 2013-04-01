import java.awt.event.KeyEvent;

class UserInput {
  private boolean[] k = new boolean[32767];
  int mouseEvent;
  private boolean isTriggerPressed;

  public boolean isTriggerPressed() {
    return isTriggerPressed;
  }

  public void setTriggerPressed(boolean isTriggerPressed) {
    this.isTriggerPressed = isTriggerPressed;
  }

  public UserInput() {
    isTriggerPressed = false;
  }

  void handleKeyboardInput(Point movement) {
    // Move the player according to keyboard state.
    if (k[KeyEvent.VK_A])
      movement.x--;
    if (k[KeyEvent.VK_D])
      movement.x++;
    if (k[KeyEvent.VK_W])
      movement.y--;
    if (k[KeyEvent.VK_S])
      movement.y++;
  }

  public boolean isReloadPressed() {
    return k[KeyEvent.VK_R];
  }

  public void setIsPressed(int keyCode, boolean isPressed) {
    k[keyCode] = isPressed;
  }
}