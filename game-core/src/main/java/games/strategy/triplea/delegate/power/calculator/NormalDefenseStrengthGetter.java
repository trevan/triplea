package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import java.util.Collection;
import java.util.function.Function;
import lombok.Value;

@Value(staticConstructor = "with")
class NormalDefenseStrengthGetter
    implements Function<Unit, TotalPowerAndTotalRolls.ValueAndSupportAllowances> {

  Collection<TerritoryEffect> territoryEffects;
  GameData gameData;

  @Override
  public TotalPowerAndTotalRolls.ValueAndSupportAllowances apply(final Unit unit) {
    int strength = unit.getUnitAttachment().getDefense(unit.getOwner());
    boolean allowFriendly = true;
    if (isFirstTurnLimitedRoll(unit.getOwner(), gameData)) {
      // if first turn is limited, the strength is a max of 1 and no friendly support
      strength = Math.min(1, strength);
      allowFriendly = false;
    }
    strength +=
        TerritoryEffectHelper.getTerritoryCombatBonus(unit.getType(), territoryEffects, true);

    return TotalPowerAndTotalRolls.ValueAndSupportAllowances.of(
        DiceValue.of(gameData, DiceValue.Type.STRENGTH, strength),
        allowFriendly,
        UnitSupportAttachment::getStrength);
  }

  private static boolean isFirstTurnLimitedRoll(final GamePlayer player, final GameData data) {
    // If player is null, Round > 1, or player has negate rule set: return false
    return !player.isNull()
        && data.getSequence().getRound() == 1
        && !isNegateDominatingFirstRoundAttack(player)
        && isDominatingFirstRoundAttack(data.getSequence().getStep().getPlayerId());
  }

  private static boolean isNegateDominatingFirstRoundAttack(final GamePlayer player) {
    final RulesAttachment ra =
        (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
    return ra != null && ra.getNegateDominatingFirstRoundAttack();
  }

  private static boolean isDominatingFirstRoundAttack(final GamePlayer player) {
    if (player == null) {
      return false;
    }
    final RulesAttachment ra =
        (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
    return ra != null && ra.getDominatingFirstRoundAttack();
  }
}
