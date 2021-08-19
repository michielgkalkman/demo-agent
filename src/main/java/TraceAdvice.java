import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

public class TraceAdvice {

    @Advice.OnMethodEnter
    static void onEnter(@Advice.Origin Method method) {
        // Avoid '+' string concatenation because of https://github.com/raphw/byte-buddy/issues/740
        System.out.println("[+] ".concat(method.toString()));
    }

    @Advice.OnMethodExit
    static void onExit(@Advice.Origin Method method) {
        // Avoid '+' string concatenation because of https://github.com/raphw/byte-buddy/issues/740
        System.out.println("[-] ".concat(method.toString()));
    }
}
