import javassist.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.MethodInfo;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
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

        log.info(String.format("Agent:Starting Java Agent...... %s", agentArgs == null ? "" : agentArgs));

        instrumentation.addTransformer( new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader classLoader, String s, Class<?> aClass, ProtectionDomain protectionDomain, byte[] bytes) {

                log.info( String.format("Agent:START en STOP .... %s", s));
                // Javassist
                try {
                    ClassPool cp = ClassPool.getDefault();
                    CtClass cc = cp.get(s.replace('/', '.'));
                    if( cc == null) {
                        log.info(String.format("Agent:Cannot find: %s", s));
                    } else if( cc.isInterface()) {
                        log.info(String.format("Agent:Cannot transform interface: %s", s));
                    } else {
                        log.info(String.format("Agent: .. %s ( frozen: %b, interface: %b)",
                                cc.getName(), cc.isFrozen(), cc.isInterface()));

                        if( isTransform(cc)) {
                            annotate(cc);
                        }
                    }
                    byte[] byteCode = cc.toBytecode();
                    cc.detach();
                    return byteCode;
                } catch (Exception ex) {
                    log.log(Level.SEVERE, "Agent:" + ex.getLocalizedMessage(), ex);
                }

                return null;
            }
        }, true);

        log.info(String.format("Agent:Is retransformation supported: %b", instrumentation.isRetransformClassesSupported()));

        log.info(String.format("Agent:Is File modifiable: %b", instrumentation.isModifiableClass(File.class)));

        instrumentation.retransformClasses(
                File.class);


    }

    private static boolean isTransform(CtClass cc) {
        log.info(String.format("Agent:check %s::%S", cc.getPackageName(), cc.getSimpleName()));
        return
                !(
                    "java.lang".equals(cc.getPackageName())
                    || (cc.getPackageName() == null && "JavaAgent".equals(cc.getSimpleName()))
                );
    }

    private static void annotate(CtClass cc) throws CannotCompileException {
        for (CtMethod m : cc.getMethods()) {
            log.info(String.format("Agent:Method: %s::%s", m.getDeclaringClass().getSimpleName(), m.getLongName()));

            if(
                    !("java.lang".equals(m.getDeclaringClass().getPackageName()) &&
                      m.getDeclaringClass().getSimpleName().equals("Object") )
                            &&
                    !Modifier.isAbstract(m.getModifiers())) {


                final MethodInfo methodInfo = m.getMethodInfo();
                final List<AttributeInfo> attributes = methodInfo.getAttributes();
                for( AttributeInfo attributeInfo : attributes) {
                    log.info( String.format( "Agent:attribute %s::%s", m.getLongName(), attributeInfo.getName()));
                }



                CtClass[] parameterTypes;

                try {
                    parameterTypes = m.getParameterTypes();
                } catch (NotFoundException e) {
                    parameterTypes = null;
                }

                StringBuilder stringBuilder = new StringBuilder();

                if( parameterTypes != null) {
                    stringBuilder.append("{");
                    for (int i = 0; i <= parameterTypes.length; i++) {
                        if( i ==0 && (m.getMethodInfo().getAccessFlags() & AccessFlag.STATIC) != 0) {
                         log.info(String.format( "static: %s", m.getLongName()));
                        } else {
                            if( i != 0) {
                                stringBuilder
                                        .append(String.format("System.out.println(\"===== $%d;%s;\" + $%d);", i, parameterTypes[i - 1].getName(), i));
                            }
                        }
                    }
                    stringBuilder.append("}");
                }

                String before = stringBuilder.toString();

                final String beforeString = String.format(
                        "System.out.println(\"START %s\");%s" +
                                "", m.getLongName(), before);
                log.info( String.format("Agent:%s", beforeString));
                m.insertBefore(beforeString);

//                m.insertBefore("{ System.out.println($1); System.out.println($2); }");
                m.insertAfter(String.format(
                        "System.out.println(\"END   %s\");", m.getLongName()));
            }
        }
        for (CtConstructor m : cc.getConstructors()) {
            m.insertBefore(String.format(
                    "System.out.println(\"START %s\");", m.getLongName()));
            m.insertAfter(String.format(
                    "System.out.println(\"END   %s\");", m.getLongName()));
        }
    }

    public static void hello() {
        System.out.println("Hello");
    }
}
