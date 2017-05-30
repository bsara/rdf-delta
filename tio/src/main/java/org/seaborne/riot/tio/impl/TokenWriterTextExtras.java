/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seaborne.riot.tio.impl;

import java.io.*;
import java.util.ArrayList ;
import java.util.List ;

import org.apache.jena.atlas.io.AWriter ;
import org.apache.jena.atlas.io.BufferingWriter ;
import org.apache.jena.atlas.io.IO ;
import org.apache.jena.atlas.lib.Chars ;
import org.apache.jena.graph.Node ;
import org.apache.jena.riot.RiotException ;
import org.apache.jena.riot.out.NodeFormatter ;
import org.apache.jena.riot.out.NodeFormatterTTL ;
import org.apache.jena.riot.system.PrefixMap ;
import org.apache.jena.riot.system.PrefixMapFactory ;
import org.apache.jena.riot.tokens.Token ;
import org.apache.jena.riot.tokens.TokenizerText ;
import org.apache.jena.sparql.util.FmtUtils ;
import org.seaborne.riot.tio.TokenWriter ;

abstract class TokenWriterTextExtras implements TokenWriter
{
    // Both token output stream and tuple abbreviation rules.
    // Separate out abbreviation/LastTuple support?
    
    // Will need rework to improve performance.
    // XXX TokenWriter. See tokenToString
    
    // Whether to space out the tuples a bit for readability.
    private static final boolean GAPS = true ;
    
    private final AWriter out ;
    private List<Object> lastTuple = null ;
    private boolean inTuple = false ;
    private boolean inSection = false ;
    private List<Object> thisTuple = new ArrayList<Object> () ;
    
    private final PrefixMap pmap = PrefixMapFactory.create() ;
    private final NodeFormatter fmt ;

    private String label ;
    
    /** Create a TokenOutputStreamWriter going to a OutputStream.
     *  @param out
     */
    public TokenWriterTextExtras(OutputStream out) {
        this(writer(out)) ;
    }
    
    private static Writer writer(OutputStream out) {
        
        // IO.wrap(out) -- need buffering version, 
        Writer w1 = IO.asBufferedUTF8(out);
        Writer w2 = new BufferingWriter(w1, 1024*1024);
        return w2;
    }
    
    /** Create a TokenOutputStreamWriter going to a Writer,
     * ideally one that buffers (e.g. {@linkplain BufferingWriter}).  
     * @param out
     */
    public TokenWriterTextExtras(Writer out) {
        this(null, null, out) ;
    }
    
    /** Create a TokenOutputStreamWriter going to a Writer,
     * ideally one that buffers (e.g. {@linkplain BufferingWriter}).  
     * @param out
     */
    public TokenWriterTextExtras(AWriter out) {
        this(null, null, out) ;
    }

    /** Create a TokenOutputStreamWriter going to a Writer,
     * with a given NodeFormatter policy 
     * ideally one that buffers (e.g. {@linkplain BufferingWriter}).  
     * @param out
     */
    public TokenWriterTextExtras(NodeFormatter formatter, Writer out) {
        this(null, formatter, out) ;
    }
    
    /** Create a TokenOutputStreamWriter going to a Writer,
     * with a given NodeFormatter policy 
     * ideally one that buffers (e.g. {@linkplain BufferingWriter}).  
     * @param out
     */
    public TokenWriterTextExtras(NodeFormatter formatter, AWriter out) {
        this(null, formatter, out) ;
    }

    public TokenWriterTextExtras(String label, NodeFormatter formatter, Writer out)
    { this(label, formatter, IO.wrap(out)) ; }
    
    public TokenWriterTextExtras(String label, NodeFormatter formatter, AWriter out)
    {
        if ( formatter == null )
            // XXX Must write bNodes as <_:....>
            formatter = new NodeFormatterTTL(null, pmap) ;
        formatter = new NodeFormatterBNode(formatter) ;
        
        this.fmt = formatter ;
        this.out = out ;
        this.label = label ;
    }
    
    static class NodeFormatterBNode extends NodeFormatterWrapper {
        public NodeFormatterBNode(NodeFormatter other) {
            super(other) ;
        }
        @Override
        public void format(AWriter w, Node n)
        { 
            if ( n.isBlank() )
                formatBNode(w, n) ;
            else
                super.format(w, n);
        }
        
        @Override
        public void formatBNode(AWriter w, Node n)
        { formatBNode(w, n.getBlankNodeLabel()); }

        @Override
        public void formatBNode(AWriter w, String label)
        { 
            w.print("<_:");
            w.print(label) ;
            w.print(">");
        }
    }
    
    static class NodeFormatterWrapper implements NodeFormatter {
        private final NodeFormatter fmt ;

        public NodeFormatterWrapper(NodeFormatter other)
        { this.fmt = other ; }
        
        @Override
        public void format(AWriter w, Node n)
        { fmt.format(w, n); }

        @Override
        public void formatURI(AWriter w, Node n)
        { fmt.formatURI(w, n); }

        @Override
        public void formatURI(AWriter w, String uriStr)
        { fmt.formatURI(w, uriStr); }

        @Override
        public void formatVar(AWriter w, Node n) 
        { fmt.formatVar(w, n); }

        @Override
        public void formatVar(AWriter w, String name)
        { fmt.formatVar(w, name); }

        @Override
        public void formatBNode(AWriter w, Node n)
        { fmt.formatBNode(w, n); }

        @Override
        public void formatBNode(AWriter w, String label)
        { fmt.formatBNode(w, label); }

        @Override
        public void formatLiteral(AWriter w, Node n)
        { fmt.formatLiteral(w, n); }

        @Override
        public void formatLitString(AWriter w, String lex)
        { fmt.formatLitString(w, lex); }

        @Override
        public void formatLitLang(AWriter w, String lex, String langTag)
        { fmt.formatLitLang(w, lex, langTag); }

        @Override
        public void formatLitDT(AWriter w, String lex, String datatypeURI)
        { fmt.formatLitDT(w, lex, datatypeURI); }
        
    }
    // Really want to directly insert in to the output byte buffer.
    // Optimization for later - get something working for now
    // (and it may well be fast enough anyway).
    
    public void setPrefixMapping(String prefix, String uri)
    {
        String pf = prefix ;
        if ( pf.endsWith(":") )
            pf = pf.substring(0, pf.length()-1) ;
        else
            prefix = prefix + ":" ;
        if ( pmap != null )
            pmap.add(pf, uri) ;
        startTuple() ;
        lastTuple = null ;
        write("@prefix") ;
        gap(true) ;
        write(prefix) ;
        gap(true) ;
        write("<") ;
        write(uri) ;
        write(">") ;
        gap(false) ;
        endTuple() ;
    }
    
    @Override
    public void sendToken(Token token)
    {
        remember(token) ;
        String string = tokenToString(token) ;
        write(string) ;
        gap(true) ;
    }
    
    @Override
    public void sendNode(Node node)
    {
        remember(node) ;
        fmt.format(out, node);
        gap(false) ;
    }

    @Override
    public void sendString(String string)
    {
        remember(string) ;
        fmt.formatLitString(out, string); 
        gap(false) ;
    }
    
    @Override
    public void sendWord(String string)
    {
        remember(string) ;
        write(string) ; // no escapes, no quotes
        gap(true) ;
    }
    
    private static String cntrlAsString(char cntrl)
    {
      return Character.toString((char)TokenizerText.CTRL_CHAR)+Character.toString(cntrl);
    }
   
    @Override
    public void sendControl(char controlChar)
    {
        String x = cntrlAsString(controlChar) ;
        remember(x) ;
        write(x) ; 
        gap(false) ;
    }

    @Override
    public void sendNumber(long number)
    {
        remember(number) ;
        write(Long.toString(number)) ;
        gap(true) ;
    }
    
    @Override
    public void startTuple()
    { }

    @Override
    public void endTuple() {
        if ( !inTuple )
            return ;
        out.write(Chars.CH_DOT) ;
        out.write("\n") ;

        // Compression.
        lastTuple = thisTuple ;
        thisTuple = new ArrayList<Object>(lastTuple.size()) ;
        inTuple = false ;
    }
    
    @Override
    public void close()
    {
        if ( inTuple ) {}
        IO.close(out);
    }
    
    @Override
    public void flush() { 
        out.flush() ; 
    }
    
    // --------
    
    private String tokenToString(Token token)
    {
        switch ( token.getType() )
        {
            // superclass case NODE:
            case IRI:
                return "<"+token.getImage()+">" ;
            case PREFIXED_NAME: 
                notImplemented(token) ;
                return null ;
            case BNODE:
                return "_:"+token.getImage() ;
            //BOOLEAN,
            case STRING:
                // XXX
                //return "'"+NodeFmtLib.esc(token.getImage())+"'" ;
                return "\""+FmtUtils.stringEsc(token.getImage())+"\"" ;
            case LITERAL_LANG:
                return "\""+FmtUtils.stringEsc(token.getImage())+"\"@"+token.getImage2() ;
            case LITERAL_DT:
                return "\""+FmtUtils.stringEsc(token.getImage())+"\"^^"+tokenToString(token.getSubToken2()) ;
            case INTEGER:
            case DECIMAL:
            case DOUBLE:
                return token.getImage() ;
                
            // Not RDF
            case KEYWORD:
                return token.getImage() ;
            case DOT:
                return Chars.S_DOT ;
            case VAR:
                return "?"+token.getImage() ; 
            case COMMA:
                return Chars.S_COMMA ;
            case SEMICOLON:
                return Chars.S_SEMICOLON ;
            case COLON: 
                return Chars.S_COLON ;
            case LT:
                return Chars.S_LT ;
            case GT:
                return Chars.S_GT ;
            case LE:
                return Chars.S_LE ;
            case GE:
                return Chars.S_GE ;
            case UNDERSCORE:
                return Chars.S_UNDERSCORE ;
            case LBRACE:
                return Chars.S_LBRACE ;
            case RBRACE:
                return Chars.S_RBRACE ;
            case LPAREN:
                return Chars.S_LPAREN ;
            case RPAREN:
                return Chars.S_RPAREN ;
            case LBRACKET:
                return Chars.S_LBRACKET ;
            case RBRACKET:
                return Chars.S_RBRACKET ;
            case PLUS:
                return Chars.S_PLUS ;
            case MINUS:
                return Chars.S_MINUS ;
            case STAR:
                return Chars.S_STAR ;
            case SLASH:
                return Chars.S_SLASH ;
            case RSLASH:
                return Chars.S_RSLASH ;
            case HEX:
                return "0x"+token.getImage() ;
            // Syntax
            // COLON is only visible if prefix names are not being processed.
            case DIRECTIVE:
                return "@"+token.getImage() ;
            case VBAR :
                return Chars.S_VBAR;
            case AMPHERSAND :
                return Chars.S_AMPHERSAND;
            case EQUALS :
                return Chars.S_EQUALS;
            default:
                notImplemented(token) ;
                return null ;
                
            //case EOF:
                
//            case EQUIVALENT :
//                return "==";
//            case LOGICAL_AND :
//                return "&&";
//            case LOGICAL_OR :
//                return "||";
//            case NL :
//                break;
//            case NODE :
//                break;
//            case WS :
//                break;
        }
    }

    private void remember(Object obj)
    { thisTuple.add(obj) ; }
    
    // A gap is always necessary for items that are not endLog-limited.
    // For example, numbers adjacent to numbers must have a gap but
    // quoted string then quoted string does not require a gap.  
    private void gap(boolean required)
    {
        if ( required || GAPS ) write(" ") ;
    }

    // Beware of multiple stringing.
    private void write(String string)
    { 
        //System.out.println("Send: "+string) ;
        inTuple = true ; 
        out.write(string) ; 
    }
    
    private static void exception(IOException ex)
    { throw new RiotException(ex) ; }

    private void notImplemented(Token token)
    { throw new RiotException("Unencodable token: "+token) ; }
}
