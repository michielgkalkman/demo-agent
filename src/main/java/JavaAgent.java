import javassist.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.MethodInfo;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaAgent {
    private static final Logger log = Logger.getLogger(JavaAgent.class.getName());

    public static void main() {

    }

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

        ClassPool pool = ClassPool.getDefault();
        Loader cl = new Loader(pool);

        cl.addTranslator(pool, new Translator() {
            @Override
            public void start(ClassPool pool) {

            }

            @Override
            public void onLoad(ClassPool pool, String classname) {
                log.info(String.format("Agent:Loading ...... %s", classname));
            }
        });

        instrumentation.addTransformer( new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader classLoader, String s, Class<?> aClass, ProtectionDomain protectionDomain, byte[] bytes) {

                log.info( String.format("Agent:START en STOP .... %s", s));
                // Javassist
                try {
                    ClassPool cp = ClassPool.getDefault();

                    cp.importPackage("java.util");

                    final Iterator<String> importedPackages = cp.getImportedPackages();
                    while(importedPackages.hasNext()) {
                        log.info(String.format("Agent:Imported: .... %s", s));
                        importedPackages.next();
                    }

                    CtClass cc = cp.get(s.replace('/', '.'));
                    if( cc == null) {
                        log.info(String.format("Agent:Cannot find: %s", s));
                    } else if( cc.isInterface()) {
                        log.info(String.format("Agent:Cannot transform interface: %s", s));
                    } else {
                        log.info(String.format("Agent: .. %s ( frozen: %b, interface: %b, " +
                                        "annotation: %b, kotlin: %b, " +
                                        "array: %b, enum: %b, modified: %b, primitive: %b )",
                            cc.getName(), cc.isFrozen(), cc.isInterface(), cc.isAnnotation(),
                            cc.isKotlin(),
                            cc.isArray(), cc.isEnum(), cc.isModified(), cc.isPrimitive()
                        ));

                        if( isTransform(cc)) {
                            annotate(cc);
                        }
                        byte[] byteCode = cc.toBytecode();
                        cc.detach();
                        return byteCode;
                    }
                } catch (Exception ex) {
                    log.log(Level.SEVERE, "Agent!!!" + ex.getLocalizedMessage(), ex);
                }

                return null;
            }

            @Override
            public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                log.info( String.format("Agent:MODULE .... %s", module.getName()));
                return transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
            }
        }, true);

        log.info(String.format("Agent:Is retransformation supported: %b", instrumentation.isRetransformClassesSupported()));

        log.info(String.format("Agent:Is File modifiable: %b", instrumentation.isModifiableClass(File.class)));

        instrumentation.retransformClasses(
                File.class);


    }

    private static boolean isTransform(CtClass cc) {
        log.info(String.format("Agent:check %s::%s", cc.getPackageName() == null ? "-no package-" : cc.getPackageName() , cc.getSimpleName()));
        return
                !(
                    "java.lang".equals(cc.getPackageName())
                    || (cc.getPackageName() == null && "JavaAgent".equals(cc.getSimpleName()))
                );
    }

    private static void annotate(CtClass cc) throws CannotCompileException {
        for (CtMethod m : cc.getMethods()) {

            log.info(String.format("Agent:Method: %s, empty: %b", m.getLongName(), m.isEmpty()));

            log.info(String.format("Agent:Method: %s::%s", m.getDeclaringClass().getSimpleName(), m.getLongName()));

            if(
                    doTransform(m)) {


                final MethodInfo methodInfo = m.getMethodInfo();
                final List<AttributeInfo> attributes = methodInfo.getAttributes();
                for( AttributeInfo attributeInfo : attributes) {
                    log.info( String.format( "Agent:attribute %s::%s", m.getLongName(), attributeInfo.getName()));
                }



                StringBuilder stringBuilder = new StringBuilder();

                CtClass[] parameterTypes;

                try {
                    parameterTypes = m.getParameterTypes();
                } catch (NotFoundException e) {
                    parameterTypes = null;
                }

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

                m.insertAfter(String.format(
                        "System.out.println(\"END   %s\");", m.getLongName()));
            }
        }
        processConstructors(cc);
    }

    private static void processConstructors(CtClass cc) {
        for (CtConstructor m : cc.getConstructors()) {
            try {
                if (
                        doTransform(m)) {
                    log.info(String.format("Agent:Constructor: %s, empty: %b, constructor: %b, classinitializer: %b",
                            m.getLongName(), m.isEmpty(), m.isConstructor(), m.isClassInitializer()));

                    StringBuilder stringBuilder = new StringBuilder();

                    CtClass[] parameterTypes;

                    try {
                        parameterTypes = m.getParameterTypes();
                    } catch (NotFoundException e) {
                        parameterTypes = null;
                    }

                    if (parameterTypes != null) {
                        stringBuilder.append("{");
                        final String statement2 = String.format("System.out.println(\"+++++ $%d;%s;\" + $%d);", 0, cc.getName(), 0);
                        log.info(String.format("Agent?%s", statement2));
                        for (int i = 1; i <= parameterTypes.length; i++) {
                            log.info(String.format("Agent??%d,%s", i, parameterTypes[i - 1].getName()));
                            final String statement;
//                            if( "double".equals(parameterTypes[i-1].getName() )) {
//                                statement = String.format("{ System.out.println(\"+++++ $%d;%s;\" + Double.toString($%d)); }",
//                                        i, parameterTypes[i - 1].getName(), i);
//                            } else if( "float".equals(parameterTypes[i-1].getName())) {
//                                statement = String.format("{ System.out.println(\"+++++ $%d;%s;\" + Float.toString($%d)); }",
//                                        i, parameterTypes[i - 1].getName(), i);
//                                statement = String.format("System.out.println(\"+++++ $%d;%s;\");",
//                                        i, parameterTypes[i - 1].getName());
//                            } else {
                                statement = String.format("System.out.println(\"+++++ $%d;%s;\" + $%d);",
                                        i, parameterTypes[i - 1].getName(), i);
//                            }
                            log.info(String.format("Agent?%s", statement));
                            stringBuilder
                                .append(statement);
                        }
                        stringBuilder.append("}");
                    }

                    String before = stringBuilder.toString();

                    final String beforeString = String.format(
                            "System.out.println(\"START %s\");%s" +
                                    "", m.getLongName(), before);
                    log.info(String.format("Agent:%s", beforeString));
                    m.insertBefore(beforeString);

                    m.insertAfter(String.format(
                            "System.out.println(\"END   %s\");", m.getLongName()));
                }
            } catch( Throwable throwable) {
                log.severe( "Throwable " + throwable.getLocalizedMessage());
                log.log(Level.SEVERE, "Agent!!!!" + throwable.getLocalizedMessage(), throwable);
            }
        }
    }

    private static boolean doTransform(CtBehavior m) {
        final String packageName = m.getDeclaringClass().getPackageName();
        return !(

                (("java.lang".equals(packageName)
                        && m.getDeclaringClass().getSimpleName().equals("Object"))
                        && !Modifier.isAbstract(m.getModifiers()))
                ||
                    (packageName != null && packageName.startsWith("jdk.internal"))
                ||
                    (m.getMethodInfo().getAccessFlags() & AccessFlag.ABSTRACT)  != 0
                );
    }
}
