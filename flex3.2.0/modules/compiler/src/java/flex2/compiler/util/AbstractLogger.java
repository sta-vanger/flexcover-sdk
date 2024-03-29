////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.util;

import flex2.compiler.Logger;
import flex2.compiler.ILocalizableMessage;
import flash.localization.LocalizationManager;

/**
 * @author Roger Gonzalez
 */
public abstract class AbstractLogger implements Logger
{
    public boolean initialized = false;
    protected String INFO, WARNING, ERROR, COL, RECOMPILE, REASON, INCLUDEUPDATED, INCLUDEAFFECTED;


    public void init(LocalizationManager l10n)
    {
        if (initialized)
            return;
        initialized = true;
        if (l10n != null)
        {
            // todo - perhaps move this into the messages file itself using some sort of simple macro syntax?
            INFO = l10n.getLocalizedTextString( "flex2.compiler.util.AbstractLogger.Info");
            WARNING = l10n.getLocalizedTextString( "flex2.compiler.util.AbstractLogger.Warning");
            ERROR = l10n.getLocalizedTextString( "flex2.compiler.util.AbstractLogger.Error");
            COL = l10n.getLocalizedTextString("flex2.compiler.util.AbstractLogger.col");
            RECOMPILE = l10n.getLocalizedTextString("flex2.compiler.util.AbstractLogger.Recompile");
            REASON = l10n.getLocalizedTextString("flex2.compiler.util.AbstractLogger.Reason");
	        INCLUDEUPDATED = l10n.getLocalizedTextString("flex2.compiler.util.AbstractLogger.IncludeUpdated");
	        INCLUDEAFFECTED = INCLUDEUPDATED;
        }

        if (INFO == null)
        {
            INFO = "Info";
        }

        if (WARNING == null)
        {
            WARNING = "Warning";
        }

        if (ERROR == null)
        {
            ERROR = "Error";
        }

        if (COL == null)
        {
            COL = "col";
        }

        if (RECOMPILE == null)
        {
            RECOMPILE = "Recompile";
        }

        if (REASON == null)
        {
            REASON = "Reason";
        }

	    if (INCLUDEUPDATED == null)
	    {
		    INCLUDEUPDATED = "Updated";
		    INCLUDEAFFECTED = INCLUDEUPDATED;
	    }
    }

    public String formatPrefix( LocalizationManager l10n, ILocalizableMessage m )
    {
        init( l10n );
        StringBuffer prefix = new StringBuffer();
        if (m.getPath() != null)
        {
            prefix.append( m.getPath() );
            if (m.getLine() != -1)
            {
                prefix.append( '(' );
                prefix.append( m.getLine() );
                prefix.append( ")" );

                if (m.getColumn() != -1)
                {
                    prefix.append( ": " );
                    prefix.append( COL );
                    prefix.append( ": " );
                    prefix.append( m.getColumn() );
                }
            }
            prefix.append( ": " );
        }
        if (m.getLevel() == ILocalizableMessage.ERROR)
        {
            prefix.append( ERROR );
            prefix.append( ": ");
        }
        else if (m.getLevel() == ILocalizableMessage.WARNING)
        {
            prefix.append( WARNING );
            prefix.append( ": " );
        }
        else if (m.getLevel() == ILocalizableMessage.INFO)
        {
        }
        else
        {
            assert false : "Unhandled ILocalizableMessage type";
        }

        return prefix.toString();
    }

	protected String formatExceptionDetail(ILocalizableMessage m, LocalizationManager loc)
	{
		String exText = null;
		Exception ex = m.getExceptionDetail();
		if (ex != null)
		{
			if (ex instanceof ILocalizableMessage)
			{
				exText = loc.getLocalizedTextString( m.getExceptionDetail() );
			}
			else
			{
				exText = ex.getMessage();
				if (exText == null)
				    exText = ex.getClass().getName();
			}
		}
		exText = exText == null ? "" : ": " + exText;
		return exText;
	}

    public void setLocalizationManager( LocalizationManager mgr )
    {
        localizationManager = mgr;
    }

    public LocalizationManager getLocalizationManager()
    {
        return localizationManager;
    }

    private LocalizationManager localizationManager;
}
