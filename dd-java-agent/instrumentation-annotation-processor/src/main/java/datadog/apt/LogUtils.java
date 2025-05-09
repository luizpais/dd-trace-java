package datadog.apt;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

/** Utility class for logging from annotation processor to the ProcessingEnvironment */
public final class LogUtils {
  private LogUtils() {}

  private static final boolean NOTE = false;

  public static final void log(
      ProcessingEnvironment processingEnv, String formatStr, Object... args) {
    String msg = String.format(formatStr, args);

    processingEnv.getMessager().printMessage(Kind.NOTE, msg);
  }

  public static final void warning(
      ProcessingEnvironment processingEnv, Element element, String formatStr, Object... args) {
    message(processingEnv, element, Kind.WARNING, formatStr, args);
  }

  public static final void error(
      ProcessingEnvironment processingEnv, Element element, String formatStr, Object... args) {
    message(processingEnv, element, Kind.ERROR, formatStr, args);
  }

  public static final void message(
      ProcessingEnvironment processingEnv,
      Element element,
      Kind kind,
      String formatStr,
      Object... args) {
    String msg = String.format(formatStr, args);

    if (kind != Kind.NOTE || NOTE) {
      processingEnv.getMessager().printMessage(kind, msg, element);
    }
  }
}
