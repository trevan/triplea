package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import java.util.Collection;

/**
 * Calculates the value of a normal offensive dice roll
 *
 * <p>This takes into account marine, bombarding, territory effects, friendly support, and enemy
 * support
 */
public class NormalOffenseStrength extends StrengthOrRollCalculator {

  private final GameData gameData;
  private final Collection<TerritoryEffect> territoryEffects;
  private final boolean territoryIsLand;

  NormalOffenseStrength(
      final GameData gameData,
      final AvailableSupportCalculator friendlySupport,
      final AvailableSupportCalculator enemySupport,
      final Collection<TerritoryEffect> territoryEffects,
      final boolean territoryIsLand) {
    super(friendlySupport, enemySupport);
    this.gameData = gameData;
    this.territoryEffects = territoryEffects;
    this.territoryIsLand = territoryIsLand;
  }

  @Override
  public int getValue(final Unit unit) {
    final UnitAttachment ua = unit.getUnitAttachment();
    int strength = ua.getAttack(unit.getOwner());
    if (ua.getIsMarine() != 0 && unit.getWasAmphibious()) {
      strength += ua.getIsMarine();
    }
    if (ua.getIsSea() && territoryIsLand) {
      // Change the strength to be bombard, not attack/defense, because this is a bombarding
      // naval unit
      strength = ua.getBombard();
    }

    final StrengthValue strengthValue =
        StrengthValue.of(gameData.getDiceSides(), strength)
            .add(
                TerritoryEffectHelper.getTerritoryCombatBonus(
                    unit.getType(), territoryEffects, false))
            .add(addSupport(unit, friendlySupportTracker, UnitSupportAttachment::getStrength))
            .add(addSupport(unit, enemySupportTracker, UnitSupportAttachment::getStrength));
    return strengthValue.minMax();
  }
}
