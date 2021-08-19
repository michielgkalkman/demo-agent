import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class JavaByteBuddyAgent {
    private static final Logger log = Logger.getLogger(JavaByteBuddyAgent.class.getName());

    public static void doPremain(String agentArgs, Instrumentation instrumentation) {
//        new AgentBuilder.Default()
//                .disableClassFormatChanges()
//                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
//
//                .type(ElementMatchers.any())
//
//                .transform((builder, type, classLoader, module) -> {
//                    final DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<?> intercept = builder
//                            .method(MethodDescription::isMethod)
//                             .intercept(Advice.to(LogInterceptor.class));
//
//                            System.out.println( "Transforming " + type.getCanonicalName());
//
//                            return intercept;
//                })
//                .installOn(instrumentation);


        // https://stackoverflow.com/questions/68715127/what-is-the-proper-way-to-instrument-classes-loaded-by-bootstrap-extension-cla#answer-68721148
        new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                // Make sure we see helpful logs
                .with(AgentBuilder.RedefinitionStrategy.Listener.StreamWriting.toSystemError())
                .with(AgentBuilder.Listener.StreamWriting.toSystemError().withTransformationsOnly())
                .with(AgentBuilder.InstallationListener.StreamWriting.toSystemError())
                .ignore(ElementMatchers.none())
                // Ignore Byte Buddy and JDK classes we are not interested in
                .ignore(
                        nameStartsWith("net.bytebuddy.")
                                .or(nameStartsWith("jdk.internal.reflect."))
                                .or(nameStartsWith("java.lang.invoke."))
                                .or(nameStartsWith("com.sun.proxy."))
                )
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .type(any())
                .transform((builder, type, classLoader, module) -> builder
                        .visit(Advice.to(TraceAdvice.class).on(isMethod()))
                ).installOn(instrumentation);
    }
}
