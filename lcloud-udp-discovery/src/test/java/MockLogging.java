/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */
import java.util.logging.LogManager;

public class MockLogging {
  public static void setupLogging() {
    try (var is = MockLogging.class.getClassLoader().getResourceAsStream("logging.properties")) {
      LogManager.getLogManager().readConfiguration(is);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}
