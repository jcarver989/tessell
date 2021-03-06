package org.tessell.generators.views;

import static joist.sourcegen.Argument.arg;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import joist.sourcegen.Access;
import joist.sourcegen.Argument;
import joist.sourcegen.GClass;
import joist.sourcegen.GField;
import joist.sourcegen.GMethod;
import joist.util.Join;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.tessell.generators.GenUtils;
import org.tessell.generators.css.CssGenerator;
import org.tessell.generators.css.CssStubGenerator;
import org.tessell.generators.resources.ResourcesGenerator;
import org.tessell.gwt.dom.client.GwtElement;
import org.tessell.gwt.dom.client.IsElement;
import org.tessell.gwt.dom.client.IsStyle;
import org.tessell.gwt.user.client.ui.IsWidget;
import org.tessell.widgets.StubView;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.UIObject;

/** A {@code ui.xml} file. */
class UiXmlFile {

  private final ViewGenerator viewGenerator;
  private final File uiXml;
  final File uiXmlCopy;
  final String baseName;
  final GClass gwtView;
  final GClass isView;
  final GClass stubView;

  // the handler is only created if we have to parser the ui.xml file
  private UiXmlHandler handler;

  UiXmlFile(ViewGenerator viewGenerator, final File uiXml) {
    this.viewGenerator = viewGenerator;
    this.uiXml = uiXml;
    uiXmlCopy = ResourcesGenerator.fileInOutputDirectory(viewGenerator.input, viewGenerator.output, uiXml, ".ui.xml", "-gen.ui.xml");

    final String templateClassName = deriveClassName();
    final String packageName = StringUtils.substringBeforeLast(templateClassName, ".");
    baseName = StringUtils.substringAfterLast(templateClassName, ".");
    isView = new GClass(packageName + ".Is" + baseName);
    gwtView = new GClass(packageName + ".Gwt" + baseName);
    stubView = new GClass(packageName + ".Stub" + baseName);
  }

  /** @return whether the {@code ui.xml} file is newer than the last {@code IsXxx} output. */
  public boolean hasChanged() {
    File isFile = new File(viewGenerator.output, isView.getFileName());
    if (!isFile.exists()) {
      return true;
    }
    return uiXml.lastModified() > isFile.lastModified();
  }

  public void generate() throws Exception {
    System.out.println(uiXml);
    handler = new UiXmlHandler();
    viewGenerator.parser.parse(uiXml, handler);

    generateIsView();
    generateGwtView();
    generateStubView();
  }

  private void generateIsView() throws Exception {
    isView.baseClass(IsWidget.class);
    isView.setInterface();

    // methods for each ui:field
    for (final UiFieldDeclaration uiField : handler.uiFields) {
      isView.getMethod(uiField.name).returnType(viewGenerator.config.getInterface(uiField.type));
    }
    // methods for each ui:style
    for (final UiStyleDeclaration style : handler.styleFields) {
      isView.getMethod(style.name).returnType(style.type);
    }

    viewGenerator.markAndSave(isView);
  }

  private void generateGwtView() throws Exception {
    gwtView.baseClass(Composite.class).implementsInterface(isView.getSimpleName());
    final GMethod cstr = gwtView.getConstructor();
    gwtView.addImports(GWT.class);
    if (handler.withFields.size() > 0 || handler.uiFields.size() > 0) {
      gwtView.addImports(UiField.class);
    }

    final GMethod debugId = gwtView.getMethod("onEnsureDebugId").argument("String", "baseDebugId");
    debugId.addAnnotation("@Override").setProtected();
    debugId.body.line("super.onEnsureDebugId(baseDebugId);");

    {
      // uibinder
      final GClass uibinder = gwtView.getInnerClass("MyUiBinder").setInterface();
      uibinder.baseClassName("{}<{}, {}>", UiBinder.class.getName(), handler.firstTagType, gwtView.getSimpleName());
      uibinder.addAnnotation("@UiTemplate(\"{}\")", uiXml.getName().replace(".ui.xml", "-gen.ui.xml"));
      gwtView.addImports(UiTemplate.class);

      gwtView.getField("binder").type("MyUiBinder").setStatic().setFinal().initialValue("GWT.create(MyUiBinder.class)");
    }

    {
      String uiXmlContent = FileUtils.readFileToString(uiXml);
      uiXmlContent = ResourcesGenerator.doMozWebkitSubstitution(uiXmlContent);
      // use the tessell subclasses that implement the IsXxx interfaces
      uiXmlContent = uiXmlContent.replace("urn:import:com.google.gwt.user.client.ui", "urn:import:org.tessell.gwt.user.client.ui");
      GenUtils.saveIfChanged(uiXmlCopy, uiXmlContent);
      viewGenerator.cleanup.markOkay(uiXmlCopy);
    }

    // for each ui:with, add a @UiField(provided=true) field and a constructor arg
    for (final UiFieldDeclaration field : handler.withFields) {
      gwtView.getField(field.name).type(field.type).setAccess(Access.PACKAGE).addAnnotation("@UiField(provided = true)");
      cstr.argument(field.type, field.name);
      cstr.body.line("this.{} = {};", field.name, field.name);
    }

    // for each ui:style, make @UiField fields, getter methods, plus Css class for each ui:style
    for (final UiStyleDeclaration style : handler.styleFields) {
      gwtView.getField(style.name).type(style.type).setAccess(Access.PACKAGE).addAnnotation("@UiField");
      gwtView.getMethod(style.name).returnType(style.type).body.line("return {};", style.name);
      new CssGenerator(style.getCssInFile(), viewGenerator.cleanup, style.type, viewGenerator.output).run();
    }

    // for each ui:field, make @UiField (usually provided=true) and getter methods
    for (final UiFieldDeclaration field : handler.uiFields) {
      final String interfaceType = viewGenerator.config.getInterface(field.type);
      final GField f = gwtView.getField(field.name);
      final GMethod m = gwtView.getMethod(field.name).returnType(interfaceType);

      if (field.isElement) {
        f.type(field.type).setAccess(Access.PACKAGE).addAnnotation("@UiField");
        m.body.line("return new {}({});", GwtElement.class.getName(), field.name);
      } else {
        // let UiBinder instantiate the type, which as a bonus means it will handle UiConstructor logic
        f.type(field.type).setAccess(Access.PACKAGE).addAnnotation("@UiField");
        m.body.line("return {};", field.name);
      }

      if (field.isElement) {
        debugId.body.line("UIObject.ensureDebugId({}, baseDebugId + \"-{}\");", field.name, field.name);
        gwtView.addImports(UIObject.class);
      } else {
        debugId.body.line("{}.ensureDebugId(baseDebugId + \"-{}\");", field.name, field.name);
      }
    }

    cstr.body.line("initWidget(binder.createAndBindUi(this));");
    cstr.body.line("ensureDebugId(\"{}\");", gwtView.getSimpleName().replaceAll("View$", "").replaceAll("^Gwt", ""));

    // add implements of getStyle and getIsElement
    GMethod getStyle = gwtView.getMethod("getStyle").returnType(IsStyle.class).addAnnotation("@Override");
    getStyle.body.line("return getIsElement().getStyle();");

    GMethod getIsElement = gwtView.getMethod("getIsElement").returnType(IsElement.class).addAnnotation("@Override");
    getIsElement.body.line("return new GwtElement(getElement());");
    gwtView.addImports(GwtElement.class);

    viewGenerator.markAndSave(gwtView);
  }

  private void generateStubView() throws Exception {
    stubView.baseClass(StubView.class).implementsInterface(isView.getSimpleName());

    // find any stubs that need parameters
    final Map<String, Argument> stubDependencies = new TreeMap<String, Argument>();
    for (String cstrType : getStubDependencies()) {
      if (!stubDependencies.containsKey(cstrType)) {
        String name = ViewGenerator.simpleName(cstrType);
        stubDependencies.put(cstrType, arg(cstrType, name));
      }
    }

    final GMethod cstr = stubView.getConstructor(new ArrayList<Argument>(stubDependencies.values()));

    final GMethod debugId = stubView.getMethod("onEnsureDebugId").argument("String", "baseDebugId");
    debugId.addAnnotation("@Override").setProtected();
    debugId.body.line("super.onEnsureDebugId(baseDebugId);");

    // for each ui:field, add a field assigned to the stub type, and a getter method
    for (final UiFieldDeclaration field : handler.uiFields) {
      final String stubType = viewGenerator.config.getStub(field.type);
      // our stub may take cstr params
      final List<String> cstrTypes = viewGenerator.config.getStubCstrParams(stubType);
      final List<String> cstrNames = new ArrayList<String>();
      for (String cstrType : cstrTypes) {
        cstrNames.add(ViewGenerator.simpleName(cstrType));
      }
      stubView.getField(field.name).type(stubType).setFinal();
      stubView.getMethod(field.name).returnType(stubType).body.line("return {};", field.name);
      debugId.body.line("{}.ensureDebugId(baseDebugId + \"-{}\");", field.name, field.name);
      // now use cstrNames in the call to new
      cstr.body.line("this.{} = new {}({});", field.name, stubType, Join.commaSpace(cstrNames));
    }
    for (final UiFieldDeclaration field : handler.uiFields) {
      if (!field.isElement) {
        cstr.body.line("widgets.add({});", field.name);
      }
    }

    // for each ui:style, make @UiField fields, getter method, plus StubCss class
    for (final UiStyleDeclaration style : handler.styleFields) {
      CssStubGenerator g = new CssStubGenerator(style.getCssInFile(), viewGenerator.cleanup, style.type, viewGenerator.output);
      g.run();
      stubView.getField(style.name).type(style.type).setFinal().initialValue("new {}()", g.getCssStubClassName());
      stubView.getMethod(style.name).returnType(style.type).body.line("return {};", style.name);
    }

    cstr.body.line("ensureDebugId(\"{}\");", stubView.getSimpleName().replaceAll("View$", "").replaceAll("^Stub", ""));

    viewGenerator.markAndSave(stubView);
  }

  String getPath() {
    return uiXml.getPath();
  }

  /** @return the ui:with fields, if we parsed the file. */
  List<UiFieldDeclaration> getFreshWiths() {
    return handler.withFields;
  }

  List<UiFieldDeclaration> getPossiblyCachedWiths(UiXmlCache cache) {
    return handler == null ? cache.getCachedWiths(this) : getFreshWiths();
  }

  /** @return the ui:style fields, if we parsed the file. */
  List<UiStyleDeclaration> getFreshStyles() {
    return handler.styleFields;
  }

  List<UiStyleDeclaration> getPossiblyCachedStyles(UiXmlCache cache) {
    return handler == null ? cache.getCachedStyles(this) : getFreshStyles();
  }

  List<String> getStubDependencies() {
    final Set<String> stubDependencies = new TreeSet<String>();
    for (final UiFieldDeclaration field : handler.uiFields) {
      final String stubType = viewGenerator.config.getStub(field.type);
      stubDependencies.addAll(viewGenerator.config.getStubCstrParams(stubType));
    }
    return new ArrayList<String>(stubDependencies);
  }

  List<String> getPossiblyCachedStubDependencies(UiXmlCache cache) {
    return handler == null ? cache.getCachedStubDependencies(this) : getStubDependencies();
  }

  private String deriveClassName() {
    // Get the absolute path, drop off the input path, drop ui.xml, / -> .
    return appendViewIfNeeded(uiXml
      .getAbsolutePath()
      .replace(viewGenerator.input.getPath() + File.separator, "")
      .replace(".ui.xml", "")
      .replace(File.separatorChar, '.'));
  }

  private String appendViewIfNeeded(String input) {
    return input.endsWith("View") ? input : input + "View";
  }

}
