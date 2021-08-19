import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;
import java.util.logging.Logger;

public class LogInterceptor {
    private static final Logger log = Logger.getLogger(LogInterceptor.class.getName());

    @Advice.OnMethodEnter
    public static void onMethodEnter(@Advice.Origin Method m, @Advice.AllArguments Object[] para) {
        System.out.println("Entering class unknown::" + m.getName() + " !");
    }

    @Advice.OnMethodExit
    public static void intercept(@Advice.Origin Method m, @Advice.AllArguments Object[] para) {
        System.out.println("Hello World from class unknown::" + m.getName() + " !");
//        final Class<?> declaringClass = m.getDeclaringClass();
//
//        if( declaringClass == null) {
//            System.out.println("Hello Static World from unknown::" + m.getName() + "!");
//        } else {
//            System.out.println("Hello Static World from known::" + m.getName() + "!");
////            System.out.println("Hello Static World from class " + declaringClass.getSimpleName() + "::" + m.getName() + " !");
//        }


        if( para.length > 0) {
            System.out.print(String.format("%s", para[0]));
            for( int i=1; i<para.length; i++) {
                System.out.print(String.format(",%s", para[i]));
            }
        }
        System.out.println();
    }
}
