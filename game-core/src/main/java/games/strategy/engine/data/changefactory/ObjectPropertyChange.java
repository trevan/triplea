package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Unit;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import org.triplea.java.RemoveOnNextMajorRelease;

/** A game data change that captures a change to an object property value. */
public class ObjectPropertyChange extends Change {
  private static final long serialVersionUID = 4218093376094170940L;

  @RemoveOnNextMajorRelease("Object is only used for old save games")
  private final Unit object = null;

  @Getter private String property;

  @Getter(AccessLevel.PACKAGE)
  private final Object newValue;

  @Getter(AccessLevel.PACKAGE)
  private final Object oldValue;

  private final String objectId;

  ObjectPropertyChange(final Unit object, final String property, final Object newValue) {
    this(
        object.getId().toString(),
        property,
        newValue,
        object.getPropertyOrThrow(property).getValue());
  }

  private ObjectPropertyChange(
      final String objectId, final String property, final Object newValue, final Object oldValue) {
    // prevent multiple copies of the property names being held in the game
    this.property = property.intern();
    this.newValue = newValue;
    this.oldValue = oldValue;
    this.objectId = objectId;
  }

  private void readObject(final ObjectInputStream stream)
      throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    property = property.intern();
  }

  @Override
  public Change invert() {
    return new ObjectPropertyChange(objectId, property, oldValue, newValue);
  }

  @Override
  protected void perform(final GameData data) {
    final Unit hydratedObject;
    // this.object is not null when this object is loaded from an old save game
    if (this.object != null) {
      hydratedObject = this.object;
    } else {
      hydratedObject = data.getUnits().get(UUID.fromString(this.objectId));
    }

    try {
      hydratedObject.getPropertyOrThrow(property).setValue(newValue);
    } catch (final MutableProperty.InvalidValueException e) {
      throw new IllegalStateException(
          String.format(
              "failed to set value '%s' on property '%s' for object '%s'",
              newValue, property, object),
          e);
    }
  }

  @Override
  public String toString() {
    return "Property change, unit:"
        + objectId
        + " property:"
        + property
        + " newValue:"
        + newValue
        + " oldValue:"
        + oldValue;
  }
}
