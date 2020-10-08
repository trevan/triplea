package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import java.util.function.Function;
import lombok.Value;

@Value(staticConstructor = "with")
public class AaOffenseStrengthGetter
    implements Function<Unit, TotalPowerAndTotalRolls.ValueAndSupportAllowances> {

  GameData gameData;

  @Override
  public TotalPowerAndTotalRolls.ValueAndSupportAllowances apply(final Unit unit) {
    return TotalPowerAndTotalRolls.ValueAndSupportAllowances.of(
        DiceValue.of(
            gameData,
            DiceValue.Type.STRENGTH,
            unit.getUnitAttachment().getOffensiveAttackAa(unit.getOwner())),
        true,
        UnitSupportAttachment::getAaStrength);
  }
}
