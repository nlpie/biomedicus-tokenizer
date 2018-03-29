/*
 * Copyright (c) 2018 Regents of the University of Minnesota - All Rights Reserved
 * Unauthorized Copying of this file, via any medium is strictly prohibited
 * Proprietary and Confidential
 */

package edu.umn.biomedicus.tokenization;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import edu.umn.biomedicus.tokenization.Tokenizer.StandardTokenResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.testng.annotations.Test;

public class TokenizerTest {

  public static final String SENTENCE = "This test's logic will confirm that the tokenizer (P.T.B.-like) is well-behaved.";

  @Test
  public void testWords() {
    assertEquals(Tokenizer.allTokens(SENTENCE), Arrays.asList(
        new StandardTokenResult(0, 4),
        new StandardTokenResult(5, 9),
        new StandardTokenResult(9, 11),
        new StandardTokenResult(12, 17),
        new StandardTokenResult(18, 22),
        new StandardTokenResult(23, 30),
        new StandardTokenResult(31, 35),
        new StandardTokenResult(36, 39),
        new StandardTokenResult(40, 49),
        new StandardTokenResult(50, 51),
        new StandardTokenResult(51, 57),
        new StandardTokenResult(57, 58),
        new StandardTokenResult(58, 62),
        new StandardTokenResult(62, 63),
        new StandardTokenResult(64, 66),
        new StandardTokenResult(67, 71),
        new StandardTokenResult(71, 72),
        new StandardTokenResult(72, 80)
    ));
  }

  @Test
  public void testIterator() throws Exception {
    Iterator<TokenResult> iterator = Tokenizer.tokenize(SENTENCE).iterator();
    assertEquals(iterator.next(), new StandardTokenResult(0, 4));
    assertEquals(iterator.next(), new StandardTokenResult(5, 9));
    assertEquals(iterator.next(), new StandardTokenResult(9, 11));
    assertEquals(iterator.next(), new StandardTokenResult(12, 17));
    assertEquals(iterator.next(), new StandardTokenResult(18, 22));
    assertEquals(iterator.next(), new StandardTokenResult(23, 30));
    assertEquals(iterator.next(), new StandardTokenResult(31, 35));
    assertEquals(iterator.next(), new StandardTokenResult(36, 39));
    assertEquals(iterator.next(), new StandardTokenResult(40, 49));
    assertEquals(iterator.next(), new StandardTokenResult(50, 51));
    assertEquals(iterator.next(), new StandardTokenResult(51, 57));
    assertEquals(iterator.next(), new StandardTokenResult(57, 58));
    assertEquals(iterator.next(), new StandardTokenResult(58, 62));
    assertEquals(iterator.next(), new StandardTokenResult(62, 63));
    assertEquals(iterator.next(), new StandardTokenResult(64, 66));
    assertEquals(iterator.next(), new StandardTokenResult(67, 71));
    assertEquals(iterator.next(), new StandardTokenResult(71, 72));
    assertEquals(iterator.next(), new StandardTokenResult(72, 80));
    assertFalse(iterator.hasNext());
  }

  @Test
  public void testDoesSplitZWSP() {
    List<TokenResult> results = Tokenizer.allTokens(
        "This sentence has some zero-width spaces.\u200b\u200b"
    );

    TokenResult tokenCandidate = results.get(results.size() - 1);
    assertEquals(tokenCandidate.getEndIndex(), 41);
  }

  @Test
  public void testWordsEmptySentence() {
    List<TokenResult> list = Tokenizer.allTokens("");

    assertEquals(list.size(), 0);
  }

  @Test
  public void testWordsWhitespaceSentence() {
    List<TokenResult> list = Tokenizer.allTokens("\n \t   ");

    assertEquals(list.size(), 0);
  }

  @Test
  public void testDoNotSplitCommaNumbers() {
    List<TokenResult> spanList = Tokenizer.allTokens("42,000,000");

    assertEquals(spanList.size(), 1);
    assertEquals(spanList.get(0).getStartIndex(), 0);
    assertEquals(spanList.get(0).getEndIndex(), 10);
  }

  @Test
  public void testSplitTrailingComma() {
    List<TokenResult> list = Tokenizer.allTokens("first,");

    assertEquals(list.size(), 2);
    assertEquals(list.get(0).getStartIndex(), 0);
    assertEquals(list.get(0).getEndIndex(), 5);
  }

  @Test
  public void testSplitPercent() {
    List<TokenResult> spans = Tokenizer.allTokens("42%");

    assertEquals(spans.size(), 2);
    assertEquals(spans.get(0), new StandardTokenResult(0, 2));
    assertEquals(spans.get(1), new StandardTokenResult(2, 3));
  }

  @Test
  public void testParenSplitMid() {
    List<TokenResult> spans = Tokenizer.allTokens("abc(asf");

    assertEquals(spans.size(), 3);
    assertEquals(spans.get(0), new StandardTokenResult(0, 3));
    assertEquals(spans.get(1), new StandardTokenResult(3, 4));
    assertEquals(spans.get(2), new StandardTokenResult(4, 7));
  }

  @Test
  public void testSplitUnitsOffTheEnd() {
    List<TokenResult> list = Tokenizer.allTokens("2.5cm");

    assertEquals(list.size(), 2);
    assertEquals(list.get(0), new StandardTokenResult(0, 3));
    assertEquals(list.get(1), new StandardTokenResult(3, 5));
  }

  @Test
  public void testSingleQuote() {
    List<TokenResult> list = Tokenizer.allTokens("'xyz");

    assertEquals(list.size(), 2);
    assertEquals(list.get(0), new StandardTokenResult(0, 1));
    assertEquals(list.get(1), new StandardTokenResult(1, 4));
  }

  @Test
  public void testSplitNumbersSeparatedByX() throws Exception {
    List<TokenResult> list = Tokenizer.allTokens("2x3x4");

    assertEquals(list.size(), 5);
    assertEquals(list.get(0), new StandardTokenResult(0, 1));
    assertEquals(list.get(1), new StandardTokenResult(1, 2));
    assertEquals(list.get(2), new StandardTokenResult(2, 3));
    assertEquals(list.get(3), new StandardTokenResult(3, 4));
    assertEquals(list.get(4), new StandardTokenResult(4, 5));
  }

  @Test
  public void testDontSplitEndOfSentenceAM() throws Exception {
    List<TokenResult> list = Tokenizer.allTokens("a.m.");

    assertEquals(list, Collections.singletonList(new StandardTokenResult(0, 4)));
  }

  @Test
  public void testDontSplitCommaAfterParen() throws Exception {
    List<TokenResult> list = Tokenizer.allTokens("(something), something");

    assertEquals(list, Arrays.asList(
        new StandardTokenResult(0, 1),
        new StandardTokenResult(1, 10),
        new StandardTokenResult(10, 11),
        new StandardTokenResult(11, 12),
        new StandardTokenResult(13, 22)
    ));
  }
}
