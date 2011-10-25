package org.gwtmpv.widgets;

import com.google.gwt.event.dom.client.HasAllMouseHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasErrorHandlers;
import com.google.gwt.event.dom.client.HasLoadHandlers;
import com.google.gwt.resources.client.ImageResource;

public interface IsImage extends IsWidget, HasLoadHandlers, HasErrorHandlers, HasClickHandlers, HasAllMouseHandlers {

  String getUrl();

  void setUrl(String url);

  void setResource(ImageResource imageResource);

}
