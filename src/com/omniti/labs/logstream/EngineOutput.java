/* Copyright 2012-2014 Circonus, Inc. All rights reserved. */
/* This program released under the terms of the GPLv2 */

package com.omniti.labs.logstream;

import java.util.UUID;

public class EngineOutput {
  public UUID uuid;
  public String object[];

  public EngineOutput(UUID uuid, String object[]) {
    this.uuid = uuid;
    this.object = object;
  }
}
