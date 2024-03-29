////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

/*
 * Written by Jeff Dyer
 * Copyright (c) 1998-2003 Mountain View Compiler Company
 * All rights reserved.
 */

package macromedia.asc.parser;

import macromedia.asc.util.*;
import macromedia.asc.semantics.*;

/**
 * Node
 *
 * @author Jeff Dyer
 */
public class ClassDefinitionNode extends DefinitionNode
{
	public IdentifierNode name;
	public StatementListNode statements;
	public Node baseclass;
	public ReferenceValue baseref;
	public ListNode interfaces;
	public ReferenceValue ref;
	public ObjectList<FunctionCommonNode> fexprs;
	public ObjectList<ClassDefinitionNode> clsdefs;
	public ObjectList<Node> instanceinits;
	public ObjectList<FunctionCommonNode> staticfexprs;
	public int var_count;
	public int temp_count;
	public TypeValue cframe;
	public ObjectValue iframe;
	public int is_dynamic;
	public Context cx;
	public String debug_name;
    public String package_name;
    public boolean owns_cframe;
    public boolean needs_init;
	public ObjectList<ClassDefinitionNode> deferred_subclasses;
    public ExpressionStatementNode init;
    public boolean needs_prototype_ns;

    public Multinames imported_names = new Multinames();
	public Namespaces used_namespaces = new Namespaces(); // don't delete
    public Namespaces used_def_namespaces = new Namespaces(); // don't delete
	public ObjectValue private_namespace;
	public ObjectValue default_namespace;
    public ObjectValue public_namespace;
    public ObjectValue protected_namespace;
    public ObjectValue static_protected_namespace;

    public boolean is_default_nullable = true;

    public int state;
    public static final int INIT = 0;
    public static final int INHERIT = 1;
    public static final int MAIN = 2;

	public Namespaces namespaces = new Namespaces();

	public Node initializerStatement(Context cx)
	{
		return this;
	}

	public ClassDefinitionNode(Context cx, PackageDefinitionNode pkgdef, AttributeListNode attrs, IdentifierNode name, Node baseclass, ListNode interfaces, StatementListNode statements)
	{
		super(pkgdef, attrs, -1);
		this.name = name;
		this.baseclass = baseclass;
		this.interfaces = interfaces;
		this.statements = statements;
		ref = null;
		cframe = null;
		iframe = null;
		var_count = 0;
		temp_count = 0;
		staticfexprs = null;
		this.cx = cx;
		state = INIT;
        needs_prototype_ns = true;

        private_namespace = null;
		default_namespace = null;
        public_namespace = null;
        protected_namespace = null;
        package_name = "";
	}

	public Value evaluate(Context cx, Evaluator evaluator)
	{
		if (evaluator.checkFeature(cx, this))
		{
			return evaluator.evaluate(cx, this);
		}
		else
		{
			return null;
		}
	}

	public Node first()
	{
		return statements.first();
	}

	public Node last()
	{
		return statements.last();
	}

	public boolean isConst()
	{
		return true;
	}
	
	public boolean isInterface() 
	{
		return false;
	}


	public String toString()
	{
      if(Node.useDebugToStrings)
         return "ClassDefinition@" + pos() + (name != null ? ": " + name.toString() : "");
      else
         return "ClassDefinition";
	}
}
