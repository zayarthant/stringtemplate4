/*
 [The "BSD licence"]
 Copyright (c) 2009 Terence Parr
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.stringtemplate.compiler;

import org.antlr.runtime.*;
import org.stringtemplate.*;
import org.stringtemplate.misc.Interval;

import java.util.*;

/** A compiler for a single template */
public class Compiler implements CodeGenerator {
    /** Given a template of length n, how much code will result?
     *  For now, let's assume n/5. Later, we can test in practice.
     */
    public static final double CODE_SIZE_FACTOR = 5.0;
    public static final int SUBTEMPLATE_INITIAL_CODE_SIZE = 15;

    public static final Map<String, Interpreter.Option> supportedOptions =
        new HashMap<String, Interpreter.Option>() {
            {
                put("anchor",       Interpreter.Option.ANCHOR);
                put("format",       Interpreter.Option.FORMAT);
                put("null",         Interpreter.Option.NULL);
                put("separator",    Interpreter.Option.SEPARATOR);
                put("wrap",         Interpreter.Option.WRAP);
            }
        };

    public static final int NUM_OPTIONS = supportedOptions.size();

    public static final Map<String,String> defaultOptionValues =
        new HashMap<String,String>() {
            {
                put("anchor", "true");
                put("wrap",   "\n");
            }
        };

    public static Map<String, Short> funcs = new HashMap<String, Short>() {
        {
            put("first", Bytecode.INSTR_FIRST);
            put("last", Bytecode.INSTR_LAST);
            put("rest", Bytecode.INSTR_REST);
            put("trunc", Bytecode.INSTR_TRUNC);
            put("strip", Bytecode.INSTR_STRIP);
            put("trim", Bytecode.INSTR_TRIM);
            put("length", Bytecode.INSTR_LENGTH);
            put("strlen", Bytecode.INSTR_STRLEN);
            put("reverse", Bytecode.INSTR_REVERSE);
        }
    };

    StringTable strings = new StringTable();
    byte[] instrs;
    Interval[] sourceMap;
    int ip = 0;
    CompiledST code = new CompiledST();

    /** subdir context.  If we're compiling templates in subdir a/b/c, then
     *  /a/b/c is the path prefix to add to all ID refs; it fully qualifies them.
     *  It's like resolving x to this.x in Java for field x. 
     */
    String templatePathPrefix;

    String enclosingTemplateName;

    public static int subtemplateCount = 0; // public for testing access

    public Compiler() { this("/", "<unknown>"); }

    /** To compile a template, we need to know what directory level it's at
     *  (if any; most web apps do this but code gen apps don't) and what
     *  its name is.
     */
    public Compiler(String templatePathPrefix, String enclosingTemplateName) {
        this.templatePathPrefix = templatePathPrefix;
        this.enclosingTemplateName = enclosingTemplateName;
    }

    public CompiledST compile(String template) {
        return compile(template, '<', '>');
    }

    public CompiledST compile(String template,
                              char delimiterStartChar,
                              char delimiterStopChar)
    {
        int initialSize = Math.max(5, (int)(template.length() / CODE_SIZE_FACTOR));
        instrs = new byte[initialSize];
        sourceMap = new Interval[initialSize];
        code.template = template;

        STLexer lexer =
            new STLexer(new ANTLRStringStream(template), delimiterStartChar, delimiterStopChar);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        STParser parser = new STParser(tokens, this, enclosingTemplateName);
        try {
            parser.templateAndEOF(); // parse, trigger compile actions for single expr
        }
        catch (RecognitionException re) {
            throwSTException(tokens, parser, re);
        }

        if ( strings!=null ) code.strings = strings.toArray();
        code.codeSize = ip;
        code.instrs = new byte[code.codeSize];
        code.sourceMap = new Interval[code.codeSize];
        System.arraycopy(instrs, 0, code.instrs, 0, code.codeSize);
        System.arraycopy(sourceMap, 0, code.sourceMap, 0, code.codeSize);
        return code;
    }

    public CompiledST compile(TokenStream tokens,
                              RecognizerSharedState state)
    {
        instrs = new byte[SUBTEMPLATE_INITIAL_CODE_SIZE];
        sourceMap = new Interval[SUBTEMPLATE_INITIAL_CODE_SIZE];
        STParser parser = new STParser(tokens, state, this, enclosingTemplateName);
        try {
            parser.template(); // parse, trigger compile actions for single expr
        }
        catch (RecognitionException re) {
            throwSTException(tokens, parser, re);
        }

        if ( strings!=null ) code.strings = strings.toArray();
        code.codeSize = ip;
        code.instrs = new byte[code.codeSize];
        code.sourceMap = new Interval[code.codeSize];
        System.arraycopy(instrs, 0, code.instrs, 0, code.codeSize);
        System.arraycopy(sourceMap, 0, code.sourceMap, 0, code.codeSize);
        return code;
    }

    protected void throwSTException(TokenStream tokens, STParser parser, RecognitionException re) {
        String msg = parser.getErrorMessage(re, parser.getTokenNames());
        //String hdr = parser.getErrorHeader(re);
        if ( re.token.getType() == STLexer.EOF_TYPE ) {
            throw new STException(
                "premature EOF",
                re);
        }
        else if ( re instanceof NoViableAltException) {
            throw new STException(
                "'"+re.token.getText()+"' came as a complete surprise to me",
                re);
        }
        else if ( tokens.index() == 0 ) {
            // couldn't parse anything
            throw new STException(
                "this doesn't look like a template: \""+tokens+"\"",
                re);
        }
        else if ( tokens.LA(1) == STLexer.LDELIM ) {
            // couldn't parse anything
            throw new STException(
                "doesn't look like an expression",
                re);
        }
        else {
            throw new STException(msg, re);
        }
    }

    public int defineString(String s) {
        return strings.add(s);
    }

    // CodeGenerator interface impl.

    public void emit(short opcode) { emit(opcode,-1,-1); }

    public void emit(short opcode, int p, int q) {
        ensureCapacity(1);
        if ( !(p<0 || q<0) ) sourceMap[ip] = new Interval(p, q);
        instrs[ip++] = (byte)opcode;
    }

    public void emit(short opcode, int arg) { emit(opcode,arg,-1,-1); }

    public void emit(short opcode, int arg, int p, int q) {
        emit(opcode, p, q);
        ensureCapacity(2);
        writeShort(instrs, ip, (short)arg);
        ip += 2;
    }

    public void emit(short opcode, int arg1, int arg2, int p, int q) {
        emit(opcode, arg1, p, q);
        ensureCapacity(2);
        writeShort(instrs, ip, (short)arg2);
        ip += 2;
    }

    public void emit(short opcode, String s) { emit(opcode,s,-1,-1);}

    public void emit(short opcode, String s, int p, int q) {
        int i = defineString(s);
        emit(opcode, i, p, q);
    }

    public void write(int addr, short value) {
        writeShort(instrs, addr, value);
    }

    public int address() { return ip; }

    public String templateReferencePrefix() { return templatePathPrefix; }

    public String compileAnonTemplate(String enclosingTemplateName,
                                      TokenStream input,
                                      List<Token> argIDs,
                                      RecognizerSharedState state) {
        subtemplateCount++;
        String name = templatePathPrefix+ST.SUBTEMPLATE_PREFIX+subtemplateCount;
        TokenSource tokenSource = input.getTokenSource();
        STLexer lexer = null;
        int start=-1, stop=-1;
        if ( tokenSource instanceof STLexer ) {
            lexer = (STLexer) tokenSource;
            start = lexer.input.index();
        }
        Compiler c = new Compiler(templatePathPrefix, enclosingTemplateName);
        CompiledST sub = c.compile(input, state);
        sub.name = name;
        sub.isSubtemplate = true;
        if ( tokenSource instanceof STLexer ) {
            stop = lexer.input.index();
            // sub.template = lexer.input.substring(start,stop-2);
            //System.out.println(start+".."+stop);
            sub.embeddedStart = start;
            sub.embeddedStop = stop-1;
            sub.template = lexer.input.substring(0, lexer.input.size()-1);
        }
        if ( code.implicitlyDefinedTemplates == null ) {
            code.implicitlyDefinedTemplates = new ArrayList<CompiledST>();
        }
        code.implicitlyDefinedTemplates.add(sub);
        if ( argIDs!=null ) {
            sub.formalArguments = new LinkedHashMap<String,FormalArgument>();
            for (Token arg : argIDs) {
                String argName = arg.getText();
                sub.formalArguments.put(argName, new FormalArgument(argName));
            }
        }
        return name;
    }

    public String compileRegion(String enclosingTemplateName,
                                String regionName,
                                TokenStream input,
                                RecognizerSharedState state)
    {
        Compiler c = new Compiler(templatePathPrefix, enclosingTemplateName);
        CompiledST sub = c.compile(input, state);
        String fullName =
            templatePathPrefix+
            STGroup.getMangledRegionName(enclosingTemplateName, regionName);
        sub.isRegion = true;
        sub.regionDefType = ST.RegionType.EMBEDDED;
        sub.name = fullName;
        if ( code.implicitlyDefinedTemplates == null ) {
            code.implicitlyDefinedTemplates = new ArrayList<CompiledST>();
        }
        code.implicitlyDefinedTemplates.add(sub);
        return fullName;
    }

    public void defineBlankRegion(String fullyQualifiedName) {
        // TODO: combine with above method
        CompiledST blank = new CompiledST();
        blank.isRegion = true;
        blank.regionDefType = ST.RegionType.IMPLICIT;
        blank.name = fullyQualifiedName;
        if ( code.implicitlyDefinedTemplates == null ) {
            code.implicitlyDefinedTemplates = new ArrayList<CompiledST>();
        }
        code.implicitlyDefinedTemplates.add(blank);
    }

    protected void ensureCapacity(int n) {
        if ( (ip+n) >= instrs.length ) { // ensure room for full instruction
            byte[] c = new byte[instrs.length*2];
            System.arraycopy(instrs, 0, c, 0, instrs.length);
            instrs = c;
            Interval[] sm = new Interval[sourceMap.length*2];
            System.arraycopy(sourceMap, 0, sm, 0, sourceMap.length);
            sourceMap = sm;
        }
    }

    /** Write value at index into a byte array highest to lowest byte,
     *  left to right.
     */
    public static void writeShort(byte[] memory, int index, short value) {
        memory[index+0] = (byte)((value>>(8*1))&0xFF);
        memory[index+1] = (byte)(value&0xFF);
    }
}