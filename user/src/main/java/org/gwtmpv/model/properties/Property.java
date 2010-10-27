package org.gwtmpv.model.properties;

import org.gwtmpv.model.events.PropertyChangedHandler;
import org.gwtmpv.model.validation.Valid;
import org.gwtmpv.model.validation.rules.Rule;
import org.gwtmpv.model.values.Value;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

public interface Property<P> extends HasRuleTriggers, Value<P> {

  P get();

  void set(P value);

  void reassess();

  void addRule(Rule rule);

  boolean isTouched();

  void setTouched(boolean touched);

  Valid touch();

  /** @return whether this property was invalid, does not rerun validation. */
  Valid wasValid();

  /** For rules to fire events against our handlers. */
  void fireEvent(GwtEvent<?> event);

  /** Adds {@code} downstream as a derivative of us. */
  <T extends Property<?>> T addDerived(final T downstream);

  /** Adds us as a derivative of {@code upstream} properties. */
  Property<P> depends(Property<?>... upstream);

  HandlerRegistration addPropertyChangedHandler(PropertyChangedHandler<P> handler);

  String getName();

}
