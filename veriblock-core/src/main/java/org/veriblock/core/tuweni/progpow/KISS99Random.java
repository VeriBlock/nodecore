/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.veriblock.core.tuweni.progpow;

public final class KISS99Random {

    private static final int FILTER = 65535;

    public int z;
    public int w;
    public int jsr;
    public int jcong;

    KISS99Random(int z, int w, int jsr, int jcong) {
        this.z = z;
        this.w = w;
        this.jsr = jsr;
        this.jcong = jcong;
    }

    int generate() {
        z = ((z & (FILTER)) * (36969)) + (z >>> 16);
        w = ((w & (FILTER)) * 18000) + (w >>> 16);
        int mwc = (z << (16)) + (w);
        jsr = jsr ^ (jsr << (17));
        jsr = jsr ^ (jsr >>> (13));
        jsr = jsr ^ (jsr << (5));
        jcong = (jcong * 69069) + (1234567);
        return (mwc ^ (jcong)) + (jsr);
    }
}
