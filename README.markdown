
See [tessell.org](http://www.tessell.org).

Todo
----

* Add @Place annotation
  * Take name of presenter
  * Any constructor parameters--how to denote application-level vs. request-level?
* Figure out cross-presenter transitions
  * Event bus doesn't make sense--these aren't global things
  * Have Slot listen to presenter events?
* Support 2nd generation Presenter interfaces via event hook hints in `ui.xml` file
* form disable on ServerCall
* indicator on ServerCall
* Pre-fill the handler (optional);
* doLogin.call(user, pass);
* HasDispatchAsync to ServerCall cstr
* ListProperty with filters/updates on child property change

Limitations
-----------

* ResourcesGenerator assumes a global namespace of image/CSS files, even if you use subdirectories. It also assumes all image/CSS files are below the packageName you pass to it.

Notes
-----

* Mapping cases:
  * `entity * (all fields) -> dto`
  * `entity some fields -> dto`
  * `entity + other stuff -> dto + other dto`
  * `entity field errors -> dto -> model property errors`
  * have the codegen make both the dtos and the mapping
  * List<Entity> <-> List<String> names

EventBus Todo
-------------

* `addHandler` should take `CanRegisterHandlers`--enforced registration, implicit is too magical for now
* `AbstractTypedHandler` with a `getType()` instance method


