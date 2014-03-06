package cz.muni.fi.ngmon.logtranslator.translator;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * This is failsafe logging framework. It comes in actions when some class is extending other
 * in which is Log defined. It might be impossible to find correct logging system, so we assume all
 * possibilities of log methods.
 */
public class FailsafeLoggerLoader extends LoggerLoader {
    private Collection<String> translateLogMethods;
    private Collection checkerLogMethods;
    private List<String> levels = Arrays.asList("trace", "debug", "info", "warn", "error", "fatal", "", "log");

    public FailsafeLoggerLoader() {
        super();
        List<String> imports = LoggerFactory.getActualLoggingImports();
        setLogger(null);
        setLogFactory(null);  // take care, log4j has no logFactory

        // Does log4j has any custom methods? Add them into this list.
        List<String> failsafeCustomizedMethods = Arrays.asList("log", "entry", "printf");
        this.checkerLogMethods = generateCheckerMethods(levels);
        this.translateLogMethods = generateTranslateMethods(levels, failsafeCustomizedMethods);
        // TODO translate or remove isEnabled method checker!
        this.translateLogMethods.add("isEnabled");

    }


    @Override
    public Collection getTranslateLogMethods() {
        return translateLogMethods;
    }

    @Override
    public Collection getCheckerLogMethods() {
        return checkerLogMethods;
    }

    public String[] getFactoryInitializations() {
        return new String[]{"LogManager.getLogger"};
    }
}
