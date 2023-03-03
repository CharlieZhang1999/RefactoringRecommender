package cmu.csdetector;

import cmu.csdetector.console.ToolParameters;
import cmu.csdetector.console.output.ObservableExclusionStrategy;
import cmu.csdetector.metrics.MethodMetricValueCollector;
import cmu.csdetector.metrics.TypeMetricValueCollector;
import cmu.csdetector.resources.Method;
import cmu.csdetector.resources.loader.JavaFilesFinder;
import cmu.csdetector.resources.loader.SourceFile;
import cmu.csdetector.resources.loader.SourceFilesLoader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cmu.csdetector.resources.Type;
import org.apache.commons.cli.ParseException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CodeSmellDetector {

    public static void main(String[] args) throws IOException {
        CodeSmellDetector instance = new CodeSmellDetector();

        instance.start(args);

    }

    private void start(String[] args) throws IOException {
        ToolParameters parameters = ToolParameters.getInstance();

        try {
            parameters.parse(args);
        } catch (ParseException exception) {
            System.out.println(exception.getMessage());
            parameters.printHelp();

            System.exit(-1);
        }

        System.out.println(new Date());

        List<String> sourcePaths = List.of(parameters.getValue(ToolParameters.SOURCE_FOLDER));
        List<Type> allTypes = loadAllTypes(sourcePaths);

        System.out.println(allTypes);

        collectTypeMetrics(allTypes);
        saveSmellsFile(allTypes);

        //todo: we'll include the logic to collect the metrics and detect smells here

        System.out.println(new Date());

    }

    private List<Type> loadAllTypes(List<String> sourcePaths) throws IOException{
        List<Type> allTypes = new ArrayList<>();

        JavaFilesFinder sourceLoader = new JavaFilesFinder(sourcePaths);
        SourceFilesLoader compUnitLoader = new SourceFilesLoader(sourceLoader);  // Dependency Injection

        List<SourceFile> sourceFiles = compUnitLoader.getLoadedSourceFiles();
        for(SourceFile sourceFile: sourceFiles) {
            allTypes.addAll(sourceFile.getTypes());
        }

        return allTypes;
    }

    private void collectTypeMetrics(List<Type> types){ // classes or interfaces
        TypeMetricValueCollector collector;
        for (Type type: types){
            collector = new TypeMetricValueCollector();
            collector.collect(type);
            this.collectMethodMetrics(type);
        }
    }

    private void collectMethodMetrics(Type type){
        for (Method method: type.getMethods()){
            MethodMetricValueCollector collector = new MethodMetricValueCollector();
            collector.collect(method);
        }
    }


    private void saveSmellsFile(List<Type> smellyTypes) throws IOException {
        ToolParameters parameters = ToolParameters.getInstance();
        File smellsFile = new File(parameters.getValue(ToolParameters.SMELLS_FILE));
        BufferedWriter writer = new BufferedWriter(new FileWriter(smellsFile));
        System.out.println("Saving smells file...");

        GsonBuilder builder = new GsonBuilder();
        builder.addSerializationExclusionStrategy(new ObservableExclusionStrategy());
        builder.disableHtmlEscaping();
        builder.setPrettyPrinting();
        builder.serializeNulls();

        Gson gson = builder.create();
        gson.toJson(smellyTypes, writer);
        writer.close();
    }
}
