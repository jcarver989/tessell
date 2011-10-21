package org.gwtmpv.model.dsl;

import java.util.Arrays;

import org.gwtmpv.model.properties.EnumProperty;
import org.gwtmpv.widgets.IsListBox;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;

/** Binds {@link EnumProperty}s to widgets. */
public class EnumPropertyBinder<E extends Enum<E>> extends PropertyBinder<E> {

  private final EnumProperty<E> ep;

  EnumPropertyBinder(EnumProperty<E> ep) {
    super(ep);
    this.ep = ep;
  }

  public HandlerRegistrations to(final IsListBox source, final E[] values) {
    int i = 0;
    for (E value : values) {
      source.addItem(value.toString(), Integer.toString(i++));
    }
    if (ep.get() == null) {
      // TODO don't currently support an empty option
      ep.set(values[0]);
    }
    source.setSelectedIndex(Arrays.asList(values).indexOf(ep.get()));
    return new HandlerRegistrations(source.addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        int i = source.getSelectedIndex();
        if (i == -1) {
          ep.set(null);
        } else {
          ep.set(values[i]);
        }
      }
    }));
  }

}
