/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */
package org.mtbo.lcloud.discovery.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Logger with filename and line taken from stackTrace */
public class FileLineLogger {

  //  private static final Object obj = new Object();
  //  private static final Scheduler logScheduler =
  //      Schedulers.newParallel("log-scheduler", (Schedulers.DEFAULT_POOL_SIZE + 1) / 2);
  private final Logger logger;

  /**
   * Wrapping constructor
   *
   * @param logger being wrapped
   */
  public FileLineLogger(Logger logger) {
    this.logger = logger;
  }

  /**
   * Wrapping factory
   *
   * @param name of logger
   * @return FileLineLogger instance
   */
  public static FileLineLogger getLogger(String name) {
    return new FileLineLogger(Logger.getLogger(name));
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
  public static <T> void pt(T message) {
    //    System.out.printf("[%1$s] %2$s%n", Thread.currentThread().getName(), message.toString());
    //    System.out.flush();
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

  //  public <T> Mono<T> infoMono(T pass, String message) {
  //    return monoLog(pass, () -> log(Level.INFO, message));
  //  }

  /**
   * Wraps finer
   *
   * @param message text
   */
  public void finer(String message) {
    log(Level.FINER, message);
  }

  //  private Object log(Level info, String message) {
  //    if (logger.isLoggable(info)) {
  //      StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
  //
  //      logger.log(info, message + getFileLineString(stackTraceElement));
  //    }
  //
  //    return obj;
  //  }

  private void log(Level info, String message) {
    if (logger.isLoggable(info)) {
      StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];

      logger.log(info, message + getFileLineString(stackTraceElement));
    }
  }

  private void log(Level info, String message, Throwable throwable) {
    if (logger.isLoggable(info)) {
      StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];

      logger.log(info, message + getFileLineString(stackTraceElement), throwable);
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

  //  public <T> Mono<T> fineMono(T pass, String message) {
  //    return monoLog(pass, () -> log(Level.FINE, message));
  //  }

  //  private <T> Mono<T> monoLog(T pass, Callable<Object> callable) {
  //    return Mono.fromCallable(
  //            () -> {
  //              try {
  //                callable.call();
  //              } catch (Exception _) {
  //              }
  //              return pass;
  //            })
  //        .publishOn(logScheduler);
  //  }
}
