import net.bytebuddy.implementation.bind.annotation.Origin;

import java.lang.reflect.Method;
import java.util.logging.Logger;

public class LogInterceptor {
    private static final Logger log = Logger.getLogger(LogInterceptor.class.getName());

    public static void intercept(@Origin Method m) {
        System.out.println("Hello World from " + m.getName() + "!");
    }
}
