/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Copyright (c) 2012, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package java.time.chrono;

import java.time.DateTimeException;

/**
 * ISO日历系统中的一个时代。
 * <p>
 * ISO-8601标准未定义时代。
 * 因此，已创建了两个时代的定义 - '当前时代' (CE) 用于 0001-01-01 (ISO) 及之后的年份，而 '当前时代之前' (BCE) 用于之前的年份。
 *
 * <table summary="ISO years and eras" cellpadding="2" cellspacing="3" border="0" >
 * <thead>
 * <tr class="tableSubHeadingColor">
 * <th class="colFirst" align="left">year-of-era</th>
 * <th class="colFirst" align="left">era</th>
 * <th class="colLast" align="left">proleptic-year</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr class="rowColor">
 * <td>2</td><td>CE</td><td>2</td>
 * </tr>
 * <tr class="altColor">
 * <td>1</td><td>CE</td><td>1</td>
 * </tr>
 * <tr class="rowColor">
 * <td>1</td><td>BCE</td><td>0</td>
 * </tr>
 * <tr class="altColor">
 * <td>2</td><td>BCE</td><td>-1</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * <b>不要使用 {@code ordinal()} 来获取 {@code IsoEra} 的数值表示。
 * 请改用 {@code getValue()}。</b>
 *
 * @implSpec
 * 这是一个不可变且线程安全的枚举。
 *
 * @since 1.8
 */
public enum IsoEra implements Era {

    /**
     * 当前时代之前的单一实例，'Before Current Era'，其数值为 0。
     */
    BCE,
    /**
     * 当前时代的单一实例，'Current Era'，其数值为 1。
     */
    CE;

    //-----------------------------------------------------------------------
    /**
     * 从 {@code int} 值中获取 {@code IsoEra} 的实例。
     * <p>
     * {@code IsoEra} 是一个表示 ISO 时代的 BCE/CE 的枚举。
     * 此工厂方法允许从 {@code int} 值中获取枚举实例。
     *
     * @param isoEra  要表示的 BCE/CE 值，从 0 (BCE) 到 1 (CE)
     * @return 时代单例，不为空
     * @throws DateTimeException 如果值无效
     */
    public static IsoEra of(int isoEra) {
        switch (isoEra) {
            case 0:
                return BCE;
            case 1:
                return CE;
            default:
                throw new DateTimeException("Invalid era: " + isoEra);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * 获取时代的数值 {@code int} 值。
     * <p>
     * 时代 BCE 的值为 0，而时代 CE 的值为 1。
     *
     * @return 时代值，从 0 (BCE) 到 1 (CE)
     */
    @Override
    public int getValue() {
        return ordinal();
    }

}
