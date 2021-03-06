package org.tessell.testing;

import static org.tessell.util.StringUtils.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.tessell.bus.StubEventBus;
import org.tessell.gwt.dom.client.StubStyle;
import org.tessell.gwt.user.client.ui.HasCss;
import org.tessell.place.events.PlaceRequestEvent;
import org.tessell.widgets.IsTextList;
import org.tessell.widgets.StubTextList;

public class TessellMatchers {

  /** A matcher to assert display != none. */
  public static Matcher<HasCss> shown() {
    return new AbstractCssMatcher("shown", "display", null, "block", "inline");
  }

  /** A matcher to assert display == none. */
  public static Matcher<HasCss> hidden() {
    return new AbstractCssMatcher("hidden", "display", "none");
  }

  /** A matcher to assert visible == hidden. */
  public static Matcher<HasCss> visible() {
    return new AbstractCssMatcher("visible", "visibility", null, "visible");
  }

  /** A matcher to assert visible == visible|unset. */
  public static Matcher<HasCss> invisible() {
    return new AbstractCssMatcher("invisible", "visibility", "hidden");
  }

  public static class AbstractCssMatcher extends TypeSafeMatcher<HasCss> {
    private final String description;
    private final String styleName;
    private final List<String> goodValues;

    public AbstractCssMatcher(String description, String styleName, String... goodValues) {
      this.description = description;
      this.styleName = styleName;
      this.goodValues = Arrays.asList(goodValues);
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(this.description);
    }

    @Override
    protected boolean matchesSafely(HasCss item) {
      return goodValues.contains(getValue(item));
    }

    @Override
    protected void describeMismatchSafely(HasCss item, Description mismatchDescription) {
      mismatchDescription.appendValue(item);
      mismatchDescription.appendText(" " + styleName + " is ");
      mismatchDescription.appendValue(getValue(item));
    }

    private String getValue(HasCss item) {
      return ((StubStyle) item.getStyle()).getStyle().get(styleName);
    }
  }

  /** A matcher to assert an arbitrary CSS property. */
  public static Matcher<HasCss> hasStyle(final String name, final String value) {
    return new TypeSafeMatcher<HasCss>() {
      @Override
      protected boolean matchesSafely(HasCss item) {
        return value.equals(get(item));
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(name + " is ").appendValue(value);
      }

      @Override
      protected void describeMismatchSafely(HasCss item, Description mismatchDescription) {
        mismatchDescription.appendValue(item);
        mismatchDescription.appendText(" " + name + " is ");
        mismatchDescription.appendValue(get(item));
      }

      private String get(HasCss item) {
        return ((StubStyle) item.getStyle()).getStyle().get(name);
      }
    };
  }

  /** A matcher to assert a class name being present. */
  public static Matcher<HasCss> hasStyle(final String className) {
    return new TypeSafeMatcher<HasCss>() {
      @Override
      protected boolean matchesSafely(HasCss item) {
        return (" " + item.getStyleName() + " ").indexOf(" " + className + " ") > -1;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("style ").appendValue(className);
      }

      @Override
      protected void describeMismatchSafely(HasCss item, Description mismatchDescription) {
        mismatchDescription.appendValue(item);
        mismatchDescription.appendText(" does not have ");
        mismatchDescription.appendValue(className);
      }
    };
  }

  /** A matcher to assert no validation errors. */
  public static Matcher<IsTextList> hasNoErrors() {
    return new TypeSafeMatcher<IsTextList>() {
      @Override
      protected boolean matchesSafely(IsTextList item) {
        return ((StubTextList) item).getList().size() == 0;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("has no errors");
      }

      @Override
      protected void describeMismatchSafely(IsTextList item, Description mismatchDescription) {
        mismatchDescription.appendValue(item);
        mismatchDescription.appendText(" has errors ");
        mismatchDescription.appendValueList("", ", ", "", ((StubTextList) item).getList());
      }
    };
  }

  /** A matcher to assert validation errors. */
  public static Matcher<IsTextList> hasErrors(final String... errors) {
    return new TypeSafeMatcher<IsTextList>() {
      @Override
      protected boolean matchesSafely(IsTextList item) {
        String expected = join(errors, ", ");
        String actual = join(((StubTextList) item).getList(), ", ");
        return expected.equals(actual);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("errors ");
        description.appendValueList("<", ", ", ">", errors);
      }

      @Override
      protected void describeMismatchSafely(IsTextList item, Description mismatchDescription) {
        mismatchDescription.appendText("errors ");
        mismatchDescription.appendValueList("<", ", ", ">", ((StubTextList) item).getList());
      }
    };
  }

  /** A matcher to assert place requests on the event bus. */
  public static Matcher<StubEventBus> hasPlaceRequests(final String... places) {
    return new TypeSafeMatcher<StubEventBus>() {
      @Override
      protected boolean matchesSafely(StubEventBus bus) {
        return Arrays.asList(places).equals(getRequests(bus));
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("has places ");
        description.appendValueList("[", ", ", "]", Arrays.asList(places));
      }

      @Override
      protected void describeMismatchSafely(StubEventBus bus, Description mismatchDescription) {
        mismatchDescription.appendText("places are ");
        mismatchDescription.appendValueList("[", ", ", "]", getRequests(bus));
      }

      private List<String> getRequests(StubEventBus bus) {
        List<String> requests = new ArrayList<String>();
        for (PlaceRequestEvent e : bus.getEvents(PlaceRequestEvent.class)) {
          requests.add(e.getRequest().toString());
        }
        return requests;
      }
    };
  }

  /** A matcher to assert based on toString. */
  public static Matcher<Object> asString(final String expected) {
    return new BaseMatcher<Object>() {
      @Override
      public boolean matches(Object item) {
        return item != null && item.toString() != null && item.toString().equals(expected);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(expected);
      }
    };
  }
}
