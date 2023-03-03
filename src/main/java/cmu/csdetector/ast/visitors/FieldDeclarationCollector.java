package cmu.csdetector.ast.visitors;

import cmu.csdetector.ast.CollectorVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;

public class FieldDeclarationCollector extends CollectorVisitor<FieldDeclaration> {

	@Override
	public boolean visit(FieldDeclaration node) {
		super.addCollectedNode(node);
		return false;
	}
}
