package org.ngmon.logger.logtranslator.generator;

import org.ngmon.logger.logtranslator.common.Log;
import org.ngmon.logger.logtranslator.common.LogFile;
import org.ngmon.logger.logtranslator.common.Utils;
import org.ngmon.logger.logtranslator.ngmonLogging.LogTranslatorNamespace;

public class HelperGenerator {

    private static LogTranslatorNamespace LOG = Utils.getLogger();


    /**
     * Generate method name from 'comments' list - strings found in given log method call.
     * If comment list and variable list is empty, use autogenerated method name from property file.
     *
     * @param log to generate and set method log from
     */
    public static void generateMethodName(Log log) {
        if (log.getComments().size() == 0) {
            StringBuilder tempName = new StringBuilder();
            for (LogFile.Variable var : log.getVariables()) {
                if (var.getNgmonName() != null) {
                    tempName.append(var.getNgmonName());
                } else {
                    tempName.append(var.getName());
                }
            }
            int maxLengthUtils = Utils.getNgmonEmptyLogStatementMethodNameLength();
            int maxLength = (tempName.length() < maxLengthUtils) ? tempName.length() : maxLengthUtils;
            if (tempName.length() > 0) {
                log.setMethodName(tempName.substring(0, maxLength));
            } else {
                log.setMethodName(tempName.substring(0, maxLength) + Utils.getNgmonEmptyLogStatement());
            }
        } else {
            StringBuilder logName = new StringBuilder();
            int counter = 0;
            int logNameLength = Utils.getNgmonLogLength();
            for (String comment : log.getComments()) {
                for (String str : comment.split(" ")) {
                    if (!Utils.BANNED_LIST.contains(str)) {
                        if (counter != 0) {
                            logName.append("_");
                        }
                        logName.append(str);
                        counter++;
                    }
                    if (counter >= logNameLength) break;
                }
            }
            if (Utils.itemInList(Utils.JAVA_KEYWORDS, logName.toString())) {
                System.out.println("logname=" + logName.toString());
                log.setMethodName(Utils.getNgmonEmptyLogStatement());
            } else {
                log.setMethodName(logName.toString());
            }
        }
    }


    /**
     * Generate new log method call which will be replaced by 'original' log method call.
     * This new log method will use NGMON logger. Which is goal of this mini-application.
     *
     * @param logName name of current logger variable (mostly "LOG")
     * @param log     current log to get information from
     * @return log method calling in NGMON's syntax form
     */
    public static String generateLogMethod(String logName, Log log) {
        // TODO/wish - if line is longer then 80 chars, append to newline!
        if (log != null) {
            // generate variables
            StringBuilder vars = new StringBuilder();
            StringBuilder tags = new StringBuilder();

            LOG.variablesInLog(log.getVariables().toString()).trace();
//            System.out.println("\t" + log.getVariables() + "\n\t" + log.getOriginalLog() );
            int j = 0;
            for (LogFile.Variable var : log.getVariables()) {
                if (var == null) {
                    System.out.println("dsao");
                }
                if (var.getChangeOriginalName() == null) {
                    vars.append(var.getName());
                } else {
                    vars.append(var.getChangeOriginalName());
                }
                // Append .toString() if variable is of any other type then NGMON allowed data types
                if (Utils.listContainsItem(Utils.NGMON_ALLOWED_TYPES, var.getType().toLowerCase()) == null) {
                    vars.append(".toString()");
                }

                if (j != log.getVariables().size() - 1) {
                    vars.append(", ");
                }
                j++;
            }

            // generate tags
            if (log.getTag() != null) {
                int tagSize = log.getTag().size();
                if (tagSize == 0) {
                    tags = null;
                } else {
                    for (int i = 0; i < tagSize; i++) {
                        tags.append(".tag(\"").append(log.getTag().get(0)).append("\")");
                    }
                }
            }
            String replacementLog = String.format("%s.%s(%s)%s.%s()", logName, log.getMethodName(), vars, tags, log.getLevel());
            LOG.replacementLogOriginalLog(replacementLog, log.getOriginalLog()).trace();
            log.setGeneratedReplacementLog(replacementLog);
            return replacementLog;
        } else {
            return null;
        }
    }

    public static String generateEmptySpaces(int numberOfSpaces) {
        StringBuilder spaces = new StringBuilder();
        while (numberOfSpaces > 0) {
            spaces.append(" ");
            numberOfSpaces--;
        }
        return spaces.toString();
    }

    public static String addStringTypeCast(String typecastMe) {
        if (!typecastMe.endsWith(".toString")) {
            return "String.valueOf(" + typecastMe + ")";
        } else {
            return typecastMe;
        }
    }


}
