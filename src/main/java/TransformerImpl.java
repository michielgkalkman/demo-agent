import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.logging.Logger;

public class TransformerImpl implements ClassFileTransformer {
    private static final Logger log = Logger.getLogger(TransformerImpl.class.getName());

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        log.info(String.format("Transform...... %s", className));
        return transform(className, classfileBuffer);
    }

    public byte[] transform(String className, byte[] classfileBuffer) {
        log.info(String.format("Transform...... %s", className));
        return transform( className, classfileBuffer);
    }
}