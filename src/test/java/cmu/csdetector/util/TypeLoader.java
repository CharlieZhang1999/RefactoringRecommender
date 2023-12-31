package cmu.csdetector.util;

import cmu.csdetector.resources.Type;
import cmu.csdetector.resources.loader.JavaFilesFinder;
import cmu.csdetector.resources.loader.SourceFile;
import cmu.csdetector.resources.loader.SourceFilesLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TypeLoader {

	public static List<Type> loadAllFromDir(File sourcePath) throws IOException {
		JavaFilesFinder finder = new JavaFilesFinder(sourcePath.getAbsolutePath());
		SourceFilesLoader loader = new SourceFilesLoader(finder);
		List<Type> types = new ArrayList<>();
		for (SourceFile source : loader.getLoadedSourceFiles()) {
			types.addAll(source.getTypes());
		}
		return types;
	}

	public static Type getTargetClassType(String targetClass) throws IOException {
		File testPath = new File("src/test/java/cmu/csdetector/dummy/smells");
		var types = TypeLoader.loadAllFromDir(testPath);
		GenericCollector.collectAll(types);

		// get the target class
		var targetClassTypeOpt = types.stream().filter((t) -> t.getFullyQualifiedName().equals(targetClass)).findFirst();
		if (targetClassTypeOpt.isEmpty()) {
			throw new IOException(targetClass + " not found");
		}
		return targetClassTypeOpt.get();
	}
}
