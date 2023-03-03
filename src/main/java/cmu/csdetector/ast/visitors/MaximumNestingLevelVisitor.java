package cmu.csdetector.ast.visitors;

import org.eclipse.jdt.core.dom.*;

public class MaximumNestingLevelVisitor extends ASTVisitor {
	
	private int MNL = 0;
	
	private int tempMNL = 0;
	
	public boolean visit(CatchClause node) {
		tempMNL++;
		if(node.getBody()!=null){
			node.getBody().accept(this);
			updateMNL();
		}
		tempMNL--;
		return false;
	}
	
	public boolean visit(ConditionalExpression node) {	
		tempMNL++;
		if(node.getThenExpression()!=null){
			node.getThenExpression().accept(this);
			updateMNL();
		}
		if(node.getElseExpression()!=null){
			node.getElseExpression().accept(this);
			updateMNL();
		}
		tempMNL--;
		return false;
	}
	
	public boolean visit(DoStatement node) {
		tempMNL++;
		if(node.getBody()!=null){
			node.getBody().accept(this);
			updateMNL();
		}
		tempMNL--;
		return false;
	}
	
	public boolean visit(ForStatement node) {
		tempMNL++;
		if(node.getBody()!=null){
			node.getBody().accept(this);
			updateMNL();
		}
		tempMNL--;
		return false;
	}
	
	public boolean visit(EnhancedForStatement node) {
		tempMNL++;
		if(node.getBody()!=null){
			node.getBody().accept(this);
			updateMNL();
		}
		tempMNL--;
		return false;
	}
	
	public boolean visit(IfStatement node) {
		tempMNL++;
		if(node.getThenStatement()!=null){
			node.getThenStatement().accept(this);
			updateMNL();
		}
		if(node.getElseStatement()!=null){
			node.getElseStatement().accept(this);
			updateMNL();
		}
		tempMNL--;
		return false;
	}

	public boolean visit(WhileStatement node) {
		tempMNL++;
		if(node.getBody()!=null){
			node.getBody().accept(this);
			updateMNL();
		}
		tempMNL--;
		return false;
	}
	
	public Double getMNL(){
		return new Double(MNL);
	}
	
	private void updateMNL(){
		if(tempMNL>MNL){
			MNL=tempMNL;
		}		
	}	
}