/*
 * Copyright (c) 2018 Regents of the University of Minnesota - All Rights Reserved
 * Unauthorized Copying of this file, via any medium is strictly prohibited
 * Proprietary and Confidential
 */

package edu.umn.biomedicus.tokenization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class Tokenizer {

  private static final List<String> UNITS = loadUnitsList();
  private static final Pattern MID_BREAKS = Pattern.compile(
      // math and connector symbols and punctuation (except . , ' ’ - # $), dash following anything or before a letter
      "[\\p{Sm}\\p{Sk}\\p{P}&&[^.,'’\\-#$]]|(?<=[^\\p{Z}])-|-(?=[\\p{L}])"
          // comma between two characters at least one of which is not a number
          + "|(?<=[^\\p{N}]),(?=[^\\p{N}])|(?<=[^\\p{N}]),(?=[\\p{N}])|(?<=[\\p{N}]),(?=[^\\p{N}])"
  );
  private static final Pattern START_BREAKS = Pattern.compile("^[',’]");
  private static final Pattern X = Pattern.compile("[xX]");
  private static final Pattern END_BREAKS = Pattern.compile(
      "(?<=('[SsDdMm]|n't|N'T|'ll|'LL|'ve|'VE|'re|'RE|'|’|,))$"
  );
  private static final Pattern NUMBER_WORD = Pattern
      .compile("[-]?[0-9.xX]*[0-9]++(?<suffix>[\\p{Alpha}]++)$");
  private static final Pattern NUMBER_X = Pattern
      .compile(".*?[0-9.]*[0-9]++([xX][0-9.]*[0-9]++)+$");
  private final StringBuilder word = new StringBuilder();
  private int startIndex = -1;
  private List<TokenResult> results;

  public static List<TokenResult> allTokens(String string) {
    List<TokenResult> results = new ArrayList<>();

    Tokenizer tokenizer = new Tokenizer();

    for (int i = 0; i < string.length(); i++) {
      results.addAll(tokenizer.advance(string.charAt(i), i));
    }

    results.addAll(tokenizer.finish());

    return results;
  }

  public static Iterable<TokenResult> tokenize(String string) {
    return () -> new Iterator<TokenResult>() {
      int index = 0;
      Iterator<TokenResult> subIt = null;
      TokenResult next = null;
      Tokenizer tokenizer = new Tokenizer();

      {
        advance();
      }

      void advance() {
        if (subIt != null && subIt.hasNext()) {
          next = subIt.next();
          return;
        }
        subIt = null;
        if (index > string.length()) {
          next = null;
          return;
        } else if (index == string.length()) {
          subIt = tokenizer.finish().iterator();
          index++;
        } else {
          List<TokenResult> results = tokenizer.advance(string.charAt(index), index++);
          subIt = results.size() > 0 ? results.iterator() : null;
        }
        advance();
      }

      @Override
      public boolean hasNext() {
        return next != null;
      }

      @Override
      public TokenResult next() {
        TokenResult temp = next;
        advance();
        return temp;
      }
    };
  }

  private static List<String> loadUnitsList() {
    String unitListPath = System.getProperty("biomedicus.tokenizer.unitsListPath");
    if (unitListPath != null) {
      try {
        return Files.readAllLines(Paths.get(unitListPath));
      } catch (IOException ignored) {

      }
    }
    InputStream is = Tokenizer.class.getResourceAsStream("unitsList.txt");
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    return reader.lines().collect(Collectors.toList());
  }

  @Nonnull
  public List<TokenResult> advance(char ch, int index) {
    int type = Character.getType(ch);
    if (type == Character.SPACE_SEPARATOR || type == Character.LINE_SEPARATOR
        || type == Character.PARAGRAPH_SEPARATOR || type == Character.FORMAT
        || ch == '\n' || ch == '\t' || ch == '\r') {
      return breakWord();
    } else {
      if (word.length() == 0) {
        startIndex = index;
      }
      word.append(ch);
    }
    return Collections.emptyList();
  }

  @Nonnull
  public List<TokenResult> finish() {
    return breakWord();
  }

  private List<TokenResult> breakWord() {
    if (word.length() == 0) {
      return Collections.emptyList();
    }

    results = new ArrayList<>();

    Matcher midMatcher = MID_BREAKS.matcher(word);
    int start = 0;
    while (midMatcher.find()) {
      if (start != midMatcher.start()) {
        breakStarts(start, midMatcher.start());
      }
      if (midMatcher.start() != midMatcher.end()) {
        addResult(midMatcher.start(), midMatcher.end());
      }
      start = midMatcher.end();
    }
    if (start != word.length()) {
      breakStarts(start, word.length());
    }

    startIndex = -1;
    word.setLength(0);
    return results;
  }

  private void breakStarts(int start, int end) {
    while (true) {
      Matcher startMatcher = START_BREAKS.matcher(word.subSequence(start, end));
      if (startMatcher.find() && startMatcher.end() != 0) {
        addResult(start, start + startMatcher.end());
        start = start + startMatcher.end();
      } else {
        if (start != end) {
          breakEnds(start, end);
        }
        break;
      }
    }
  }

  private void breakEnds(int start, int end) {
    Matcher matcher = END_BREAKS.matcher(word.subSequence(start, end));
    if (matcher.find()) {
      if (matcher.start(1) != 0) {
        breakEnds(start, start + matcher.start(1));
      }
      if (matcher.start(1) != matcher.end(1)) {
        addResult(start + matcher.start(1), start + matcher.end(1));
      }
    } else {
      breakUnitsOfTheEndsOfNumbers(start, end);
    }
  }

  private void breakUnitsOfTheEndsOfNumbers(int start, int end) {
    CharSequence tokenText = word.subSequence(start, end);
    Matcher matcher = NUMBER_WORD.matcher(tokenText);
    if (matcher.matches()) {
      String suffix = matcher.group("suffix");
      if (suffix != null && UNITS.contains(suffix.toLowerCase())) {
        splitNumbersByX(start, end - suffix.length());
        addResult(end - suffix.length(), end);
        return;
      }
    }
    splitNumbersByX(start, end);
  }

  private void splitNumbersByX(int start, int end) {
    CharSequence tokenText = word.subSequence(start, end);
    Matcher matcher = NUMBER_X.matcher(tokenText);
    if (matcher.matches()) {
      int prev = start;
      Matcher xMatcher = X.matcher(tokenText);
      while (xMatcher.find()) {
        addResult(prev, start + xMatcher.start());
        prev = start + xMatcher.end();
        addResult(start + xMatcher.start(), prev);
      }
      if (prev != end) {
        addResult(prev, end);
      }
    } else {
      addResult(start, end);
    }
  }

  private void addResult(int start, int end) {
    results.add(new StandardTokenResult(startIndex + start, startIndex + end));
  }

  static class StandardTokenResult implements TokenResult {

    private final int startIndex;
    private final int endIndex;

    StandardTokenResult(int startIndex, int endIndex) {
      this.startIndex = startIndex;
      this.endIndex = endIndex;
    }

    public int getStartIndex() {
      return startIndex;
    }

    public int getEndIndex() {
      return endIndex;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      StandardTokenResult result = (StandardTokenResult) o;
      return startIndex == result.startIndex &&
          endIndex == result.endIndex;
    }

    @Override
    public int hashCode() {

      return Objects.hash(startIndex, endIndex);
    }
  }
}
