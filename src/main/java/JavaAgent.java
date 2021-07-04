import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.Arrays;
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
    public static void premain(String agentArgs, Instrumentation instrumentation) throws UnmodifiableClassException {

        log.info(String.format("Starting Java Agent...... %s", agentArgs == null ? "" : agentArgs));

        instrumentation.addTransformer( new ClassFileTransformer() {
               @Override
               public byte[] transform(ClassLoader classLoader, String s, Class<?> aClass, ProtectionDomain protectionDomain, byte[] bytes) {

                   log.info(String.format("Transform '%s'", s));

                   if ("java/io/File".equals(s)) {
                       log.info( String.format("Transforming .... %s", s));
                       // Javassist
                       try {
                           ClassPool cp = ClassPool.getDefault();
                           CtClass cc = cp.get("java.io.File");
                           for( CtConstructor m : cc.getConstructors()) {
                               m.addLocalVariable("elapsedTime", CtClass.longType);
                               m.insertBefore("elapsedTime = System.currentTimeMillis();");
                               m.insertAfter("{elapsedTime = System.currentTimeMillis() - elapsedTime;"
                                       + "System.out.println(\"Method Executed in ms: \" + elapsedTime);}");
                           }
                           for( CtMethod m : cc.getDeclaredMethods()) {
//                           for( CtMethod m : cc.getMethods()) {
                               log.info(String.format("Processing %s", m.getLongName()));
                               m.addLocalVariable("elapsedTime", CtClass.longType);
                               StringBuilder stringBuilder = new StringBuilder();
                               stringBuilder.append("elapsedTime = System.currentTimeMillis();")
                                       .append( String.format("System.out.println(\"Method %s(", m.getLongName()));

                               final CtClass[] parameterTypes = m.getParameterTypes();
                               for(int i = 0; i< parameterTypes.length; i++) {
                                   if( i>0) {
                                       stringBuilder.append(',');
                                   }
                                   stringBuilder.append(parameterTypes[i].toString());
                               }

                                stringBuilder.append( ")\");");

                                m.insertBefore( stringBuilder.toString());

//                               m.insertBefore(String.format("elapsedTime = System.currentTimeMillis();"
//                                               + "System.out.println(\"Method %s Executing\");", m.getLongName()));
                               m.insertAfter(
                                       String.format(
                                       "{elapsedTime = System.currentTimeMillis() - elapsedTime;"
                                       + "System.out.println(\"Method %s Executed in ms: \" + elapsedTime);}"
                               , m.getLongName()));
                           }
//                           CtMethod m = cc.getDeclaredMethod("exists");
                           byte[] byteCode = cc.toBytecode();
                           cc.detach();
                           return byteCode;
                       } catch (Exception ex) {
                           ex.printStackTrace();
                       }
                   }

                   return null;
               }
           }, true);
        instrumentation.addTransformer( new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader classLoader, String s, Class<?> aClass, ProtectionDomain protectionDomain, byte[] bytes) {

                log.info( String.format("START en STOP .... %s", s));
                // Javassist
                try {
                    ClassPool cp = ClassPool.getDefault();
                    CtClass cc = cp.get(s);
                    if( cc == null) {
                        log.info(String.format(" Cannot find: %s", s));
                    } else {
                        log.info(String.format(" .. %s", cc.getName()));
                        for (CtConstructor m : cc.getConstructors()) {
                            m.insertBefore(String.format(
                                    "System.out.println(\"START %s\");", m.getLongName()));
                            m.insertAfter(String.format(
                                    "System.out.println(\"END   %s\");", m.getLongName()));
                        }
                        for (CtMethod m : cc.getDeclaredMethods()) {
                            m.insertBefore(String.format(
                                    "System.out.println(\"START %s\");", m.getLongName()));
                            m.insertAfter(String.format(
                                    "System.out.println(\"END   %s\");", m.getLongName()));
                        }
                    }
//                           CtMethod m = cc.getDeclaredMethod("exists");
                    byte[] byteCode = cc.toBytecode();
                    cc.detach();
                    return byteCode;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                return null;
            }
        }, true);

        log.info(String.format("Is retransformation supported: %b", instrumentation.isRetransformClassesSupported()));

        log.info(String.format("Is File modifiable: %b", instrumentation.isModifiableClass(File.class)));

        instrumentation.retransformClasses(
                File.class);


    }
}
