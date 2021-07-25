import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

public class JavaByteBuddyAgent {
    private static final Logger log = Logger.getLogger(JavaByteBuddyAgent.class.getName());

    public static void doPremain(String agentArgs, Instrumentation instrumentation) {
        new AgentBuilder.Default()
                .type(ElementMatchers.any())
                .transform((builder, type, classLoader, module) -> {
                    final DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<?> intercept =
                            builder.method(ElementMatchers.isMethod())
                                .intercept(MethodDelegation.to(LogInterceptor.class)
                                    .andThen(SuperMethodCall.INSTANCE));

                    log.info("Intercepted " + type.getCanonicalName());

                    return intercept;
                })
                .installOn(instrumentation);

    }
}
