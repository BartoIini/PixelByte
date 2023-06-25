package com.bartolini.pixelbyte.modules.terminal.shell.parser;

import java.util.Collection;

/**
 * A <i>Parser</i> is used to parse an input string into a {@linkplain Collection} of
 * {@linkplain ParseResult ParseResults}.
 *
 * @author Bartolini
 * @version 1.0
 */
public interface Parser {
    Collection<ParseResult> parseInput(String input);
}