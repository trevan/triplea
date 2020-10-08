package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/**
 * Tracks the available support that a collection of units can give to other units.
 *
 * <p>Once a support is used, it will no longer be available for other units to use.
 */
@Data
@Builder(access = AccessLevel.PACKAGE)
@AllArgsConstructor
public class AvailableSupportTracker {

  public static final AvailableSupportTracker EMPTY_RESULT =
      AvailableSupportTracker.builder()
          .supportRules(new HashMap<>())
          .supportUnits(new HashMap<>())
          .supportLeft(new IntegerMap<>())
          .build();

  final Map<String, List<UnitSupportAttachment>> supportRules;
  final IntegerMap<UnitSupportAttachment> supportLeft;
  final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnits;

  AvailableSupportTracker(final AvailableSupportTracker availableSupportTracker) {

    final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnitsCopy = new HashMap<>();
    for (final UnitSupportAttachment usa : availableSupportTracker.supportUnits.keySet()) {
      supportUnitsCopy.put(usa, new IntegerMap<>(availableSupportTracker.supportUnits.get(usa)));
    }

    supportRules = availableSupportTracker.supportRules;
    supportLeft = new IntegerMap<>(availableSupportTracker.supportLeft);
    supportUnits = supportUnitsCopy;
  }

  static AvailableSupportTracker getSortedAaSupport(
      final Collection<Unit> unitsGivingTheSupport,
      final Set<UnitSupportAttachment> unFilteredRules,
      final boolean defence,
      final boolean allies) {
    final Set<UnitSupportAttachment> rules =
        unFilteredRules.parallelStream()
            .filter(usa -> (usa.getAaRoll() || usa.getAaStrength()))
            .collect(Collectors.toSet());
    return getSortedSupport(unitsGivingTheSupport, rules, defence, allies);
  }

  /** Sorts 'supportsAvailable' lists based on unit support attachment rules. */
  static AvailableSupportTracker getSortedSupport(
      final Collection<Unit> unitsGivingTheSupport,
      final Set<UnitSupportAttachment> rules,
      final boolean defence,
      final boolean allies) {
    final AvailableSupportTracker supportCalculationResult =
        getSupport(unitsGivingTheSupport, rules, defence, allies);

    final SupportRuleSort supportRuleSort =
        SupportRuleSort.builder()
            .defense(defence)
            .friendly(allies)
            .roll(UnitSupportAttachment::getRoll)
            .strength(UnitSupportAttachment::getStrength)
            .build();
    supportCalculationResult
        .getSupportRules()
        .forEach((name, unitSupportAttachment) -> unitSupportAttachment.sort(supportRuleSort));
    return supportCalculationResult;
  }

  /**
   * Constructs an AvailableSupportTracker to track the possible support given by the units
   *
   * @param defence are the receiving units defending?
   * @param allies are the receiving units allied to the giving units?
   */
  public static AvailableSupportTracker getSupport(
      final Collection<Unit> unitsGivingTheSupport,
      final Set<UnitSupportAttachment> rules,
      final boolean defence,
      final boolean allies) {
    if (unitsGivingTheSupport == null || unitsGivingTheSupport.isEmpty()) {
      return EMPTY_RESULT;
    }
    final Map<String, List<UnitSupportAttachment>> supportsAvailable = new HashMap<>();
    final IntegerMap<UnitSupportAttachment> supportLeft = new IntegerMap<>();
    final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnitsLeft = new HashMap<>();

    for (final UnitSupportAttachment rule : rules) {
      if (rule.getPlayers().isEmpty()) {
        continue;
      }
      if (!((defence && rule.getDefence()) || (!defence && rule.getOffence()))) {
        continue;
      }
      if (!((allies && rule.getAllied()) || (!allies && rule.getEnemy()))) {
        continue;
      }
      final Predicate<Unit> canSupport =
          Matches.unitIsOfType((UnitType) rule.getAttachedTo())
              .and(Matches.unitOwnedBy(rule.getPlayers()));
      final List<Unit> supporters = CollectionUtils.getMatches(unitsGivingTheSupport, canSupport);
      int numSupport = supporters.size();
      if (numSupport <= 0) {
        continue;
      }
      final List<Unit> impArtTechUnits = new ArrayList<>();
      if (rule.getImpArtTech()) {
        impArtTechUnits.addAll(
            CollectionUtils.getMatches(
                supporters, Matches.unitOwnerHasImprovedArtillerySupportTech()));
      }
      numSupport += impArtTechUnits.size();
      supportLeft.put(rule, numSupport * rule.getNumber());
      final IntegerMap<Unit> unitsForRule = new IntegerMap<>();
      supporters.forEach(unit -> unitsForRule.put(unit, rule.getNumber()));
      impArtTechUnits.forEach(unit -> unitsForRule.add(unit, rule.getNumber()));
      supportUnitsLeft.put(rule, unitsForRule);
      supportsAvailable
          .computeIfAbsent(rule.getBonusType().getName(), (name) -> new ArrayList<>())
          .add(rule);
    }

    return builder()
        .supportLeft(supportLeft)
        .supportRules(supportsAvailable)
        .supportUnits(supportUnitsLeft)
        .build();
  }

  Map<Unit, IntegerMap<Unit>> giveSupportToUnit(
      final Unit unit, final Predicate<UnitSupportAttachment> ruleFilter) {
    final Map<Unit, IntegerMap<Unit>> supportUsed = new HashMap<>();
    for (final List<UnitSupportAttachment> rulesByBonusType : supportRules.values()) {

      int maxPerBonusType = rulesByBonusType.get(0).getBonusType().getCount();
      for (final UnitSupportAttachment rule : rulesByBonusType) {
        if (!ruleFilter.test(rule)) {
          continue;
        }
        final Set<UnitType> types = rule.getUnitType();
        final int numSupportAvailableToApply = getSupportAvailable(rule);
        if (types != null && types.contains(unit.getType()) && numSupportAvailableToApply > 0) {

          for (int i = 0; i < numSupportAvailableToApply; i++) {
            final Unit u = useSupport(rule);
            supportUsed.computeIfAbsent(u, j -> new IntegerMap<>()).add(unit, rule.getBonus());
          }
          maxPerBonusType -= numSupportAvailableToApply;
          if (maxPerBonusType <= 0) {
            break;
          }
        }
      }
    }
    return supportUsed;
  }

  private int getSupportAvailable(final UnitSupportAttachment support) {
    return Math.min(
        support.getBonusType().getCount(),
        Math.min(supportLeft.getInt(support), supportUnits.get(support).size()));
  }

  private Unit useSupport(final UnitSupportAttachment support) {
    supportLeft.add(support, -1);
    final Set<Unit> supporters = supportUnits.get(support).keySet();
    final Unit u = supporters.iterator().next();
    supportUnits.get(support).add(u, -1);
    if (supportUnits.get(support).getInt(u) <= 0) {
      supportUnits.get(support).removeKey(u);
    }
    return u;
  }
}
