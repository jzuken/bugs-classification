package org.ml_methods_group.common.ast.normalization;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.ml_methods_group.common.ast.NodeType;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class ASTProcessor {

    private final TreeContext context;

    protected ASTProcessor(TreeContext context) {
        this.context = context;
    }

    protected ITree createNode(NodeType type, String label) {
        return context.createTree(type.ordinal(), label, type.humanReadableName);
    }

    protected ITree visit(ITree node) {
        NodeType type;
        try {
            type = NodeType.valueOf(node.getType());
        } catch (InvalidParameterException ex) {
            throw new IllegalStateException(
                    String.format("Node type is not found for node:\n%s\n context:\n%s",
                            node.toPrettyString(context),
                            context.toString()));
        }
        if (type == null) {
            throw new IllegalStateException();
        }
        String ps = node.toPrettyString(context);
        
        ITree newTree;
        switch (type) {
            case NONE:
                newTree = visitNone(node);
             break; 
 			case ANONYMOUS_CLASS_DECLARATION:
                newTree = visitAnonymousClassDeclaration(node);
             break; 
 			case ARRAY_ACCESS:
                newTree = visitArrayAccess(node);
             break; 
 			case ARRAY_CREATION:
                newTree = visitArrayCreation(node);
             break; 
 			case ARRAY_INITIALIZER:
                newTree = visitArrayInitializer(node);
             break; 
 			case ARRAY_TYPE:
                newTree = visitArrayType(node);
             break; 
 			case ASSERT_STATEMENT:
                newTree = visitAssertStatement(node);
             break; 
 			case ASSIGNMENT:
                newTree = visitAssignment(node);
             break; 
 			case BLOCK:
                newTree = visitBlock(node);
             break; 
 			case BOOLEAN_LITERAL:
                newTree = visitBooleanLiteral(node);
             break; 
 			case BREAK_STATEMENT:
                newTree = visitBreakStatement(node);
             break; 
 			case CAST_EXPRESSION:
                newTree = visitCastExpression(node);
             break; 
 			case CATCH_CLAUSE:
                newTree = visitCatchClause(node);
             break; 
 			case CHARACTER_LITERAL:
                newTree = visitCharacterLiteral(node);
             break; 
 			case CLASS_INSTANCE_CREATION:
                newTree = visitClassInstanceCreation(node);
             break; 
 			case COMPILATION_UNIT:
                newTree = visitCompilationUnit(node);
             break; 
 			case CONDITIONAL_EXPRESSION:
                newTree = visitConditionalExpression(node);
             break; 
 			case CONSTRUCTOR_INVOCATION:
                newTree = visitConstructorInvocation(node);
             break; 
 			case CONTINUE_STATEMENT:
                newTree = visitContinueStatement(node);
             break; 
 			case DO_STATEMENT:
                newTree = visitDoStatement(node);
             break; 
 			case EMPTY_STATEMENT:
                newTree = visitEmptyStatement(node);
             break; 
 			case EXPRESSION_STATEMENT:
                newTree = visitExpressionStatement(node);
             break; 
 			case FIELD_ACCESS:
                newTree = visitFieldAccess(node);
             break; 
 			case FIELD_DECLARATION:
                newTree = visitFieldDeclaration(node);
             break; 
 			case FOR_STATEMENT:
                newTree = visitForStatement(node);
             break; 
 			case IF_STATEMENT:
                newTree = visitIfStatement(node);
             break; 
 			case IMPORT_DECLARATION:
                newTree = visitImportDeclaration(node);
             break; 
 			case INFIX_EXPRESSION:
                newTree = visitInfixExpression(node);
             break; 
 			case INITIALIZER:
                newTree = visitInitializer(node);
             break; 
 			case JAVADOC:
                newTree = visitJavadoc(node);
             break; 
 			case LABELED_STATEMENT:
                newTree = visitLabeledStatement(node);
             break; 
 			case METHOD_DECLARATION:
                newTree = visitMethodDeclaration(node);
             break; 
 			case METHOD_INVOCATION:
                newTree = visitMethodInvocation(node);
             break; 
 			case NULL_LITERAL:
                newTree = visitNullLiteral(node);
             break; 
 			case NUMBER_LITERAL:
                newTree = visitNumberLiteral(node);
             break; 
 			case PACKAGE_DECLARATION:
                newTree = visitPackageDeclaration(node);
             break; 
 			case PARENTHESIZED_EXPRESSION:
                newTree = visitParenthesizedExpression(node);
             break; 
 			case POSTFIX_EXPRESSION:
                newTree = visitPostfixExpression(node);
             break; 
 			case PREFIX_EXPRESSION:
                newTree = visitPrefixExpression(node);
             break; 
 			case PRIMITIVE_TYPE:
                newTree = visitPrimitiveType(node);
             break; 
 			case QUALIFIED_NAME:
                newTree = visitQualifiedName(node);
             break; 
 			case RETURN_STATEMENT:
                newTree = visitReturnStatement(node);
             break; 
 			case SIMPLE_NAME:
                newTree = visitSimpleName(node);
             break; 
 			case SIMPLE_TYPE:
                newTree = visitSimpleType(node);
             break; 
 			case SINGLE_VARIABLE_DECLARATION:
                newTree = visitSingleVariableDeclaration(node);
             break; 
 			case STRING_LITERAL:
                newTree = visitStringLiteral(node);
             break; 
 			case SUPER_CONSTRUCTOR_INVOCATION:
                newTree = visitSuperConstructorInvocation(node);
             break; 
 			case SUPER_FIELD_ACCESS:
                newTree = visitSuperFieldAccess(node);
             break; 
 			case SUPER_METHOD_INVOCATION:
                newTree = visitSuperMethodInvocation(node);
             break; 
 			case SWITCH_CASE:
                newTree = visitSwitchCase(node);
             break; 
 			case SWITCH_STATEMENT:
                newTree = visitSwitchStatement(node);
             break; 
 			case SYNCHRONIZED_STATEMENT:
                newTree = visitSynchronizedStatement(node);
             break; 
 			case THIS_EXPRESSION:
                newTree = visitThisExpression(node);
             break; 
 			case THROW_STATEMENT:
                newTree = visitThrowStatement(node);
             break; 
 			case TRY_STATEMENT:
                newTree = visitTryStatement(node);
             break; 
 			case TYPE_DECLARATION:
                newTree = visitTypeDeclaration(node);
             break; 
 			case TYPE_DECLARATION_STATEMENT:
                newTree = visitTypeDeclarationStatement(node);
             break; 
 			case TYPE_LITERAL:
                newTree = visitTypeLiteral(node);
             break; 
 			case VARIABLE_DECLARATION_EXPRESSION:
                newTree = visitVariableDeclarationExpression(node);
             break; 
 			case VARIABLE_DECLARATION_FRAGMENT:
                newTree = visitVariableDeclarationFragment(node);
             break; 
 			case VARIABLE_DECLARATION_STATEMENT:
                newTree = visitVariableDeclarationStatement(node);
             break; 
 			case WHILE_STATEMENT:
                newTree = visitWhileStatement(node);
             break; 
 			case INSTANCEOF_EXPRESSION:
                newTree = visitInstanceofExpression(node);
             break; 
 			case LINE_COMMENT:
                newTree = visitLineComment(node);
             break; 
 			case BLOCK_COMMENT:
                newTree = visitBlockComment(node);
             break; 
 			case TAG_ELEMENT:
                newTree = visitTagElement(node);
             break; 
 			case TEXT_ELEMENT:
                newTree = visitTextElement(node);
             break; 
 			case MEMBER_REF:
                newTree = visitMemberRef(node);
             break; 
 			case METHOD_REF:
                newTree = visitMethodRef(node);
             break; 
 			case METHOD_REF_PARAMETER:
                newTree = visitMethodRefParameter(node);
             break; 
 			case ENHANCED_FOR_STATEMENT:
                newTree = visitEnhancedForStatement(node);
             break; 
 			case ENUM_DECLARATION:
                newTree = visitEnumDeclaration(node);
             break; 
 			case ENUM_CONSTANT_DECLARATION:
                newTree = visitEnumConstantDeclaration(node);
             break; 
 			case TYPE_PARAMETER:
                newTree = visitTypeParameter(node);
             break; 
 			case PARAMETERIZED_TYPE:
                newTree = visitParameterizedType(node);
             break; 
 			case QUALIFIED_TYPE:
                newTree = visitQualifiedType(node);
             break; 
 			case WILDCARD_TYPE:
                newTree = visitWildcardType(node);
             break; 
 			case NORMAL_ANNOTATION:
                newTree = visitNormalAnnotation(node);
             break; 
 			case MARKER_ANNOTATION:
                newTree = visitMarkerAnnotation(node);
             break; 
 			case SINGLE_MEMBER_ANNOTATION:
                newTree = visitSingleMemberAnnotation(node);
             break; 
 			case MEMBER_VALUE_PAIR:
                newTree = visitMemberValuePair(node);
             break; 
 			case ANNOTATION_TYPE_DECLARATION:
                newTree = visitAnnotationTypeDeclaration(node);
             break; 
 			case ANNOTATION_TYPE_MEMBER_DECLARATION:
                newTree = visitAnnotationTypeMemberDeclaration(node);
             break; 
 			case MODIFIER:
                newTree = visitModifier(node);
             break; 
 			case UNION_TYPE:
                newTree = visitUnionType(node);
             break; 
 			case DIMENSION:
                newTree = visitDimension(node);
             break; 
 			case LAMBDA_EXPRESSION:
                newTree = visitLambdaExpression(node);
             break; 
 			case INTERSECTION_TYPE:
                newTree = visitIntersectionType(node);
             break; 
 			case NAME_QUALIFIED_TYPE:
                newTree = visitNameQualifiedType(node);
             break; 
 			case CREATION_REFERENCE:
                newTree = visitCreationReference(node);
             break; 
 			case EXPRESSION_METHOD_REFERENCE:
                newTree = visitExpressionMethodReference(node);
             break; 
 			case SUPER_METHOD_REFERENCE:
                newTree = visitSuperMethodReference(node);
             break; 
 			case TYPE_METHOD_REFERENCE:
                newTree = visitTypeMethodReference(node);

             break; 
 			case MY_MEMBER_NAME:
                newTree = visitMyMemberName(node);
             break; 
 			case MY_ALL_CLASSES:
                newTree = visitMyAllClasses(node);
             break; 
 			case MY_PATH_NAME:
                newTree = visitMyPathName(node);
             break; 
 			case MY_METHOD_INVOCATION_ARGUMENTS:
                newTree = visitMyMethodInvocationArguments(node);
             break; 
 			case MY_VARIABLE_NAME:
                newTree = visitMyVariableName(node);

             break; 
 			case C_BLOCK:
                newTree = visitCBlock(node);
             break; 
 			case C_NAME:
                newTree = visitCName(node);
            break;
            case C_COMMENT:
                newTree = visitComment(node);
             break; 
            default:
                newTree = defaultVisit(node);
        }
       // if(! ps.equals(newTree.toPrettyString(context)))
       //     System.out.println( "id=" + node.getId() +" [" +  ps +"] --> [" + newTree.toPrettyString(context) +"]"  );
        return newTree;
    }

    protected ITree visitNone(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitAnonymousClassDeclaration(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitArrayAccess(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitArrayCreation(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitArrayInitializer(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitArrayType(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitAssertStatement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitAssignment(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitBlock(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitBooleanLiteral(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitBreakStatement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitCastExpression(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitCatchClause(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitCharacterLiteral(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitClassInstanceCreation(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitCompilationUnit(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitConditionalExpression(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitConstructorInvocation(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitContinueStatement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitDoStatement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitEmptyStatement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitExpressionStatement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitFieldAccess(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitFieldDeclaration(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitForStatement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitIfStatement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitImportDeclaration(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitInfixExpression(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitInitializer(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitJavadoc(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitLabeledStatement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitMethodDeclaration(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitMethodInvocation(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitNullLiteral(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitNumberLiteral(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitPackageDeclaration(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitParenthesizedExpression(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitPostfixExpression(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitPrefixExpression(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitPrimitiveType(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitQualifiedName(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitReturnStatement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitSimpleName(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitSimpleType(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitSingleVariableDeclaration(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitStringLiteral(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitSuperConstructorInvocation(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitSuperFieldAccess(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitSuperMethodInvocation(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitSwitchCase(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitSwitchStatement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitSynchronizedStatement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitThisExpression(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitThrowStatement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitTryStatement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitTypeDeclaration(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitTypeDeclarationStatement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitTypeLiteral(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitVariableDeclarationExpression(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitVariableDeclarationFragment(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitVariableDeclarationStatement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitWhileStatement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitInstanceofExpression(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitLineComment(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitBlockComment(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitComment(ITree node) {
        return defaultVisit(node);
    }


    protected ITree visitTagElement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitTextElement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitMemberRef(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitMethodRef(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitMethodRefParameter(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitEnhancedForStatement(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitEnumDeclaration(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitEnumConstantDeclaration(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitTypeParameter(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitParameterizedType(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitQualifiedType(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitWildcardType(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitNormalAnnotation(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitMarkerAnnotation(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitSingleMemberAnnotation(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitMemberValuePair(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitAnnotationTypeDeclaration(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitAnnotationTypeMemberDeclaration(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitModifier(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitUnionType(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitDimension(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitLambdaExpression(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitIntersectionType(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitNameQualifiedType(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitCreationReference(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitExpressionMethodReference(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitSuperMethodReference(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitTypeMethodReference(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitMyMemberName(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitMyPathName(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitMyAllClasses(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitMyMethodInvocationArguments(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitMyVariableName(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitCName(ITree node) {
        return defaultVisit(node);
    }

    protected ITree visitCBlock(ITree node) {
        return defaultVisit(node);
    }

    protected ITree defaultVisit(ITree node) {
        final List<ITree> children = node.getChildren();
        final List<ITree> generated = children.stream()
                .map(this::visit)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!children.equals(generated)) {
            node.setChildren(generated);
        }
        return node;
    }
}
