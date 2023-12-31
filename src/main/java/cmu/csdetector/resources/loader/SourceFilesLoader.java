package cmu.csdetector.resources.loader;

import cmu.csdetector.ast.ASTBuilder;
import cmu.csdetector.console.ConsoleProgressMonitor;
import org.eclipse.jdt.core.dom.ASTParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SourceFilesLoader {

	private List<SourceFile> sourceFiles;

	private JavaFilesFinder loader;

	public SourceFilesLoader(JavaFilesFinder loader) throws IOException {
		this.loader = loader;
		this.sourceFiles = new ArrayList<>();
		this.load(loader.findAll());
	}

	public SourceFilesLoader(JavaFilesFinder loader, List<File> files) throws IOException {
		this.loader = loader;
		this.sourceFiles = new ArrayList<>();
		this.load(files);
	}

	public SourceFilesLoader(JavaFilesFinder loader, File file) throws IOException {
		this.loader = loader;
		this.sourceFiles = new ArrayList<>();
		this.load(Arrays.asList(file));
	}

	private void load(List<File> sourceFiles) throws IOException {
		String[] files = new String[sourceFiles.size()];
		for (int i = 0; i < sourceFiles.size(); i++) {
			files[i] = sourceFiles.get(i).getAbsolutePath();
		}

		String[] sourcePaths = loader.getSourcePaths();
		ASTBuilder builder = new ASTBuilder(sourcePaths);
		ASTParser parser = builder.create();

		SourceFileASTRequestor requestor = new SourceFileASTRequestor();
		parser.createASTs(files, null, new String[0], requestor, null);

		this.sourceFiles = requestor.getSourceFiles();
	}

	public List<SourceFile> getLoadedSourceFiles() {
		return sourceFiles;
	}

}
