////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package macromedia.asc.util;

import macromedia.asc.embedding.CompilerHandler;
import macromedia.asc.embedding.avmplus.ByteCodeFactory;
import macromedia.asc.parser.NodeFactory;
import macromedia.asc.parser.MetaDataNode;
import macromedia.asc.semantics.Emitter;
import macromedia.asc.semantics.ObjectValue;
import macromedia.asc.semantics.TypeValue;
import macromedia.asc.semantics.Slot;

import java.util.*;

/**
 * @author Clement Wong
 */
public class ContextStatics
{
	// HACK: Flex hack -- setting this to false removes the "[Compiler] Error#..." header from error output.
	public static boolean useVerboseErrors = true;

	/* Used so that shellErrorString() and lint warnings return error strings that are
	     * system/punctuation/language independent. Used with -sanity switch.
	     * Suppresses most unnecesary output (including 'bytes written', etc.).
	     */
	public static boolean useSanityStyleErrors = false;

	// used to trigger individual warning callbacks for each warning via 
	// simpleLogWarnings (as useSanityStyleErrors still does). used by authoring. 
	public static boolean useSimpleLogWarnings = false;

	// used by authoring to omit trace statements
	public static boolean omitTrace = false;

	Emitter emitter;
	NodeFactory nodeFactory;
	ByteCodeFactory bytecodeFactory;
	ObjectList<ObjectValue> scopes = new ObjectList<ObjectValue>();
	public int withDepth = -1;
	public NamespacesTable internNamespaces = new NamespacesTable();
	public CompilerHandler handler = null;
	String pathspec;
	String scriptname;
	
	// FLEXCOVER: this list accumulates all coverage keys recorded by the instrumentation code, to be
	//   emitted later in a .cvm file.
	Set<String> coverageKeys = new LinkedHashSet<String>();

	public static final int LANG_EN		= 0;
	public static final int LANG_DE		= 1;
	public static final int LANG_ES		= 2;
	public static final int LANG_FR		= 3;
	public static final int LANG_IT		= 4;
	public static final int LANG_JP		= 5;
	public static final int LANG_KR		= 6;
	public static final int LANG_CN		= 7;
	public static final int LANG_TW		= 8;

	ObjectValue global;

	HashMap<String, TypeValue> builtins;
	HashMap<String, TypeValue> userDefined;
    HashMap<String, ObjectValue> namespaces;
    HashMap<String, ObjectValue> internal_namespaces;
    HashMap<String, ObjectValue> protected_namespaces;
    HashMap<String, ObjectValue> static_protected_namespaces;
    HashMap<String, ObjectValue> private_namespaces;

	Set<String> validImports;
    // maps ErrorCode to its localized error string.  Must not be static, there may be multiple different langauge contexts in use at the same time
    //  (on the flex server).
	public HashMap<Number,String> errorCodeMap = new HashMap<Number,String>();
    public  int languageID	= 0;

	public ObjectValue globalPrototype;

    private int nextSlotID = 1;
    private int expectedSlotID = -1;

    public int getNextSlotID()
    {
	    if (expectedSlotID != -1)
	    {
		    int id = expectedSlotID;
		    expectedSlotID = -1;
		    return id;
	    }
	    else
	    {
		    return nextSlotID++;
	    }
    }
    public void pushExpectedSlotID(int id)
    {
	    expectedSlotID = id;
    }

	int unresolved_ns_count = 0;
	public int ticket_count = 0;
	public boolean use_static_semantics = false;
    public int dialect = 9;

	ObjectValue _publicNamespace;
	ObjectValue _anyNamespace;
    TypeValue _noType;
    TypeValue _objectType;
    TypeValue _arrayType;
	TypeValue _voidType;
	TypeValue _nullType;
	TypeValue _booleanType;
	TypeValue _stringType;
	TypeValue _typeType;
	TypeValue _functionType;
	TypeValue _intType;
	TypeValue _uintType;
    TypeValue _numberType;
    TypeValue _xmlType;
	TypeValue _xmlListType;
    TypeValue _regExpType;

	public int errCount = 0;

	// C: This is for tracking recursive include path.
	public ObjectList<String> includePaths = new ObjectList<String>();

	public void clear()
	{
		if (builtins != null)
		{
			builtins.clear();
		}

        errorCodeMap.clear();
		_publicNamespace = null;
		_anyNamespace = null;
        _noType = null;
        _objectType = null;
        _arrayType = null;
		_voidType = null;
		_nullType = null;
		_booleanType = null;
		_stringType = null;
		_typeType = null;
		_functionType = null;
		_intType = null;
		_uintType = null;
        _numberType = null;
        _xmlType = null;
		_xmlListType = null;
        _regExpType = null;

        if (namespaces != null)
        {
	        namespaces.clear();
        }
        if (internal_namespaces != null)
        {
	        internal_namespaces.clear();
        }
        if (protected_namespaces != null)
        {
	        protected_namespaces.clear();
        }
        if (static_protected_namespaces != null)
        {
	        static_protected_namespaces.clear();
        }
        if (private_namespaces != null)
        {
	        private_namespaces.clear();
        }
		if (bytecodeFactory != null)
		{
			bytecodeFactory.clear();
		}
        errCount = 0;
		handler = null;

		unresolved_ns_count = 0;
		ticket_count = 0;

		if (validImports != null)
		{
			validImports.clear();
		}
	}

	public void reuse()
	{
		emitter = null;
		nodeFactory = null;

		if (bytecodeFactory != null)
		{
			bytecodeFactory.clear();
			bytecodeFactory = null;
		}

		if (scopes != null)
		{
			scopes.clear();
		}

		if (internNamespaces != null)
		{
			internNamespaces.clear();
		}

		handler = null;
		pathspec = null;
		scriptname = null;
		global = null;

		globalPrototype = null;

        errCount = 0;
		handler = null;

		unresolved_ns_count = 0;
		ticket_count = 0;

		includePaths.clear();

		if (builtins != null)
		{
			cleanSlots(builtins);
		}

		if (userDefined != null)
		{
			cleanSlots(userDefined);
		}

		if (validImports != null)
		{
			validImports.clear();
		}
	}

	private static void cleanSlots(HashMap<String, TypeValue> types)
	{
		for (Iterator<TypeValue> i = types.values().iterator(); i.hasNext();)
		{
			TypeValue value = i.next();
			for (int j = 0, length = (value.slots != null) ? value.slots.size() : 0; j < length; j++)
			{
				cleanSlot((Slot) value.slots.get(j));
			}

			ObjectValue ov = value.prototype;
			for (int j = 0, length = (ov != null && ov.slots != null) ? ov.slots.size() : 0; j < length; j++)
			{
				cleanSlot((Slot) ov.slots.get(j));
			}
		}
	}

	public static void cleanSlot(Slot slot)
	{
		if (slot != null)
		{
            final List<MetaDataNode> list = slot.getMetadata();
            if (list != null)
            {
                MetaDataNode dep = null;
                for (int i = 0, size = list != null ? list.size() : 0; i < size; i++)
                {
                    if( "Deprecated".equals(list.get(i).id) )
                    {
                        dep = list.get(i);
                    }
                }
                slot.setMetadata(null);
                
                // Have to keep "Deprecated" metadata so deprecated warnings work
                if( dep != null )
                    slot.addMetadata(dep);
                
                /*
			    List<MetaDataNode> list = slot.getMetadata();
			    for (int i = 0, size = list != null ? list.size() : 0; i < size; i++)
			    {
				    list.get(i).def = null;
			    }
                */
            }
            
            slot.setImplNode(null);
		}
	}
	
    public void removeNamespace(String name)
    {
    	// package name: e.g. mx.controls
    	// class name: e.g. mx.controls:Button
    	internal_namespaces.remove(name);
    	private_namespaces.remove(name);
    	protected_namespaces.remove(name);
    	static_protected_namespaces.remove(name);
    	namespaces.remove(name);
    }
    
    // FLEXCOVER: add a coverage key for later reporting
    //
    public void addCoverageKey(String key)
    {
        coverageKeys.add(key);
    }
}
