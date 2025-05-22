/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Logger with filename and line taken from stackTrace */
public class FileLineLogger {

  private final String offsetString;

  private final Logger logger;

  /**
   * Wrapping constructor
   *
   * @param logger being wrapped
   * @param label message prefix
   * @param offset message offset
   */
  protected FileLineLogger(Logger logger, String label, int offset) {
    this.logger = logger;
    this.offsetString = 0 < offset ? String.format("%1$" + offset + "s%2$s", " ", label) : label;
  }

  /**
   * Wrapping factory
   *
   * @param name of logger
   * @return FileLineLogger instance
   */
  public static FileLineLogger getLogger(String name) {
    return new FileLineLogger(Logger.getLogger(name), "", 0);
  }

  /**
   * Named logger factory
   *
   * @param name of logger
   * @param label message prefix
   * @param offset of message
   * @return logger
   */
  public static FileLineLogger getLogger(String name, String label, int offset) {
    return new FileLineLogger(Logger.getLogger(name), label, offset);
  }

  /**
   * Get named logger
   *
   * @param name of logger
   * @param label message prefix
   * @return logger
   */
  public static FileLineLogger getLogger(String name, String label) {
    return new FileLineLogger(Logger.getLogger(name), label, 0);
  }

  private static String getFileLineString(StackTraceElement stackTraceElement) {
    return " (" + stackTraceElement.getFileName() + ":" + stackTraceElement.getLineNumber() + ")";
  }

  /**
   * print thread
   *
   * @param message what to print
   * @param <T> type of message
   */
  public static <T> void pt(@SuppressWarnings("unused") T message) {
    //    System.out.printf("[%1$s] %2$s%n", Thread.currentThread().getName(), message.toString());
  }

  /**
   * Wraps {@link Logger#isLoggable}
   *
   * @param level of logging
   * @return is level loggable
   */
  public boolean isLoggable(Level level) {
    return logger.isLoggable(level);
  }

  /**
   * Wraps info
   *
   * @param message message
   */
  public void info(String message) {
    log(Level.INFO, message);
  }

  /**
   * Wraps finer
   *
   * @param message text
   */
  public void finer(String message) {
    log(Level.FINER, message);
  }

  private void log(Level level, String message) {

    if (logger.isLoggable(level)) {
      StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];

      synchronized (FileLineLogger.class) {
        logger.log(level, offsetString + message + getFileLineString(stackTraceElement));
      }
    }
  }

  private void log(Level level, String message, Throwable throwable) {
    if (logger.isLoggable(level)) {
      StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];

      synchronized (FileLineLogger.class) {
        if (logger.isLoggable(Level.FINEST)) {
          logger.log(
              level, offsetString + message + getFileLineString(stackTraceElement), throwable);
        } else {
          logger.log(level, offsetString + message + getFileLineString(stackTraceElement));
        }
      }
    }
  }

  /**
   * Wraps severe
   *
   * @param message text
   */
  public void severe(String message) {
    log(Level.SEVERE, message);
  }

  /**
   * Wraps finer
   *
   * @param message text
   * @param throwable cause
   */
  public void finer(String message, Throwable throwable) {
    log(Level.FINER, message, throwable);
  }

  /**
   * Wraps severe
   *
   * @param message text
   * @param throwable cause
   */
  public void severe(String message, Throwable throwable) {
    log(Level.SEVERE, message, throwable);
  }

  /**
   * Wraps finest
   *
   * @param message text
   */
  public void finest(String message) {
    log(Level.FINEST, message);
  }

  /**
   * Wraps fine
   *
   * @param message text
   */
  public void fine(String message) {
    log(Level.FINE, message);
  }
}
