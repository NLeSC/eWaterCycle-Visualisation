package nl.esciencecenter.visualization.ewatercycle.data;

import java.util.ArrayList;
import java.util.List;

import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class NetcdfVariable {
    private final Variable variable;
    private final List<NetcdfFile> files;

    private NetcdfVariable(NetcdfFile file, Variable variable) {
        this.variable = variable;
        this.files = new ArrayList<NetcdfFile>();
        this.files.add(file);
    }

    public NetcdfVariable(Variable variable, List<NetcdfFile> files) {
        ArrayList<NetcdfFile> filesContainingVariable = new ArrayList<NetcdfFile>();

        for (NetcdfFile currentFile : files) {
            List<Variable> variablesContainedInFile = currentFile.getVariables();
            if (variablesContainedInFile.contains(variable)) {
                filesContainingVariable.add(currentFile);
            }
        }

        this.variable = variable;
        this.files = filesContainingVariable;
    }

    public NetcdfVariable(NetcdfFile file) throws NoSingleVariableInFileException {
        List<Variable> variablesContainedInFile = file.getVariables();

        List<Variable> variablesContainedinFileThatAreNotDimensions = new ArrayList<Variable>();

        for (Variable currentVariable : variablesContainedInFile) {
            if (!isAlsoADimensionInThisFile(file, currentVariable)) {
                variablesContainedinFileThatAreNotDimensions.add(currentVariable);
            }
        }

        if (variablesContainedinFileThatAreNotDimensions.size() == 1) {
            this.variable = variablesContainedinFileThatAreNotDimensions.get(0);
        } else if (variablesContainedinFileThatAreNotDimensions.size() > 1) {
            throw new NoSingleVariableInFileException("multiple found");
        } else {
            throw new NoSingleVariableInFileException("none found");
        }

        this.files = new ArrayList<NetcdfFile>();
        this.files.add(file);
    }

    public static boolean isAlsoADimensionInThisFile(NetcdfFile file, Variable var) {
        List<Dimension> dimensionsContainedInFile = file.getDimensions();
        boolean isNotDimension = true;
        for (Dimension currentDimension : dimensionsContainedInFile) {
            if (currentDimension.getName().compareTo(var.getFullName()) == 0) {
                isNotDimension = false;
            }
        }
        return !isNotDimension;
    }

    public static List<NetcdfVariable> getAllVariablesThatAreNotDimensionsFromThisFile(NetcdfFile file) {
        List<NetcdfVariable> result = new ArrayList<NetcdfVariable>();

        List<Variable> variablesContainedInFile = file.getVariables();
        List<Variable> variablesContainedinFileThatAreNotDimensions = new ArrayList<Variable>();

        for (Variable currentVariable : variablesContainedInFile) {
            if (!isAlsoADimensionInThisFile(file, currentVariable)) {
                variablesContainedinFileThatAreNotDimensions.add(currentVariable);
            }
        }

        if (variablesContainedinFileThatAreNotDimensions.size() > 0) {
            for (Variable var : variablesContainedinFileThatAreNotDimensions) {
                result.add(new NetcdfVariable(file, var));
            }
        }

        return result;
    }

}
