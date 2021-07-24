import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

public class JavaAgent {
    private static final Logger log = Logger.getLogger(JavaAgent.class.getName());

    /**
     * As soon as the JVM initializes, This  method will be called.
     * Configs for intercepting will be read and added to Transformer so that Transformer will intercept when the
     * corresponding Java Class and Method is loaded.
     *
     * @param agentArgs       The list of agent arguments
     * @param instrumentation The instrumentation object
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) throws Throwable {

        log.info(String.format("Agent:Starting Java Agent...... %s", agentArgs == null ? "" : agentArgs));

        JavaAssistAgent.premain(agentArgs, instrumentation);
    }
}
