import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

public class StaticInterceptor {
    @Advice.OnMethodEnter
    public static void intercept(@Advice.Origin Method m, @Advice.AllArguments Object[] para) {
        final Class<?> declaringClass = m.getDeclaringClass();
        if( declaringClass == null) {
            System.out.println("Hello Static World from unknown::" + m.getName() + "!");
        } else {
            System.out.println("Hello Static World from class " + declaringClass.getName() + "::" + m.getName() + " !");
        }

//        System.out.println("Hello World from " + m.getDeclaringClass().getCanonicalName() + "::" + m.getName() + "!");

        if( para.length > 0) {
            System.out.print(String.format("%s", para[0]));
            for( int i=1; i<para.length; i++) {
                System.out.print(String.format(",%s", para[i]));
            }
        }
        System.out.println();
    }
}
