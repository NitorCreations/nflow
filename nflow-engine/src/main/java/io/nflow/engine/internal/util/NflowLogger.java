package io.nflow.engine.internal.util;

import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;

@Component
public class NflowLogger {

  public void log(Logger logger, Level level, String message, Object... args) {
    getLogMethod(logger, level).accept(message, args);
  }

  private BiConsumer<String, Object[]> getLogMethod(Logger logger, Level level) {
    switch (level) {
    case TRACE:
      return logger::trace;
    case DEBUG:
      return logger::debug;
    case INFO:
      return logger::info;
    case WARN:
      return logger::warn;
    case ERROR:
    default:
      return logger::error;
    }
  }
}
