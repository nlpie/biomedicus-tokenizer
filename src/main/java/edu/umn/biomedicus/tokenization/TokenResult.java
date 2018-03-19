/*
 * Copyright (c) 2018 Regents of the University of Minnesota - All Rights Reserved
 * Unauthorized Copying of this file, via any medium is strictly prohibited
 * Proprietary and Confidential
 */

package edu.umn.biomedicus.tokenization;

public interface TokenResult {
  int getStartIndex();

  int getEndIndex();
}
