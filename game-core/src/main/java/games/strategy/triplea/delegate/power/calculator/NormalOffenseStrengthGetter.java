package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import java.util.Collection;
import java.util.function.Function;
import lombok.Value;

@Value(staticConstructor = "with")
class NormalOffenseStrengthGetter
    implements Function<Unit, TotalPowerAndTotalRolls.ValueAndSupportAllowances> {

  boolean territoryIsLand;
  Collection<TerritoryEffect> territoryEffects;
  GameData gameData;

  @Override
  public TotalPowerAndTotalRolls.ValueAndSupportAllowances apply(final Unit unit) {
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
    strength +=
        TerritoryEffectHelper.getTerritoryCombatBonus(unit.getType(), territoryEffects, false);

    return TotalPowerAndTotalRolls.ValueAndSupportAllowances.of(
        DiceValue.of(gameData, DiceValue.Type.STRENGTH, strength),
        true,
        UnitSupportAttachment::getStrength);
  }
}
