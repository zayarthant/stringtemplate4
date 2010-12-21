/*
 [The "BSD license"]
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
package org.stringtemplate.v4.compiler;

public class Bytecode {
    public static final int MAX_OPNDS = 2;
    public static final int OPND_SIZE_IN_BYTES = 2;

    public enum OperandType { NONE, STRING, ADDR, INT }

    public static class Instruction {
        String name; // E.g., "load_str", "new"
        OperandType[] type = new OperandType[MAX_OPNDS];
        int nopnds = 0;
        public Instruction(String name) {
            this(name,OperandType.NONE,OperandType.NONE); nopnds =0;
        }
        public Instruction(String name, OperandType a) {
            this(name,a,OperandType.NONE); nopnds =1;
        }
        public Instruction(String name, OperandType a, OperandType b) {
            this.name = name;
            type[0] = a;
            type[1] = b;
            nopnds = MAX_OPNDS;
        }
    }

    // don't use enum for efficiency; don't want CompiledST.instrs to
    // be an array of objects (Bytecode[]). We want it to be byte[].

    // INSTRUCTION BYTECODES (byte is signed; use a short to keep 0..255)
    public static final short INSTR_LOAD_STR        = 1;
    public static final short INSTR_LOAD_ATTR       = 2;
    public static final short INSTR_LOAD_LOCAL      = 3; // load stuff like it, i, i0
    public static final short INSTR_LOAD_PROP       = 4;
    public static final short INSTR_LOAD_PROP_IND   = 5;
    public static final short INSTR_SET_PASS_THRU   = 6;
    public static final short INSTR_STORE_OPTION    = 7;
    public static final short INSTR_NEW             = 8;  // create new template instance
    public static final short INSTR_NEW_IND         = 9;  // create new template instance using value on stack
    public static final short INSTR_SUPER_NEW       = 10;  // create new template instance using value on stack
    public static final short INSTR_WRITE           = 11;
	public static final short INSTR_WRITE_OPT       = 12;
    public static final short INSTR_MAP             = 13;  // <a:b()>, <a:b():c()>, <a:{...}>
    public static final short INSTR_ROT_MAP         = 14;  // <a:b(),c()>
    public static final short INSTR_ZIP_MAP         = 15;  // <names,phones:{n,p | ...}>
    public static final short INSTR_BR              = 16;
    public static final short INSTR_BRF             = 17;
    public static final short INSTR_OPTIONS         = 18;  // push options block
    public static final short INSTR_LIST            = 19;
    public static final short INSTR_ADD             = 20;
    public static final short INSTR_TOSTR           = 21;

    // Predefined functions
    public static final short INSTR_FIRST           = 22;
    public static final short INSTR_LAST            = 23;
    public static final short INSTR_REST            = 24;
    public static final short INSTR_TRUNC           = 25;
    public static final short INSTR_STRIP           = 26;
    public static final short INSTR_TRIM            = 27;
    public static final short INSTR_LENGTH          = 28;
    public static final short INSTR_STRLEN          = 29;
    public static final short INSTR_REVERSE         = 30;

	public static final short INSTR_NOT             = 31;
	public static final short INSTR_OR              = 32;
	public static final short INSTR_AND             = 33;

	public static final short INSTR_INDENT          = 34;
    public static final short INSTR_DEDENT          = 35;
    public static final short INSTR_NEWLINE         = 36;

    public static final short INSTR_NOOP            = 37; // do nothing
	public static final short INSTR_POP             = 38;
	public static final short INSTR_NULL            = 39; // push null value

    /** Used for assembly/disassembly; describes instruction set */
    public static Instruction[] instructions = new Instruction[] {
        null, // <INVALID>
        new Instruction("load_str",OperandType.STRING), // index is the opcode
        new Instruction("load_attr",OperandType.STRING),
        new Instruction("load_local",OperandType.STRING),
        new Instruction("load_prop",OperandType.STRING),
        new Instruction("load_prop_ind"),
        new Instruction("set_pass_thru"),
        new Instruction("store_option",OperandType.INT),
        new Instruction("new",OperandType.STRING,OperandType.INT),
        new Instruction("new_ind",OperandType.INT),
        new Instruction("super_new",OperandType.STRING,OperandType.INT),
        new Instruction("write"),
		new Instruction("write_opt"),
        new Instruction("map"),
        new Instruction("rot_map", OperandType.INT),
        new Instruction("zip_map", OperandType.INT),
        new Instruction("br", OperandType.ADDR),
        new Instruction("brf", OperandType.ADDR),
        new Instruction("options"),
        new Instruction("list"),
        new Instruction("add"),
        new Instruction("tostr"),
        new Instruction("first"),
        new Instruction("last"),
        new Instruction("rest"),
        new Instruction("trunc"),
        new Instruction("strip"),
        new Instruction("trim"),
        new Instruction("length"),
        new Instruction("strlen"),
		new Instruction("reverse"),
		new Instruction("not"),
		new Instruction("or"),
		new Instruction("and"),
		new Instruction("indent", OperandType.STRING),
        new Instruction("dedent"),
        new Instruction("newline"),
        new Instruction("noop"),
		new Instruction("pop"),
		new Instruction("null")
    };
}
