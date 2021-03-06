/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.event.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.event.shared.GwtEvent.Type;

/** Simpler implementation of {@link EventBus}.
 *
 * Specifically, we:
 * 
 * 1. Don't worry about legacy HandlerManager features (reverse firing)
 * 
 * 2. Adds/removes immediately take effect, even if an event is already firing
 *
 * The 2nd difference is the most critical to me, where something like place
 * changed firing (so firingDepth is already > 0) leads to something wanting
 * to listen to events right away, and not wait until the place change is
 * finished.
 */
public class SimplerEventBus extends EventBus {

  private int firingDepth = 0;

  /** Handler lists that have null markers in them. */
  private final List<ToClean<?>> needsCleaning = new ArrayList<ToClean<?>>();

  /** Map of event type to map of event source to list of their handlers. */
  private final Map<GwtEvent.Type<?>, Map<Object, List<?>>> map = new HashMap<GwtEvent.Type<?>, Map<Object, List<?>>>();

  @Override
  public <H extends EventHandler> HandlerRegistration addHandler(Type<H> type, H handler) {
    checkNotNull(type, "Cannot add a handler with a null type");
    checkNotNull(handler, "Cannot add a null handler");
    return doAdd(type, null, handler);
  }

  @Override
  public <H extends EventHandler> HandlerRegistration addHandlerToSource(final GwtEvent.Type<H> type, final Object source, final H handler) {
    checkNotNull(type, "Cannot add a handler with a null type");
    checkNotNull(source, "Cannot add a handler with a null source");
    checkNotNull(handler, "Cannot add a null handler");
    return doAdd(type, source, handler);
  }

  @Override
  public void fireEvent(GwtEvent<?> event) {
    checkNotNull(event, "Cannot fire null event");
    doFire(event, null);
  }

  @Override
  public void fireEventFromSource(GwtEvent<?> event, Object source) {
    checkNotNull(event, "Cannot fire null event");
    checkNotNull(source, "Cannot fire from a null source");
    doFire(event, source);
  }

  /** Creates new map entries for {@code type}/{@code source} and adds {@code handler}. */
  private <H> HandlerRegistration doAdd(final GwtEvent.Type<H> type, final Object source, final H handler) {
    ensureHandlerList(type, source).add(handler);
    return new HandlerRegistration() {
      public void removeHandler() {
        if (firingDepth > 0) {
          doRemoveWithDeferredCleanup(type, source, handler);
        } else {
          doRemoveNow(type, source, handler);
        }
      }
    };
  }

  private <H extends EventHandler> void doFire(GwtEvent<H> event, Object source) {
    try {
      firingDepth++;

      if (source != null) {
        // not visible in 2.3
        // event.setSource(source);
      }

      Set<Throwable> causes = null;

      List<H> handlers = getDispatchList(event.getAssociatedType(), source);
      for (int i = 0; i < handlers.size(); i++) {
        H handler = handlers.get(i);
        // was the handler unregistered during our iteration? 
        if (handler == null) {
          continue;
        }
        try {
          event.dispatch(handler);
        } catch (Throwable e) {
          if (causes == null) {
            causes = new HashSet<Throwable>();
          }
          causes.add(e);
        }
      }

      if (causes != null) {
        throw new UmbrellaException(causes);
      }
    } finally {
      firingDepth--;
      if (firingDepth == 0) {
        executeCleaning();
      }
    }
  }

  private <H> void doRemoveNow(final GwtEvent.Type<H> type, final Object source, final H handler) {
    List<H> l = getHandlerList(type, source);
    if (l.remove(handler) && l.isEmpty()) {
      prune(type, source);
    }
  }

  private <H> void doRemoveWithDeferredCleanup(final GwtEvent.Type<H> type, final Object source, final H handler) {
    // immediately mark the handler as removed
    final List<H> l = getHandlerList(type, source);
    final int handlerIndex = l.indexOf(handler);
    assert handlerIndex >= 0 : "handler was not registered " + handler;
    l.set(handlerIndex, null);
    // defer the cleanup of the null marker
    needsCleaning.add(new ToClean<H>(type, source, l));
  }

  private <H> void clean(ToClean<H> toClean) {
    toClean.l.remove(null);
    if (toClean.l.isEmpty()) {
      prune(toClean.type, toClean.source);
    }
  }

  private <H> List<H> getDispatchList(GwtEvent.Type<H> type, Object source) {
    if (source == null) {
      return getHandlerList(type, null);
    } else {
      List<H> all = new ArrayList<H>();
      all.addAll(getHandlerList(type, source));
      all.addAll(getHandlerList(type, null));
      return all;
    }
  }

  /** @return handlers for {@code type}/{@code source}, has no side-effects if none are registered yet. */
  private <H> List<H> getHandlerList(GwtEvent.Type<H> type, Object source) {
    Map<Object, List<?>> sourceMap = map.get(type);
    if (sourceMap == null) {
      return Collections.emptyList();
    }
    // safe, we control the puts.
    @SuppressWarnings("unchecked")
    List<H> handlers = (List<H>) sourceMap.get(source);
    if (handlers == null) {
      return Collections.emptyList();
    }
    return handlers;
  }

  /** @return handlers for {@code type}/{@code source}, creates the list if this is the first registration. */
  private <H> List<H> ensureHandlerList(GwtEvent.Type<H> type, Object source) {
    Map<Object, List<?>> sourceMap = map.get(type);
    if (sourceMap == null) {
      sourceMap = new HashMap<Object, List<?>>();
      map.put(type, sourceMap);
    }
    // safe, we control the puts.
    @SuppressWarnings("unchecked")
    List<H> handlers = (List<H>) sourceMap.get(source);
    if (handlers == null) {
      handlers = new ArrayList<H>();
      sourceMap.put(source, handlers);
    }
    return handlers;
  }

  private void executeCleaning() {
    if (needsCleaning.size() == 0) {
      return;
    }
    try {
      for (ToClean<?> h : needsCleaning) {
        clean(h);
      }
    } finally {
      needsCleaning.clear();
    }
  }

  private void prune(GwtEvent.Type<?> type, Object source) {
    Map<Object, List<?>> sourceMap = map.get(type);
    List<?> pruned = sourceMap.remove(source);
    assert pruned != null : "Can't prune what wasn't there";
    assert pruned.isEmpty() : "Pruned unempty list!";
    if (sourceMap.isEmpty()) {
      map.remove(type);
    }
  }

  private void checkNotNull(Object arg, String message) {
    if (arg == null) {
      throw new NullPointerException(message);
    }
  }

  private static class ToClean<H> {
    private final GwtEvent.Type<H> type;
    private final Object source;
    private final List<H> l;

    private ToClean(GwtEvent.Type<H> type, Object source, List<H> l) {
      this.type = type;
      this.source = source;
      this.l = l;
    }
  }

}
