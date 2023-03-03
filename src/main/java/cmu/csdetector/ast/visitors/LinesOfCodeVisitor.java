package cmu.csdetector.ast.visitors;

import org.eclipse.jdt.core.dom.*;

public class LinesOfCodeVisitor extends ASTVisitor {

	private Integer loc;
	
	public Integer getLoc() {
		return loc;
	}
	
	public LinesOfCodeVisitor() {
		this.loc = 0;
	}
	
	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(AnnotationTypeMemberDeclaration node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(AnonymousClassDeclaration node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(AssertStatement node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(BreakStatement node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(CatchClause node) {
		loc++;
		return true;
	}


	@Override
	public boolean visit(ContinueStatement node) {
		loc++;
		return true;
	}


	@Override
	public boolean visit(DoStatement node) {
		loc++;
		return true;
	}


	@Override
	public boolean visit(EnhancedForStatement node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(EnumConstantDeclaration node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(EnumDeclaration node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(ExpressionStatement node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(FieldDeclaration node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(ForStatement node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(IfStatement node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(ImportDeclaration node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(Initializer node) {
		loc++;
		return true;
	}


	@Override
	public boolean visit(LabeledStatement node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(LambdaExpression node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(MarkerAnnotation node) {
		loc++;
		return true;
	}


	@Override
	public boolean visit(MethodDeclaration node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(NormalAnnotation node) {
		loc++;
		return true;
	}


	@Override
	public boolean visit(PackageDeclaration node) {
		loc++;
		return true;
	}


	@Override
	public boolean visit(ReturnStatement node) {
		loc++;
		return true;
	}


	@Override
	public boolean visit(SingleMemberAnnotation node) {
		loc++;
		return true;
	}


	@Override
	public boolean visit(SuperConstructorInvocation node) {
		loc++;
		return true;
	}


	@Override
	public boolean visit(SwitchCase node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(SwitchStatement node) {
		loc++;
		return true;
	}


	@Override
	public boolean visit(ThrowStatement node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(TryStatement node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(TypeDeclarationStatement node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(TypeLiteral node) {
		loc++;
		return true;
	}


	@Override
	public boolean visit(VariableDeclarationStatement node) {
		loc++;
		return true;
	}

	@Override
	public boolean visit(WhileStatement node) {
		loc++;
		return true;
	}


	
}
