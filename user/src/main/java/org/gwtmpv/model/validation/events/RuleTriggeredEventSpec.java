package org.gwtmpv.model.validation.events;

import org.gwtmpv.GenEvent;
import org.gwtmpv.Param;

@GenEvent(methodName = "onTrigger")
public class RuleTriggeredEventSpec {
  @Param(1)
  Object key;
  @Param(2)
  String message;
  @Param(3)
  Boolean[] displayed;
}
