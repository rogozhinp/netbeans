/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.netbeans.modules.languages.toml;

import org.antlr.v4.runtime.misc.IntegerStack;
import org.netbeans.api.lexer.Token;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;
import org.netbeans.spi.lexer.TokenFactory;

import static org.tomlj.internal.TomlLexer.*;
import static org.netbeans.modules.languages.toml.TomlTokenId.*;

/**
 *
 * @author lkishalmi
 */
public final class TomlLexer implements Lexer<TomlTokenId> {

    private final TokenFactory<TomlTokenId> tokenFactory;
    private final org.tomlj.internal.TomlLexer lexer;
    private final LexerInputCharStream input;

    public TomlLexer(LexerRestartInfo<TomlTokenId> info) {
        this.tokenFactory = info.tokenFactory();
        this.input = new LexerInputCharStream(info.input());
        try {
            this.lexer = new org.tomlj.internal.TomlLexer(input);
            if (info.state() != null) {
                ((LexerState) info.state()).restore(lexer);
            }
            input.markToken();
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    private org.antlr.v4.runtime.Token preFetchedToken = null;

    @Override
    public org.netbeans.api.lexer.Token<TomlTokenId> nextToken() {
        org.antlr.v4.runtime.Token nextToken;
        if (preFetchedToken != null) {
            nextToken = preFetchedToken;
            lexer.getInputStream().seek(preFetchedToken.getStopIndex() + 1);
            preFetchedToken = null;
        } else {
            nextToken = lexer.nextToken();
        }
        int tokenType = nextToken.getType();
        switch (tokenType) {
            case EOF:
                return null;

            case StringChar:
                return collate(StringChar, STRING);
                
            case TripleQuotationMark:
            case TripleApostrophe:
            case QuotationMark:
            case Apostrophe:
                return token(STRING_QUOTE);

            case Comma:
            case ArrayStart:
            case ArrayEnd:
            case InlineTableStart:
            case InlineTableEnd:

            case Dot:
                return token(DOT);

            case Equals:
                return token(EQUALS);

            case TableKeyStart:
            case TableKeyEnd:
            case ArrayTableKeyStart:
            case ArrayTableKeyEnd:
                return token(TABLE_MARK);
            case UnquotedKey:
                return token(KEY);
            case Comment:
                return token(COMMENT);
            case WS:
            case NewLine:
                return token(TomlTokenId.WHITESPACE);
            case Error:
                return token(ERROR);

            case DecimalInteger:
            case HexInteger:
            case OctalInteger:
            case BinaryInteger:
            case FloatingPoint:
            case FloatingPointInf:
            case FloatingPointNaN:
                return token(NUMBER);

            case TrueBoolean:
            case FalseBoolean:
                return token(BOOLEAN);

            case EscapeSequence:
                return token(ESCAPE_SEQUENCE);

            case Dash:
            case Plus:
            case Colon:
            case Z:
            case TimeDelimiter:
            case DateDigits:
                return token(DATE);
            default:
                return token(ERROR);
        }
    }

    protected org.netbeans.api.lexer.Token<TomlTokenId> collate(int tokenType, TomlTokenId tokenId) {
        preFetchedToken = lexer.nextToken();
        while (preFetchedToken.getType() == tokenType) {
            preFetchedToken = lexer.nextToken();
        }
        lexer.getInputStream().seek(preFetchedToken.getStartIndex());
        return token(tokenId);
    }
    
    @Override
    public Object state() {
        return new LexerState(lexer);
    }

    @Override
    public void release() {
    }

    private Token<TomlTokenId> token(TomlTokenId id) {
        input.markToken();
        return tokenFactory.createToken(id);
    }

    private static class LexerState {
        final int state;
        final int mode;
        final IntegerStack modes;

        final int arrayDepth;
        final IntegerStack arrayDepthStack;

        LexerState(org.tomlj.internal.TomlLexer lexer) {
            this.state= lexer.getState();

            this.mode = lexer._mode;
            this.modes = new IntegerStack(lexer._modeStack);
            this.arrayDepth = lexer.arrayDepth;
            this.arrayDepthStack = new IntegerStack(lexer.arrayDepthStack);
        }

        public void restore(org.tomlj.internal.TomlLexer lexer) {
            lexer.setState(state);
            lexer._modeStack.addAll(modes);
            lexer._mode = mode;

            lexer.arrayDepth = arrayDepth;
            lexer.arrayDepthStack.addAll(arrayDepthStack);
        }

        @Override
        public String toString() {
            return String.valueOf(state);
        }

    }
}
