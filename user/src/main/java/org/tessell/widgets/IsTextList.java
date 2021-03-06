package org.tessell.widgets;

import org.tessell.gwt.user.client.ui.IsWidget;

public interface IsTextList extends IsWidget {

  boolean hasErrors();

  void add(String text);

  void remove(String text);

  void clear();

  boolean isEnabled();

  void setEnabled(boolean enabled);

  String getChildTag();

  void setChildTag(String childTag);

  String getChildStyleName();

  void setChildStyleName(String childStyleName);

}
