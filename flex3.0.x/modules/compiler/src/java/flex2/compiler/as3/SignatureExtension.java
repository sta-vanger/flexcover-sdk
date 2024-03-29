////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.as3;

import java.io.File;
import java.io.IOException;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

import macromedia.asc.parser.Node;
import macromedia.asc.parser.PackageDefinitionNode;
import macromedia.asc.parser.ProgramNode;
import macromedia.asc.util.Context;
import flash.util.ExceptionUtil;
import flash.util.FileUtils;
import flex2.compiler.CompilationUnit;
import flex2.compiler.as3.reflect.NodeMagic;
import flex2.compiler.as3.reflect.TypeTable;
import flex2.compiler.common.CompilerConfiguration;
import flex2.compiler.util.MimeMappings;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.compiler.util.CompilerMessage.CompilerWarning;

/**
 * Singleton.
 * 
 * Compiler extension to generate AS3 class/interface signatures.
 * 
 * Signatures will not be generated unless -incremental or -keep-generated-signatures are true.
 * 
 * This even generates signatures for InMemoryFiles, etc.. Signatures may be emitted to the file
 * system as .sig files. Like keep-generated-actionscript, this doesn't flush the directory of
 * existing .sig files.
 * 
 * Related compiler configurating settings:
 *      compiler.keep-generated-signatures
 *      compiler.signature-directory [default: generated-signatures]
 * 
 * This class is NOT thread-safe. Not that this matters...
 *      
 * @author Jono Spiro
 */
public final class SignatureExtension implements Extension
{
    public final static boolean debug = false;

    public static final String DEFAULT_SIG_DIR   = "generated-signatures",
                               WARNING_ATTRIBUTE = "SignatureExtension.warning";

    /**
     * Don't mess with this directly, please. Mess with it indirectly using init() :~)
     */
    public static String signatureDirectory;
    
    /** singleton instance */
    private static SignatureExtension _instance;

    
    
    /** private since we're a singleton */
    private SignatureExtension() {}
    
    

    /**
     * Be sure that you init() the class before using it.
     */
    public static SignatureExtension getInstance()
    {
        if (_instance == null)
            _instance = new SignatureExtension();
            
        return _instance;
    }
    
    

    /**
     * Initializes the singleton; be sure you do this before using the extension.
     */
    public static void init(CompilerConfiguration compilerConfig)
    {
        assert compilerConfig != null;
        
        if (compilerConfig.getKeepGeneratedSignatures())
        {
            final String tmp = compilerConfig.getSignatureDirectory();
            signatureDirectory = ((tmp == null) ? DEFAULT_SIG_DIR : tmp);
        }
        else
        {
            signatureDirectory = null;
        }
    }
    
    
    
    private void setWarning(CompilationUnit unit, CompilerWarning warning)
    {
        unit.getContext().setAttribute(WARNING_ATTRIBUTE, warning);
    }
    
    private CompilerWarning getWarning(CompilationUnit unit)
    {
        return (CompilerWarning) unit.getContext().getAttribute(WARNING_ATTRIBUTE);
    }
    
    
    
//    /**
//     * For external use (API.java) to generate a signature checksum on the fly,
//     * without emitting errors, updating the CompilationUnit, or saving the signature to a file.
//     */
//    public static Long getSignatureChecksum(final CompilationUnit unit)
//    {
//        final Long chksum = computeChecksum(unit, generateSignature(unit));
//        
//        // generateSignature might log a warning if the signature could not be generated
//        // since this method is supposed to be side-effectless.
//        if (chksum == null)
//            unit.getContext().setAttribute(WARNING_ATTRIBUTE, null);
//        
//        return chksum;
//    }
    
    
    // interface methods
    public void parse1  (CompilationUnit unit, TypeTable typeTable) { doSignatureGeneration(unit); }
    public void parse2  (CompilationUnit unit, TypeTable typeTable) {}
    public void analyze1(CompilationUnit unit, TypeTable typeTable) {}
    public void analyze2(CompilationUnit unit, TypeTable typeTable) {}
    public void analyze3(CompilationUnit unit, TypeTable typeTable) {}
    public void analyze4(CompilationUnit unit, TypeTable typeTable) {}
    public void generate(CompilationUnit unit, TypeTable typeTable)
    {
        // since warnings are common when a file is malformed, most unsuccessful compilations
        // will have some kind of signature warning... the only REAL warnings that are interesting
        // are those that occur on well-formed programs that compile. only output warnings if the
        // application was able to successfully compile.
        
        // ADDITIONALLY, we'll only even attempt to output warnings IF we got to code generation.
        
        // disabled for RTM since this has been active for a few betas and proven itself;
        // even if warnings occur, we default to safe behavior.
        //
        // no reason to harass our customers anymore ;-)
        if (debug)
        {
            final CompilerWarning warning = getWarning(unit);
            if((warning != null) && (ThreadLocalToolkit.errorCount() == 0))
            {
                ThreadLocalToolkit.log(warning);
            }
        }
    }
    
    
    
    
    private void doSignatureGeneration(final CompilationUnit unit)
    {
        // debug("doSignatureGeneration(" + unit.getSource().getName() + ")");
        
        // generate the signature
        final String sigString = generateSignature(unit);

        // computer and store the checksum of the signature
        {
            final Long checksum = computeChecksum(unit, sigString);
            
            // make sure we've never parsed this unit before -- we don't want to waste passes
            //TODO concern: Source.copy() means that if you recompile a copied source, this
            //              assertion will explode. Unsure IF that can/should happen;
            //              unless CU.resetKeepTypeInfo() or CU.reset() clears the signature
            //              and is always called before recompiling a copied unit.
            // this should never, ever happen in production compiler -- and has never happened so far, to boot.
            // so I've upgraded it to an assertion, to be sure that we'd catch it during development.
            assert (!unit.hasSignatureChecksum())
                        : "SignatureExtension is overwriting an existing checksum of "
                          + unit.getSignatureChecksum()
                          + " with "
                          + checksum
                          + ". Please email the Flex SDK team.";
            
            unit.setSignatureChecksum(checksum);
        }
        
        // dump signature to filesystem
        // If the siggen failed, a file won't get created. I would LIKE to
        // write a file saying *** FAILED *** or some such, but if the package or class
        // name was one of the problems, then the generated file name is unreliable
        // (since it is based on the package name) and could overwrite an existing,
        // valid, signature file
        if ((sigString != null) && (signatureDirectory != null))
        {
            final ProgramNode           pNode    = (ProgramNode)unit.getSyntaxTree();
            final PackageDefinitionNode pdn      = (PackageDefinitionNode)pNode.pkgdefs.first();
            final String                pkgName  = NodeMagic.getPackageName(pdn).replace('.', '_');
            // final VirtualFile           vFile    = unit.getSource().getBackingFile();
            
            // this is only kinda sketchy, but it works... all I need is the file name, not the path
            final String fileName = new File(unit.getSource().getNameForReporting()).getName();
            
            // older sketchy method
            // URI uri;
            // try
            // {
                // apparently a URL is-a URI, therefore, this should never fail
                // except... some vFiles are in-memory, which give us a "memory://" URI, invalid
            //     uri      = new URI(vFile.getURL());
            //     fileName = new File(uri).getName();
            // }
            // catch(URISyntaxException e)
            // {
            //     fileName = new File(vFile.getName()).getName();
            // }
            
            final String srcName
                 = fileName.substring(0, (fileName.length() -
                                          MimeMappings.getExtension(unit.getSource().getMimeType()).length()));
            
            final String sigName = pkgName.concat(pkgName.equals("") ? "" : "_")
                                                  .concat(srcName)
                                                  .concat(".sig");
            
            try
            {
                //final String NL = SignatureEvaluator.NEWLINE;
                FileUtils.writeClassToFile(signatureDirectory, "", sigName,
                                           /* (("SOURCE: " + fileName + NL +
                                             "DIGEST: " + chksum   + NL +
                                             "-------------------" + NL + NL) + */
                                             sigString);
            }
            catch(IOException ioe)
            {
                final CompilerWarning warning = new KeepGeneratedSignatureFileWritingFailed(sigName);
                warning.setPath(unit.getSource().getNameForReporting());
                
                // ThreadLocalToolkit.log(warning);
                setWarning(unit, warning);
            }
        }
    }



    private static String generateSignature(final CompilationUnit unit)
    {
        final Context cx = (Context)unit.getContext().getAttribute("cx");
        
        String sigString = null;
        {
            // good estimate of buffer size for a signature
            final int powerOfTwoBufferSize
                    = (int)Math.pow(2, Math.round(Math.log(unit.getSource().size())/Math.log(2)));

            // generate the signature
            final SignatureEvaluator evaluator = new SignatureEvaluator(powerOfTwoBufferSize);
            evaluator.setLocalizationManager(ThreadLocalToolkit.getLocalizationManager());
            try
            {
                ((ProgramNode)unit.getSyntaxTree()).evaluate(cx, evaluator);
                sigString = evaluator.getSignature();
            }
            catch(SignatureAssertionRuntimeException e)
            {
                String message;
                if (debug)
                {
                    // full stack trace
                    message = ExceptionUtil.getStackTraceUpTo(e, "flex2.compiler.API");
                }
                else
                {
                    // ultra-compact-super-mini-trace of SignatureEvaluator.evaluator() line numbers
                    message = e.getMessage();
                    final StackTraceElement[] stack = e.getStackTrace();
                    for(int i = 0; i < stack.length; i++)
                    {
                        final StackTraceElement ste = stack[i];
                        if (ste.getClassName().endsWith("SignatureEvaluator") &&
                            ste.getMethodName().equals("evaluate"))
                        {
                            message += ":" + ste.getLineNumber();
                        }
                    }
                }
                
                final SignatureGenerationFailed warning
                        = new SignatureGenerationFailed(e.node, message);
                
                warning.setPath(unit.getSource().getNameForReporting());
                warning.setLine(cx.input.getLnNum(e.node.pos()));
                warning.setColumn(cx.input.getColPos(e.node.pos()));
                
                //ThreadLocalToolkit.log(warning);
                unit.getContext().setAttribute(WARNING_ATTRIBUTE, warning);
            }
        }
        return sigString;
    }


    private static Long computeChecksum(CompilationUnit unit, String sigString)
    {
        Long chksum = null;
        
        if(sigString != null)
        {
            //store the signature as a checksum
            final byte[] bytes = sigString.getBytes();
            
            final Checksum checksum = new Adler32(); // much faster than CRC32, almost as reliable
            checksum.update(bytes, 0, bytes.length);
            chksum = new Long(checksum.getValue());
            
           // debug("COMPUTE   CRC32: " + chksum + "\t--> " + unit.getSource().getNameForReporting());
        }
        return chksum;
    }
    
    
    
    public static class KeepGeneratedSignatureFileWritingFailed extends CompilerWarning
    {
        /** serialVersionUID */
        private static final long serialVersionUID = -2077778266808945113L;
     
        public String toFile;

        public KeepGeneratedSignatureFileWritingFailed(String toFile)
        {
            super();
            this.toFile = toFile;
        }
    }
    
    
    public static class SignatureGenerationFailed extends CompilerWarning
    {
        /** serialVersionUID */
        private static final long serialVersionUID = 7217554980556429606L;
        
        public Node   node;
        public String cause;
        
        public SignatureGenerationFailed(Node node, String cause)
        {
            super();
            this.node  = node;
            this.cause = cause;
        }
    }
    
} // class

