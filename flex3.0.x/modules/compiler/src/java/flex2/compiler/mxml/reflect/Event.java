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

package flex2.compiler.mxml.reflect;

/**
 *
 */
public interface Event
{
	String getName();

	String getTypeName();

	Type getType();
	
	String getDeprecatedMessage();
	
	String getDeprecatedReplacement();
	
	String getDeprecatedSince();
}
