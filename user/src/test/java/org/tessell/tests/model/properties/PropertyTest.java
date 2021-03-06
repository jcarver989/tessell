package org.tessell.tests.model.properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.tessell.model.properties.NewProperty.integerProperty;

import org.junit.Test;
import org.tessell.model.events.PropertyChangedEvent;
import org.tessell.model.events.PropertyChangedHandler;
import org.tessell.model.properties.IntegerProperty;
import org.tessell.model.validation.Valid;
import org.tessell.model.validation.rules.Custom;
import org.tessell.model.values.DerivedValue;
import org.tessell.tests.model.validation.rules.AbstractRuleTest;

public class PropertyTest extends AbstractRuleTest {

  @Test
  public void twoWayDerived() {
    final IntegerProperty a = integerProperty("a", 1);
    final IntegerProperty b = integerProperty("b", 2);
    listenTo(a);
    listenTo(b);

    new Custom(a, "a must be greater than b", new DerivedValue<Boolean>() {
      public Boolean get() {
        return a.get() > b.get();
      }
    });
    a.addDerived(b);

    new Custom(b, "b must be within 5 of a", new DerivedValue<Boolean>() {
      public Boolean get() {
        return Math.abs(a.get() - b.get()) <= 5;
      }
    });
    b.addDerived(a);

    a.reassess();
    assertMessages("");

    a.set(-10);
    assertMessages("b must be within 5 of a", "a must be greater than b");
  }

  @Test
  public void validationHappensBeforeChange() {
    final IntegerProperty a = integerProperty("a", 10);
    new Custom(a, "a must be greater than 5", new DerivedValue<Boolean>() {
      public Boolean get() {
        return a.get() != null && a.get() > 5;
      }
    });

    final Boolean[] wasInvalidOnChange = { null };
    a.addPropertyChangedHandler(new PropertyChangedHandler<Integer>() {
      public void onPropertyChanged(PropertyChangedEvent<Integer> event) {
        wasInvalidOnChange[0] = a.wasValid() == Valid.NO;
      }
    });

    a.set(1);
    assertThat(wasInvalidOnChange[0], is(true));
  }

  @Test
  public void validationOfDerivedValuesHappensBeforeChange() {
    final IntegerProperty a = integerProperty("a");
    final IntegerProperty b = integerProperty("b");
    new Custom(b, "b must be greater than a", new DerivedValue<Boolean>() {
      public Boolean get() {
        return b.get() == null || a.get() == null || b.get() > a.get();
      }
    });
    b.depends(a);

    // set with good values
    a.set(1);
    b.set(2);

    final Boolean[] asWasInvalid = { null };
    a.addPropertyChangedHandler(new PropertyChangedHandler<Integer>() {
      public void onPropertyChanged(PropertyChangedEvent<Integer> event) {
        asWasInvalid[0] = b.wasValid() == Valid.NO;
      }
    });

    a.set(3);
    assertThat(asWasInvalid[0], is(true));
  }

}
