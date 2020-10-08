package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import lombok.Value;

@Value(staticConstructor = "of")
class DiceValue {
  enum Type {
    STRENGTH,
    ROLL;
  }

  GameData gameData;
  Type type;
  int value;
  boolean isInfinite;

  static DiceValue of(final GameData gameData, final Type type, final int value) {
    return DiceValue.of(gameData, type, value, type == Type.ROLL && value == -1);
  }

  DiceValue add(final int extraValue) {
    if (isInfinite) {
      return this;
    } else {
      return DiceValue.of(gameData, type, value + extraValue, false);
    }
  }

  int minMax() {
    if (isInfinite) {
      return -1;
    } else if (type == Type.STRENGTH) {
      return Math.min(Math.max(value, 0), gameData.getDiceSides());
    } else {
      // rolls don't have a maximum
      return Math.max(0, value);
    }
  }
}
