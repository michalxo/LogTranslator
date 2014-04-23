package org.ngmon.logger.logtranslator.generator;

import org.ngmon.logger.logtranslator.common.Log;
import org.ngmon.logger.logtranslator.common.LogFile;
import org.ngmon.logger.logtranslator.common.Utils;
import org.ngmon.logger.logtranslator.translator.CommonsLoggerLoader;
import org.ngmon.logger.logtranslator.translator.Slf4jLoggerLoader;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * // comment
 * //"originalLog":"LOG.info(\"Processing \"+count+\" messages from DataNodes \"+\"that were previously queued during standby state\")"}}}
 * org.hadooop.XYZ.replacementLog##Processing <INT:count> messages from DataNodes that were previously queued during standby state.")
 */
public class GoMatchGenerator {

    private static Set<String> goMatchPatternList = new HashSet<>();
    private static int waveCounter = 0;

    public static void createGoMatch(List<LogFile> logFiles) {
        for (LogFile logFile : logFiles) {
            for (Log log : logFile.getLogs()) {
                createGoMatchFromLog(log);
            }
        }
    }

    public static String getGoMatchPatternListToString() {
        StringBuilder output = new StringBuilder();
        for (String pattern : goMatchPatternList) {
            output.append(pattern).append("\n");
        }
        return output.toString();
    }

    public static void createGoMatchFromLog(Log log) {
        log.getComments();
        String goMatchPattern = createNewPattern(log);

        /** if pattern does not contain any variable, throw him away */
        if (goMatchPattern.contains("<") && goMatchPattern.contains(">")) {


            if (Utils.goMatchDebug) {
                // prepend with comment and original log file
                goMatchPattern = "# " + log.getOriginalLog() + "\n" + goMatchPattern;
            }

            goMatchPatternList.add(goMatchPattern);
            log.setGoMatchLog(goMatchPattern);
        }
    }

    /**
     * Create new Go-Match pattern using log object, which
     * holds all information about log.
     *
     * @param log create new gomatch pattern from this log
     * @return created new goMatch pattern
     */
    private static String createNewPattern(Log log) {
        StringBuilder pattern = new StringBuilder(Utils.getApplicationNamespace() + "." + log.getMethodName() + "##");

        String originalLog = log.getOriginalLog();
        if (Utils.goMatchWorkaround) {
            for (String escaped : Utils.JAVA_ESCAPE_CHARS) {
                String substitute;
                switch (escaped) {
                    case "\t":
                    case "\n":
                        substitute = " ";
                        break;
                    case "\\\"":
                    case "\\\'":
                    case "\\":
                        continue;
                    default:
                        substitute = "";
                        break;
                }
                originalLog = originalLog.replaceAll(escaped, substitute);
            }
        }

        if (originalLog.startsWith("LogFactory")) {
            // get substring from method name (info/debug..)
            for (String level : Utils.DEFAULT_LOG_LEVELS) {
                if (originalLog.contains("." + level + "(")) {
                    originalLog = originalLog.substring(originalLog.indexOf(level));
                    break;
                }
            }
        }
        originalLog = originalLog.substring(originalLog.indexOf("("), originalLog.lastIndexOf(")"));
        originalLog = extractVars(originalLog, log);

        List<String> variableNames = new ArrayList<>();
        for (LogFile.Variable var : log.getVariables()) {

            /** Rename duplicated variable names */
            String varName;
            if (var.getNgmonName() != null) {
                varName = var.getNgmonName();
            } else {
                varName = var.getName();
            }

            if (variableNames.contains(varName)) {
                // get all varNames, which have same name + numbered suffix, getLast and add last+1 into map
                int occurrence = 0;
                for (String vName : variableNames) {
                    if (vName.startsWith(varName) && (vName.length() > varName.length())) {
                        int tmp = Integer.parseInt(vName.substring(varName.length()));
                        if (tmp > occurrence) {
                            occurrence = tmp;
                        }
                    }
                }
                varName = varName + (occurrence + 1);
            }
            variableNames.add(varName);

            String type = var.getType();
            if (!Utils.itemInList(Utils.NGMON_ALLOWED_TYPES, type.toLowerCase())) {
                type = "String";
            }
            originalLog = originalLog.replaceFirst("~", "<" + type.toUpperCase() + ":" + varName + ">");
        }
        // TODO check slf4j and common on behaviour!
        if (log.getGeneratedReplacementLog().contains("tag(\"methodCall\")") && originalLog.contains("~")) {
            originalLog = originalLog.replaceAll("~", "");
        }

        if (originalLog.contains("~")) {
            waveCounter++;
            System.out.println(waveCounter + "=" + originalLog + " \n" + log.getOriginalLog());
        }

        /** goMatch artificial removal of \n\t\r.. and adding extra space before & after pattern */
        if (Utils.goMatchWorkaround) {
            for (String escaped : Utils.JAVA_ESCAPE_CHARS) {
                originalLog = originalLog.replaceAll(escaped, "");
            }
            // non-greedy find goMatch pattern and add spaces before/after it
            originalLog = goMatchAddSpaces(originalLog);
        }
        pattern.append(originalLog.trim());

        return pattern.toString();
    }

    /**
     * Method adds extra spaces (when needed to add one) and
     * remove escaping characters from string.
     *
     * @param text goMatch pattern to be processed
     * @return processed text with empty spaces
     */
    private static String goMatchAddSpaces(String text) {
        Pattern pattern = Pattern.compile("<[A-Z]+:.*?>");
        Matcher matcher = pattern.matcher(text);

        String tempText = text;
        while (matcher.find()) {
            int startPos = matcher.start();
            int endPos = matcher.end();

            if (startPos == 0) {
                startPos = 1;
            }
            if (endPos == text.length()) {
                endPos = text.length() - 1;
            }
            if ((text.charAt(startPos - 1) != ' ') && (text.charAt(endPos) != ' ')) {
                tempText = tempText.replace(matcher.group(), " " + matcher.group() + " ");
            } else if (text.charAt(startPos - 1) != ' ') {
                tempText = tempText.replace(matcher.group(), " " + matcher.group());
            } else if (text.charAt(endPos - 1) != ' ') {
                tempText = tempText.replace(matcher.group(), matcher.group() + " ");
            }
        }
        text = tempText.trim().replace("  ", " ");
        return text;
    }

    /**
     * Extract commentary text from log statement.
     * Handle special case of formatted variables
     *
     * @param text to extract variables from
     * @param log  containing all information about this log
     * @return altered text without variables
     */
    private static String extractVars(String text, Log log) {
        List<LogFile.Variable> formattedVariables = log.getFormattedVariables();
        String symbol = log.getFormattingSymbol();
        /** THE WORST FIX POSSIBLE FOR DUMBEST LOG USAGE EVER -> LOG.info(s = "sigma=" + sigma") where s is String!! Used twice! */
        if (text.startsWith("(s=")) {
            text = text.replace("s=", "");
        }
        String textNoFormatters = null;
        StringBuilder cleanText = new StringBuilder();

        /** If text contains slf4j's formatter brackets {}, don't extract variables
         manually */
        if (formattedVariables != null && symbol != null) {
//            System.out.println("NULL SYMBOL! " + log.getOriginalLog());
            if (symbol.equals("%")) {
                textNoFormatters = CommonsLoggerLoader.isolateFormatters(text, formattedVariables);
                textNoFormatters = textNoFormatters.replace("%%", "%");
            } else if (symbol.equals("{}") || symbol.equals("{0}")) {
                // could be slf4j or MessageFormatter - they use similar syntax
                textNoFormatters = Slf4jLoggerLoader.isolateFormatters(text, formattedVariables, symbol);
            }
            text = textNoFormatters;
        }

        /** remove ternary operator */
        if (log.getTag() != null) {
            if (log.getTag().contains("ternary-operator")) {
//                System.out.println(log.getOriginalLog());
                int questionMarkPos = text.indexOf("?");
                int startPos = text.indexOf(log.getTernaryValues().get(0));
                int endPos = text.indexOf(":" + log.getTernaryValues().get(2)) + log.getTernaryValues().get(2).length();
                if (startPos < questionMarkPos && endPos > questionMarkPos) {
                    // replace ternary Bool variable by ~ and expTrue,False remove
                    String tempText = text.substring(startPos, endPos + 1);
                    if (formattedVariables != null) {
                        text = text.replace(tempText, "");
                    } else {
                        text = text.replace(tempText, "~");
                    }
                }
            }
        }

        Set<String> tags = log.getTag();
        if (tags != null) {
            if (tags.contains("methodCall") || tags.contains("mathExp")) {
                List<String> sortedVars = new ArrayList<>();
                for (LogFile.Variable unsortedVar : log.getVariables()) {
                    if (unsortedVar.getNgmonName() != null) {
                        sortedVars.add(unsortedVar.getName());
                    }
                }
                Collections.sort(sortedVars, new StringLengthComparator());
                for (String var : sortedVars) {
                    if (var.contains("("))
                        text = text.replace(var, "~");
                }
            }
        }


        /** If not, continue manually */
        boolean parseComment = false;
        if (text == null) {
            System.out.println("null text=" + log);
            text = log.getMethodName();
        }
        char c_prev = text.charAt(0);
        for (char c : text.substring(1).toCharArray()) {
            if (c == '\"' && c_prev != '\\') {
                parseComment = !parseComment;
            }
            if (parseComment) {
                if (cleanText.length() != 0) {
                    // add empty space between variable and string
                    if (c_prev == '\"' && cleanText.charAt(cleanText.length() - 1) != ' ' && textNoFormatters != null) {
                        cleanText.append(" ");
                    }
                }
                if (c_prev == '\\' && c == '\"') {
                    cleanText.delete(cleanText.length() - 1, cleanText.length());
                    cleanText.append('\"');
                }
                if (c != '"') {
                    cleanText.append(c);
                }
                // if we don't skip variables, change them to "_"
            } else {
                if (c == ',') {
                    cleanText.append(" ");
                } else if (c == '\"') {
                    continue;
//                } else if (c == '~') {
//                    cleanText.append("@~@");
                } else if (c != '+') {
                    cleanText.append("~");
                }
            }
            c_prev = c;
        }

        /** remove multiple empty spaces or underscore chars, remove all non alphanum */
        return cleanText.toString().replaceAll("[^A-Za-z0-9,': \\[\\]_]->#\\(\\)", "").replaceAll("  +", " ").replaceAll("~+", "~");
//        return clean;
    }


}