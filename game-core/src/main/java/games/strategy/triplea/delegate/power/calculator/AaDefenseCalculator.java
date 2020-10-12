package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

/**
 * Calculates defense strength and roll for AA/Targeted dice
 *
 * <p>This takes into account friendly support, and enemy support
 */
@Builder
@Value
@Getter(AccessLevel.NONE)
public class AaDefenseCalculator implements OffenseOrDefenseCalculator {

  GameData data;
  AvailableSupportTracker friendlySupportTracker;
  AvailableSupportTracker enemySupportTracker;

  @Override
  public StrengthOrRollCalculator getRoll() {
    return new AaDefenseRoll(friendlySupportTracker, enemySupportTracker);
  }

  @Override
  public StrengthOrRollCalculator getStrength() {
    return new AaDefenseStrength(data, friendlySupportTracker, enemySupportTracker);
  }

  static class AaDefenseRoll extends StrengthOrRollCalculator {

    AaDefenseRoll(
        final AvailableSupportTracker friendlySupport, final AvailableSupportTracker enemySupport) {
      super(friendlySupport, enemySupport);
    }

    @Override
    public int getValue(final Unit unit) {
      final RollValue rollValue =
          RollValue.of(unit.getUnitAttachment().getMaxAaAttacks())
              .add(addSupport(unit, friendlySupportTracker))
              .add(addSupport(unit, enemySupportTracker));
      return rollValue.getValue();
    }

    @Override
    Predicate<UnitSupportAttachment> getRuleFilter() {
      return UnitSupportAttachment::getAaRoll;
    }
  }

  static class AaDefenseStrength extends StrengthOrRollCalculator {

    private final GameData gameData;

    AaDefenseStrength(
        final GameData gameData,
        final AvailableSupportTracker friendlySupport,
        final AvailableSupportTracker enemySupport) {
      super(friendlySupport, enemySupport);
      this.gameData = gameData;
    }

    @Override
    public int getValue(final Unit unit) {
      final StrengthValue strengthValue =
          StrengthValue.of(
                  gameData.getDiceSides(), unit.getUnitAttachment().getAttackAa(unit.getOwner()))
              .add(addSupport(unit, friendlySupportTracker))
              .add(addSupport(unit, enemySupportTracker));
      return strengthValue.getValue();
    }

    @Override
    Predicate<UnitSupportAttachment> getRuleFilter() {
      return UnitSupportAttachment::getAaStrength;
    }
  }
}
