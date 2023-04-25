package cmu.csdetector.extractor;

import cmu.csdetector.resources.Resource;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class SignatureRecommender {

    private ExtractedMethod em;

    private Resource resource;

    private CompilationUnit cu;

    private AST ast;

    public SignatureRecommender(ExtractedMethod em, Resource resource, CompilationUnit cu) {
        this.em = em;
        this.resource = resource;
        this.cu = cu;
        this.ast = AST.newAST(AST.JLS11);
    }

    private String[] recommendParameters(boolean isFeatureEnvy) {
        return new String[]{""};

    }

    private String[] recommendReturnTypes() {
        return new String[]{""};
    }
}
