package cz.muni.fi.ngmon.logtranslator.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This object represents .java file with all variables found.
 * Variable contains more detailed information about specific
 * variable in list.
 *
 * LogFile is used for incorporating Log instances belonging to same
 * namespace.
 *
 */
public class LogFile {

    // Mapping of variableName : <variableProperties>
    private static Map<String, List<Variable>> variableList = new LinkedHashMap<>();
    private String fileName;
    private String namespace;
    private List<Log> logs;

    public LogFile() {
        logs = new ArrayList<>();
    }
    public static Map<String, List<Variable>> getVariableList() {
        return variableList;
    }


    public void putVariableList(String variableName, Variable variable) {
        List<Variable> vars;
        if (getProperties(variableName) != null) {
            vars = getProperties(variableName);
        } else {
            vars = new ArrayList<>();
        }
        vars.add(variable);
        variableList.put(variableName, vars);
    }

    public List<Variable> getProperties(String variableName) {
        return variableList.get(variableName);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public List<Log> getLogs() {
        return logs;
    }

    public void addLog(Log log) {
        this.logs.add(log);
    }

    @Override
    public String toString() {
        return "LogFile{" +
                "variableList=" + variableList +
                ", fileName='" + fileName + '\'' +
                '}';
    }

    public class Variable {
        private String name; // ?
        private String type;
        private int lineNumber;
        private int startPosition;
        private int stopPosition;
        private int fileStartPosition;
        private int fileStopPosition;
        private boolean isField; // true if variable is declared in class, not in method body

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        public int getStartPosition() {
            return startPosition;
        }

        public void setStartPosition(int startPosition) {
            this.startPosition = startPosition;
        }

        public int getStopPosition() {
            return stopPosition;
        }

        public void setStopPosition(int stopPosition) {
            this.stopPosition = stopPosition;
        }

        public int getFileStartPosition() {
            return fileStartPosition;
        }

        public void setFileStartPosition(int fileStartPosition) {
            this.fileStartPosition = fileStartPosition;
        }

        public int getFileStopPosition() {
            return fileStopPosition;
        }

        public void setFileStopPosition(int fileStopPosition) {
            this.fileStopPosition = fileStopPosition;
        }

        public boolean isField() {
            return isField;
        }

        public void setField(boolean isField) {
            this.isField = isField;
        }

        @Override
        public String toString() {
            return "Variable{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", lineNumber=" + lineNumber +
                    ", field=" + isField +

                    /* ", startPosition=" + startPosition +
                    ", stopPosition=" + stopPosition +
                    ", fileStartPosition=" + fileStartPosition +
                    ", fileStopPosition=" + fileStopPosition + */
                    '}';
        }
    }
}
