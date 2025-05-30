
/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (c) 2007-2012, Stephen Colebourne & Michael Nascimento Santos
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
package java.time;

import static java.time.LocalTime.NANOS_PER_SECOND;
import static java.time.LocalTime.SECONDS_PER_DAY;
import static java.time.LocalTime.SECONDS_PER_HOUR;
import static java.time.LocalTime.SECONDS_PER_MINUTE;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 一个基于时间的时间量，例如 '34.5 秒'。
 * <p>
 * 该类以秒和纳秒的形式建模时间量。
 * 它可以使用其他基于持续时间的单位访问，例如分钟和小时。
 * 此外，可以使用 {@link ChronoUnit#DAYS DAYS} 单位，并被视为恰好等于 24 小时，从而忽略夏令时效应。
 * 有关基于日期的等效类，请参见 {@link Period}。
 * <p>
 * 物理持续时间可能是无限长的。
 * 为了实际应用，持续时间的存储受到类似于 {@link Instant} 的约束。
 * 持续时间使用纳秒分辨率，最大值为可以存储在 {@code long} 中的秒数。这超过了当前估计的宇宙年龄。
 * <p>
 * 持续时间的范围需要存储一个大于 {@code long} 的数字。
 * 为了实现这一点，该类存储一个表示秒的 {@code long} 和一个表示纳秒的 {@code int}，后者始终在 0 到 999,999,999 之间。
 * 该模型是一个有方向的持续时间，意味着持续时间可以是负数。
 * <p>
 * 持续时间以“秒”为单位测量，但这些秒不一定与基于原子钟的科学“SI 秒”定义相同。
 * 这种差异仅影响在闰秒附近测量的持续时间，不应影响大多数应用程序。
 * 有关秒和时间尺度的讨论，请参见 {@link Instant}。
 *
 * <p>
 * 这是一个 <a href="{@docRoot}/java/lang/doc-files/ValueBased.html">基于值的</a>
 * 类；对 {@code Duration} 实例使用身份敏感操作（包括引用相等性
 * ({@code ==})、身份哈希码或同步）可能会产生不可预测的结果，应避免使用。
 * 应使用 {@code equals} 方法进行比较。
 *
 * @implSpec
 * 该类是不可变的且线程安全的。
 *
 * @since 1.8
 */
public final class Duration
        implements TemporalAmount, Comparable<Duration>, Serializable {

    /**
     * 表示零的常量。
     */
    public static final Duration ZERO = new Duration(0, 0);
    /**
     * 序列化版本。
     */
    private static final long serialVersionUID = 3078945930695997490L;
    /**
     * 每秒纳秒数的常量。
     */
    private static final BigInteger BI_NANOS_PER_SECOND = BigInteger.valueOf(NANOS_PER_SECOND);
    /**
     * 用于解析的模式。
     */
    private static final Pattern PATTERN =
            Pattern.compile("([-+]?)P(?:([-+]?[0-9]+)D)?" +
                    "(T(?:([-+]?[0-9]+)H)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)(?:[.,]([0-9]{0,9}))?S)?)?",
                    Pattern.CASE_INSENSITIVE);

    /**
     * 持续时间中的秒数。
     */
    private final long seconds;
    /**
     * 持续时间中的纳秒数，表示为秒数的一部分。这始终是正数，且不超过 999,999,999。
     */
    private final int nanos;

    //-----------------------------------------------------------------------
    /**
     * 获取表示标准 24 小时天数的 {@code Duration}。
     * <p>
     * 秒数是基于标准的一天定义计算的，其中每天是 86400 秒，即 24 小时。
     * 纳秒字段设置为零。
     *
     * @param days  天数，正数或负数
     * @return 一个 {@code Duration}，不为空
     * @throws ArithmeticException 如果输入的天数超出 {@code Duration} 的容量
     */
    public static Duration ofDays(long days) {
        return create(Math.multiplyExact(days, SECONDS_PER_DAY), 0);
    }

    /**
     * 获取表示标准小时数的 {@code Duration}。
     * <p>
     * 秒数是基于标准的一小时定义计算的，其中每小时是 3600 秒。
     * 纳秒字段设置为零。
     *
     * @param hours  小时数，正数或负数
     * @return 一个 {@code Duration}，不为空
     * @throws ArithmeticException 如果输入的小时数超出 {@code Duration} 的容量
     */
    public static Duration ofHours(long hours) {
        return create(Math.multiplyExact(hours, SECONDS_PER_HOUR), 0);
    }

    /**
     * 获取表示标准分钟数的 {@code Duration}。
     * <p>
     * 秒数是基于标准的一分钟定义计算的，其中每分钟是 60 秒。
     * 纳秒字段设置为零。
     *
     * @param minutes  分钟数，正数或负数
     * @return 一个 {@code Duration}，不为空
     * @throws ArithmeticException 如果输入的分钟数超出 {@code Duration} 的容量
     */
    public static Duration ofMinutes(long minutes) {
        return create(Math.multiplyExact(minutes, SECONDS_PER_MINUTE), 0);
    }

    //-----------------------------------------------------------------------
    /**
     * 获取表示秒数的 {@code Duration}。
     * <p>
     * 纳秒字段设置为零。
     *
     * @param seconds  秒数，正数或负数
     * @return 一个 {@code Duration}，不为空
     */
    public static Duration ofSeconds(long seconds) {
        return create(seconds, 0);
    }

    /**
     * 获取表示秒数和纳秒调整的 {@code Duration}。
     * <p>
     * 该方法允许传递任意数量的纳秒。
     * 工厂将调整秒数和纳秒数，以确保存储的纳秒在 0 到 999,999,999 之间。
     * 例如，以下将导致完全相同的持续时间：
     * <pre>
     *  Duration.ofSeconds(3, 1);
     *  Duration.ofSeconds(4, -999_999_999);
     *  Duration.ofSeconds(2, 1000_000_001);
     * </pre>
     *
     * @param seconds  秒数，正数或负数
     * @param nanoAdjustment  秒数的纳秒调整，正数或负数
     * @return 一个 {@code Duration}，不为空
     * @throws ArithmeticException 如果调整导致秒数超出 {@code Duration} 的容量
     */
    public static Duration ofSeconds(long seconds, long nanoAdjustment) {
        long secs = Math.addExact(seconds, Math.floorDiv(nanoAdjustment, NANOS_PER_SECOND));
        int nos = (int) Math.floorMod(nanoAdjustment, NANOS_PER_SECOND);
        return create(secs, nos);
    }

    //-----------------------------------------------------------------------
    /**
     * 获取表示毫秒数的 {@code Duration}。
     * <p>
     * 秒数和纳秒数是从指定的毫秒数中提取的。
     *
     * @param millis  毫秒数，正数或负数
     * @return 一个 {@code Duration}，不为空
     */
    public static Duration ofMillis(long millis) {
        long secs = millis / 1000;
        int mos = (int) (millis % 1000);
        if (mos < 0) {
            mos += 1000;
            secs--;
        }
        return create(secs, mos * 1000_000);
    }

    //-----------------------------------------------------------------------
    /**
     * 获取表示纳秒数的 {@code Duration}。
     * <p>
     * 秒数和纳秒数是从指定的纳秒数中提取的。
     *
     * @param nanos  纳秒数，正数或负数
     * @return 一个 {@code Duration}，不为空
     */
    public static Duration ofNanos(long nanos) {
        long secs = nanos / NANOS_PER_SECOND;
        int nos = (int) (nanos % NANOS_PER_SECOND);
        if (nos < 0) {
            nos += NANOS_PER_SECOND;
            secs--;
        }
        return create(secs, nos);
    }

    //-----------------------------------------------------------------------
    /**
     * 获取表示指定单位数量的 {@code Duration}。
     * <p>
     * 参数表示类似 '6 小时' 这样的短语的两部分。例如：
     * <pre>
     *  Duration.of(3, SECONDS);
     *  Duration.of(465, HOURS);
     * </pre>
     * 该方法只接受一部分单位。
     * 单位必须具有 {@linkplain TemporalUnit#isDurationEstimated() 精确的持续时间} 或
     * 是 {@link ChronoUnit#DAYS}，后者被视为 24 小时。其他单位将抛出异常。
     *
     * @param amount  持续时间的数量，以单位为单位，正数或负数
     * @param unit  持续时间的单位，必须具有精确的持续时间，不为空
     * @return 一个 {@code Duration}，不为空
     * @throws DateTimeException 如果周期单位具有估计的持续时间
     * @throws ArithmeticException 如果发生数值溢出
     */
    public static Duration of(long amount, TemporalUnit unit) {
        return ZERO.plus(amount, unit);
    }

    //-----------------------------------------------------------------------
    /**
     * 从时间量中获取 {@code Duration} 的实例。
     * <p>
     * 这是基于指定量获取持续时间的方法。
     * {@code TemporalAmount} 表示一个时间量，可以是基于日期的或基于时间的，该工厂从中提取持续时间。
     * <p>
     * 转换会遍历量中的单位，并使用单位的 {@linkplain TemporalUnit#getDuration() 持续时间} 计算总 {@code Duration}。
     * 该方法只接受一部分单位。单位必须具有 {@linkplain TemporalUnit#isDurationEstimated() 精确的持续时间}
     * 或是 {@link ChronoUnit#DAYS}，后者被视为 24 小时。如果发现其他单位，则抛出异常。
     *
     * @param amount  要转换的时间量，不为空
     * @return 等效的持续时间，不为空
     * @throws DateTimeException 如果无法转换为 {@code Duration}
     * @throws ArithmeticException 如果发生数值溢出
     */
    public static Duration from(TemporalAmount amount) {
        Objects.requireNonNull(amount, "amount");
        Duration duration = ZERO;
        for (TemporalUnit unit : amount.getUnits()) {
            duration = duration.plus(amount.get(unit), unit);
        }
        return duration;
    }

    //-----------------------------------------------------------------------
    /**
     * 从文本字符串（如 {@code PnDTnHnMn.nS}）中获取 {@code Duration}。
     * <p>
     * 这将解析持续时间的文本表示，包括 {@code toString()} 生成的字符串。
     * 接受的格式基于 ISO-8601 持续时间格式 {@code PnDTnHnMn.nS}，其中天数被视为恰好 24 小时。
     * <p>
     * 字符串以可选的符号开头，用 ASCII 负号或正号表示。如果为负，则整个周期为负。
     * 接下来是大写或小写的 ASCII 字母 "P"。
     * 然后是四个部分，每个部分由一个数字和一个后缀组成。
     * 部分的后缀是 ASCII 字母 "D"、"H"、"M" 和 "S"，分别表示天数、小时数、分钟数和秒数，接受大写或小写。
     * 后缀必须按顺序出现。ASCII 字母 "T" 必须出现在小时、分钟或秒部分的第一次出现之前。
     * 至少必须有一个部分，如果 "T" 出现，则 "T" 之后必须至少有一个部分。
     * 每个部分的数字部分必须由一个或多个 ASCII 数字组成。
     * 数字可以由 ASCII 负号或正号前缀。
     * 天数、小时数和分钟数必须解析为 {@code long}。
     * 秒数必须解析为带有可选小数部分的 {@code long}。
     * 小数点可以是点或逗号。
     * 小数部分可以有 0 到 9 位。
     * <p>
     * 前导正负号和其他单位的负值不是 ISO-8601 标准的一部分。
     * <p>
     * 示例：
     * <pre>
     *    "PT20.345S" -- 解析为 "20.345 秒"
     *    "PT15M"     -- 解析为 "15 分钟"（其中一分钟是 60 秒）
     *    "PT10H"     -- 解析为 "10 小时"（其中一小时是 3600 秒）
     *    "P2D"       -- 解析为 "2 天"（其中一天是 24 小时或 86400 秒）
     *    "P2DT3H4M"  -- 解析为 "2 天，3 小时和 4 分钟"
     *    "P-6H3M"    -- 解析为 "-6 小时和 +3 分钟"
     *    "-P6H3M"    -- 解析为 "-6 小时和 -3 分钟"
     *    "-P-6H+3M"  -- 解析为 "+6 小时和 -3 分钟"
     * </pre>
     *
     * @param text  要解析的文本，不为空
     * @return 解析的持续时间，不为空
     * @throws DateTimeParseException 如果文本无法解析为持续时间
     */
    public static Duration parse(CharSequence text) {
        Objects.requireNonNull(text, "text");
        Matcher matcher = PATTERN.matcher(text);
        if (matcher.matches()) {
            // 检查是否有字母 T 但没有时间部分
            if ("T".equals(matcher.group(3)) == false) {
                boolean negate = "-".equals(matcher.group(1));
                String dayMatch = matcher.group(2);
                String hourMatch = matcher.group(4);
                String minuteMatch = matcher.group(5);
                String secondMatch = matcher.group(6);
                String fractionMatch = matcher.group(7);
                if (dayMatch != null || hourMatch != null || minuteMatch != null || secondMatch != null) {
                    long daysAsSecs = parseNumber(text, dayMatch, SECONDS_PER_DAY, "days");
                    long hoursAsSecs = parseNumber(text, hourMatch, SECONDS_PER_HOUR, "hours");
                    long minsAsSecs = parseNumber(text, minuteMatch, SECONDS_PER_MINUTE, "minutes");
                    long seconds = parseNumber(text, secondMatch, 1, "seconds");
                    int nanos = parseFraction(text,  fractionMatch, seconds < 0 ? -1 : 1);
                    try {
                        return create(negate, daysAsSecs, hoursAsSecs, minsAsSecs, seconds, nanos);
                    } catch (ArithmeticException ex) {
                        throw (DateTimeParseException) new DateTimeParseException("Text cannot be parsed to a Duration: overflow", text, 0).initCause(ex);
                    }
                }
            }
        }
        throw new DateTimeParseException("Text cannot be parsed to a Duration", text, 0);
    }


                private static long parseNumber(CharSequence text, String parsed, int multiplier, String errorText) {
        // 正则限制为 [-+]?[0-9]+
        if (parsed == null) {
            return 0;
        }
        try {
            long val = Long.parseLong(parsed);
            return Math.multiplyExact(val, multiplier);
        } catch (NumberFormatException | ArithmeticException ex) {
            throw (DateTimeParseException) new DateTimeParseException("文本无法解析为持续时间: " + errorText, text, 0).initCause(ex);
        }
    }

    private static int parseFraction(CharSequence text, String parsed, int negate) {
        // 正则限制为 [0-9]{0,9}
        if (parsed == null || parsed.length() == 0) {
            return 0;
        }
        try {
            parsed = (parsed + "000000000").substring(0, 9);
            return Integer.parseInt(parsed) * negate;
        } catch (NumberFormatException | ArithmeticException ex) {
            throw (DateTimeParseException) new DateTimeParseException("文本无法解析为持续时间: 小数部分", text, 0).initCause(ex);
        }
    }

    private static Duration create(boolean negate, long daysAsSecs, long hoursAsSecs, long minsAsSecs, long secs, int nanos) {
        long seconds = Math.addExact(daysAsSecs, Math.addExact(hoursAsSecs, Math.addExact(minsAsSecs, secs)));
        if (negate) {
            return ofSeconds(seconds, nanos).negated();
        }
        return ofSeconds(seconds, nanos);
    }

    //-----------------------------------------------------------------------
    /**
     * 获取表示两个时间对象之间持续时间的 {@code Duration}。
     * <p>
     * 这会计算两个时间对象之间的持续时间。如果对象类型不同，则持续时间是基于第一个对象的类型计算的。
     * 例如，如果第一个参数是 {@code LocalTime}，则第二个参数将转换为 {@code LocalTime}。
     * <p>
     * 指定的时间对象必须支持 {@link ChronoUnit#SECONDS SECONDS} 单位。
     * 为了获得高精度，应支持 {@link ChronoUnit#NANOS NANOS} 单位或 {@link ChronoField#NANO_OF_SECOND NANO_OF_SECOND} 字段。
     * <p>
     * 如果结束时间在开始时间之前，此方法的结果可以是负的持续时间。
     * 为了确保结果为正的持续时间，可以在结果上调用 {@link #abs()}。
     *
     * @param startInclusive  开始时间，包含，不为空
     * @param endExclusive  结束时间，不包含，不为空
     * @return 一个 {@code Duration}，不为空
     * @throws DateTimeException 如果无法获取两个时间对象之间的秒数
     * @throws ArithmeticException 如果计算结果超出 {@code Duration} 的容量
     */
    public static Duration between(Temporal startInclusive, Temporal endExclusive) {
        try {
            return ofNanos(startInclusive.until(endExclusive, NANOS));
        } catch (DateTimeException | ArithmeticException ex) {
            long secs = startInclusive.until(endExclusive, SECONDS);
            long nanos;
            try {
                nanos = endExclusive.getLong(NANO_OF_SECOND) - startInclusive.getLong(NANO_OF_SECOND);
                if (secs > 0 && nanos < 0) {
                    secs++;
                } else if (secs < 0 && nanos > 0) {
                    secs--;
                }
            } catch (DateTimeException ex2) {
                nanos = 0;
            }
            return ofSeconds(secs, nanos);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * 使用秒和纳秒获取 {@code Duration} 的实例。
     *
     * @param seconds  持续时间的秒数，可以是正数或负数
     * @param nanoAdjustment  秒内的纳秒调整值，从 0 到 999,999,999
     */
    private static Duration create(long seconds, int nanoAdjustment) {
        if ((seconds | nanoAdjustment) == 0) {
            return ZERO;
        }
        return new Duration(seconds, nanoAdjustment);
    }

    /**
     * 使用秒和纳秒构造 {@code Duration} 的实例。
     *
     * @param seconds  持续时间的秒数，可以是正数或负数
     * @param nanos  秒内的纳秒数，从 0 到 999,999,999
     */
    private Duration(long seconds, int nanos) {
        super();
        this.seconds = seconds;
        this.nanos = nanos;
    }

    //-----------------------------------------------------------------------
    /**
     * 获取请求单位的值。
     * <p>
     * 这会返回两个支持的单位的值：{@link ChronoUnit#SECONDS SECONDS} 和 {@link ChronoUnit#NANOS NANOS}。
     * 其他单位会抛出异常。
     *
     * @param unit  要返回值的 {@code TemporalUnit}
     * @return 单位的 long 值
     * @throws DateTimeException 如果单位不支持
     * @throws UnsupportedTemporalTypeException 如果单位不支持
     */
    @Override
    public long get(TemporalUnit unit) {
        if (unit == SECONDS) {
            return seconds;
        } else if (unit == NANOS) {
            return nanos;
        } else {
            throw new UnsupportedTemporalTypeException("不支持的单位: " + unit);
        }
    }

    /**
     * 获取此持续时间支持的单位集。
     * <p>
     * 支持的单位是 {@link ChronoUnit#SECONDS SECONDS} 和 {@link ChronoUnit#NANOS NANOS}。
     * 它们按秒、纳秒的顺序返回。
     * <p>
     * 可以将此集合与 {@link #get(TemporalUnit)} 一起使用来访问持续时间的全部状态。
     *
     * @return 包含秒和纳秒单位的列表，不为空
     */
    @Override
    public List<TemporalUnit> getUnits() {
        return DurationUnits.UNITS;
    }

    /**
     * 私有类，用于延迟初始化此列表，直到需要时。
     * 由于 Duration 和 ChronoUnit 之间的循环依赖关系，无法在 Duration 中进行简单的初始化。
     */
    private static class DurationUnits {
        static final List<TemporalUnit> UNITS =
                Collections.unmodifiableList(Arrays.<TemporalUnit>asList(SECONDS, NANOS));
    }

    //-----------------------------------------------------------------------
    /**
     * 检查此持续时间是否为零长度。
     * <p>
     * {@code Duration} 表示时间线上两点之间的有向距离，因此可以是正数、零或负数。
     * 此方法检查长度是否为零。
     *
     * @return 如果此持续时间的总长度为零，则返回 true
     */
    public boolean isZero() {
        return (seconds | nanos) == 0;
    }

    /**
     * 检查此持续时间是否为负数，不包括零。
     * <p>
     * {@code Duration} 表示时间线上两点之间的有向距离，因此可以是正数、零或负数。
     * 此方法检查长度是否小于零。
     *
     * @return 如果此持续时间的总长度小于零，则返回 true
     */
    public boolean isNegative() {
        return seconds < 0;
    }

    //-----------------------------------------------------------------------
    /**
     * 获取此持续时间的秒数。
     * <p>
     * 持续时间的长度使用两个字段存储：秒和纳秒。
     * 纳秒部分是从 0 到 999,999,999 的值，是对秒长的调整。
     * 通过调用此方法和 {@link #getNano()} 可以定义总持续时间。
     * <p>
     * {@code Duration} 表示时间线上两点之间的有向距离。
     * 负的持续时间通过秒部分的负号表示。
     * -1 纳秒的持续时间存储为 -1 秒加上 999,999,999 纳秒。
     *
     * @return 持续时间长度的秒部分，可以是正数或负数
     */
    public long getSeconds() {
        return seconds;
    }

    /**
     * 获取此持续时间的秒内的纳秒数。
     * <p>
     * 持续时间的长度使用两个字段存储：秒和纳秒。
     * 纳秒部分是从 0 到 999,999,999 的值，是对秒长的调整。
     * 通过调用此方法和 {@link #getSeconds()} 可以定义总持续时间。
     * <p>
     * {@code Duration} 表示时间线上两点之间的有向距离。
     * 负的持续时间通过秒部分的负号表示。
     * -1 纳秒的持续时间存储为 -1 秒加上 999,999,999 纳秒。
     *
     * @return 持续时间长度的秒内纳秒部分，从 0 到 999,999,999
     */
    public int getNano() {
        return nanos;
    }

    //-----------------------------------------------------------------------
    /**
     * 返回具有指定秒数的此持续时间的副本。
     * <p>
     * 返回的持续时间具有指定的秒数，保留此持续时间的纳秒部分。
     * <p>
     * 本实例是不可变的，不受此方法调用的影响。
     *
     * @param seconds  要表示的秒数，可以是负数
     * @return 基于此持续时间并具有请求秒数的 {@code Duration}，不为空
     */
    public Duration withSeconds(long seconds) {
        return create(seconds, nanos);
    }

    /**
     * 返回具有指定纳秒数的此持续时间的副本。
     * <p>
     * 返回的持续时间具有指定的纳秒数，保留此持续时间的秒部分。
     * <p>
     * 本实例是不可变的，不受此方法调用的影响。
     *
     * @param nanoOfSecond  要表示的纳秒数，从 0 到 999,999,999
     * @return 基于此持续时间并具有请求纳秒数的 {@code Duration}，不为空
     * @throws DateTimeException 如果纳秒数无效
     */
    public Duration withNanos(int nanoOfSecond) {
        NANO_OF_SECOND.checkValidIntValue(nanoOfSecond);
        return create(seconds, nanoOfSecond);
    }

    //-----------------------------------------------------------------------
    /**
     * 返回具有指定持续时间相加的此持续时间的副本。
     * <p>
     * 本实例是不可变的，不受此方法调用的影响。
     *
     * @param duration  要添加的持续时间，可以是正数或负数，不为空
     * @return 基于此持续时间并具有指定持续时间相加的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration plus(Duration duration) {
        return plus(duration.getSeconds(), duration.getNano());
     }

    /**
     * 返回具有指定持续时间相加的此持续时间的副本。
     * <p>
     * 持续时间的量是根据指定的单位测量的。
     * 只有部分单位被此方法接受。
     * 单位必须具有 {@linkplain TemporalUnit#isDurationEstimated() 精确的持续时间} 或是 {@link ChronoUnit#DAYS}，后者被视为 24 小时。其他单位会抛出异常。
     * <p>
     * 本实例是不可变的，不受此方法调用的影响。
     *
     * @param amountToAdd  要添加的量，根据单位测量，可以是正数或负数
     * @param unit  量的测量单位，必须具有精确的持续时间，不为空
     * @return 基于此持续时间并具有指定持续时间相加的 {@code Duration}，不为空
     * @throws UnsupportedTemporalTypeException 如果单位不支持
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration plus(long amountToAdd, TemporalUnit unit) {
        Objects.requireNonNull(unit, "unit");
        if (unit == DAYS) {
            return plus(Math.multiplyExact(amountToAdd, SECONDS_PER_DAY), 0);
        }
        if (unit.isDurationEstimated()) {
            throw new UnsupportedTemporalTypeException("单位必须没有估计的持续时间");
        }
        if (amountToAdd == 0) {
            return this;
        }
        if (unit instanceof ChronoUnit) {
            switch ((ChronoUnit) unit) {
                case NANOS: return plusNanos(amountToAdd);
                case MICROS: return plusSeconds((amountToAdd / (1000_000L * 1000)) * 1000).plusNanos((amountToAdd % (1000_000L * 1000)) * 1000);
                case MILLIS: return plusMillis(amountToAdd);
                case SECONDS: return plusSeconds(amountToAdd);
            }
            return plusSeconds(Math.multiplyExact(unit.getDuration().seconds, amountToAdd));
        }
        Duration duration = unit.getDuration().multipliedBy(amountToAdd);
        return plusSeconds(duration.getSeconds()).plusNanos(duration.getNano());
    }

    //-----------------------------------------------------------------------
    /**
     * 返回具有指定标准 24 小时天数相加的此持续时间的副本。
     * <p>
     * 天数乘以 86400 以获取要添加的秒数。
     * 这是基于标准的 24 小时天的定义。
     * <p>
     * 本实例是不可变的，不受此方法调用的影响。
     *
     * @param daysToAdd  要添加的天数，可以是正数或负数
     * @return 基于此持续时间并具有指定天数相加的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration plusDays(long daysToAdd) {
        return plus(Math.multiplyExact(daysToAdd, SECONDS_PER_DAY), 0);
    }

    /**
     * 返回具有指定小时数相加的此持续时间的副本。
     * <p>
     * 本实例是不可变的，不受此方法调用的影响。
     *
     * @param hoursToAdd  要添加的小时数，可以是正数或负数
     * @return 基于此持续时间并具有指定小时数相加的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration plusHours(long hoursToAdd) {
        return plus(Math.multiplyExact(hoursToAdd, SECONDS_PER_HOUR), 0);
    }

    /**
     * 返回具有指定分钟数相加的此持续时间的副本。
     * <p>
     * 本实例是不可变的，不受此方法调用的影响。
     *
     * @param minutesToAdd  要添加的分钟数，可以是正数或负数
     * @return 基于此持续时间并具有指定分钟数相加的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration plusMinutes(long minutesToAdd) {
        return plus(Math.multiplyExact(minutesToAdd, SECONDS_PER_MINUTE), 0);
    }

    /**
     * 返回具有指定秒数相加的此持续时间的副本。
     * <p>
     * 本实例是不可变的，不受此方法调用的影响。
     *
     * @param secondsToAdd  要添加的秒数，可以是正数或负数
     * @return 基于此持续时间并具有指定秒数相加的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration plusSeconds(long secondsToAdd) {
        return plus(secondsToAdd, 0);
    }


                /**
     * 返回一个副本，其中指定了要添加的毫秒数。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @param millisToAdd  要添加的毫秒数，正数或负数
     * @return 基于此持续时间并添加了指定毫秒数的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration plusMillis(long millisToAdd) {
        return plus(millisToAdd / 1000, (millisToAdd % 1000) * 1000_000);
    }

    /**
     * 返回一个副本，其中指定了要添加的纳秒数。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @param nanosToAdd  要添加的纳秒数，正数或负数
     * @return 基于此持续时间并添加了指定纳秒数的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration plusNanos(long nanosToAdd) {
        return plus(0, nanosToAdd);
    }

    /**
     * 返回一个副本，其中指定了要添加的持续时间。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @param secondsToAdd  要添加的秒数，正数或负数
     * @param nanosToAdd  要添加的纳秒数，正数或负数
     * @return 基于此持续时间并添加了指定秒数的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    private Duration plus(long secondsToAdd, long nanosToAdd) {
        if ((secondsToAdd | nanosToAdd) == 0) {
            return this;
        }
        long epochSec = Math.addExact(seconds, secondsToAdd);
        epochSec = Math.addExact(epochSec, nanosToAdd / NANOS_PER_SECOND);
        nanosToAdd = nanosToAdd % NANOS_PER_SECOND;
        long nanoAdjustment = nanos + nanosToAdd;  // 安全的 int + NANOS_PER_SECOND
        return ofSeconds(epochSec, nanoAdjustment);
    }

    //-----------------------------------------------------------------------
    /**
     * 返回一个副本，其中指定了要减去的持续时间。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @param duration  要减去的持续时间，正数或负数，不为空
     * @return 基于此持续时间并减去了指定持续时间的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration minus(Duration duration) {
        long secsToSubtract = duration.getSeconds();
        int nanosToSubtract = duration.getNano();
        if (secsToSubtract == Long.MIN_VALUE) {
            return plus(Long.MAX_VALUE, -nanosToSubtract).plus(1, 0);
        }
        return plus(-secsToSubtract, -nanosToSubtract);
     }

    /**
     * 返回一个副本，其中指定了要减去的持续时间。
     * <p>
     * 持续时间的量是根据指定的单位测量的。
     * 只有部分单位被此方法接受。
     * 单位必须具有 {@linkplain TemporalUnit#isDurationEstimated() 精确的持续时间} 或
     * 是 {@link ChronoUnit#DAYS}，被视为24小时。其他单位会抛出异常。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @param amountToSubtract  要减去的量，以单位为单位，正数或负数
     * @param unit  量的测量单位，必须具有精确的持续时间，不为空
     * @return 基于此持续时间并减去了指定持续时间的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration minus(long amountToSubtract, TemporalUnit unit) {
        return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-amountToSubtract, unit));
    }

    //-----------------------------------------------------------------------
    /**
     * 返回一个副本，其中指定了标准24小时天数的持续时间被减去。
     * <p>
     * 天数乘以86400以获得要减去的秒数。
     * 这是基于标准的一天定义为24小时。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @param daysToSubtract  要减去的天数，正数或负数
     * @return 基于此持续时间并减去了指定天数的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration minusDays(long daysToSubtract) {
        return (daysToSubtract == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-daysToSubtract));
    }

    /**
     * 返回一个副本，其中指定了小时数的持续时间被减去。
     * <p>
     * 小时数乘以3600以获得要减去的秒数。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @param hoursToSubtract  要减去的小时数，正数或负数
     * @return 基于此持续时间并减去了指定小时数的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration minusHours(long hoursToSubtract) {
        return (hoursToSubtract == Long.MIN_VALUE ? plusHours(Long.MAX_VALUE).plusHours(1) : plusHours(-hoursToSubtract));
    }

    /**
     * 返回一个副本，其中指定了分钟数的持续时间被减去。
     * <p>
     * 分钟数乘以60以获得要减去的秒数。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @param minutesToSubtract  要减去的分钟数，正数或负数
     * @return 基于此持续时间并减去了指定分钟数的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration minusMinutes(long minutesToSubtract) {
        return (minutesToSubtract == Long.MIN_VALUE ? plusMinutes(Long.MAX_VALUE).plusMinutes(1) : plusMinutes(-minutesToSubtract));
    }

    /**
     * 返回一个副本，其中指定了秒数的持续时间被减去。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @param secondsToSubtract  要减去的秒数，正数或负数
     * @return 基于此持续时间并减去了指定秒数的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration minusSeconds(long secondsToSubtract) {
        return (secondsToSubtract == Long.MIN_VALUE ? plusSeconds(Long.MAX_VALUE).plusSeconds(1) : plusSeconds(-secondsToSubtract));
    }

    /**
     * 返回一个副本，其中指定了毫秒数的持续时间被减去。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @param millisToSubtract  要减去的毫秒数，正数或负数
     * @return 基于此持续时间并减去了指定毫秒数的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration minusMillis(long millisToSubtract) {
        return (millisToSubtract == Long.MIN_VALUE ? plusMillis(Long.MAX_VALUE).plusMillis(1) : plusMillis(-millisToSubtract));
    }

    /**
     * 返回一个副本，其中指定了纳秒数的持续时间被减去。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @param nanosToSubtract  要减去的纳秒数，正数或负数
     * @return 基于此持续时间并减去了指定纳秒数的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration minusNanos(long nanosToSubtract) {
        return (nanosToSubtract == Long.MIN_VALUE ? plusNanos(Long.MAX_VALUE).plusNanos(1) : plusNanos(-nanosToSubtract));
    }

    //-----------------------------------------------------------------------
    /**
     * 返回一个副本，其中持续时间乘以标量。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @param multiplicand  要乘以的值，正数或负数
     * @return 基于此持续时间并乘以指定标量的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration multipliedBy(long multiplicand) {
        if (multiplicand == 0) {
            return ZERO;
        }
        if (multiplicand == 1) {
            return this;
        }
        return create(toSeconds().multiply(BigDecimal.valueOf(multiplicand)));
     }

    /**
     * 返回一个副本，其中持续时间除以指定的值。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @param divisor  要除以的值，正数或负数，不为零
     * @return 基于此持续时间并除以指定除数的 {@code Duration}，不为空
     * @throws ArithmeticException 如果除数为零或发生数值溢出
     */
    public Duration dividedBy(long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("不能除以零");
        }
        if (divisor == 1) {
            return this;
        }
        return create(toSeconds().divide(BigDecimal.valueOf(divisor), RoundingMode.DOWN));
     }

    /**
     * 将此持续时间转换为以 {@code BigDecimal} 表示的总秒数和小数纳秒。
     *
     * @return 以秒为单位的持续时间总长度，精度为9，不为空
     */
    private BigDecimal toSeconds() {
        return BigDecimal.valueOf(seconds).add(BigDecimal.valueOf(nanos, 9));
    }

    /**
     * 从秒数创建 {@code Duration} 的实例。
     *
     * @param seconds  秒数，精度为9，正数或负数
     * @return 一个 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    private static Duration create(BigDecimal seconds) {
        BigInteger nanos = seconds.movePointRight(9).toBigIntegerExact();
        BigInteger[] divRem = nanos.divideAndRemainder(BI_NANOS_PER_SECOND);
        if (divRem[0].bitLength() > 63) {
            throw new ArithmeticException("超出 Duration 的容量: " + nanos);
        }
        return ofSeconds(divRem[0].longValue(), divRem[1].intValue());
    }

    //-----------------------------------------------------------------------
    /**
     * 返回一个副本，其中持续时间的长度取反。
     * <p>
     * 此方法将此持续时间的总长度的符号互换。
     * 例如，{@code PT1.3S} 将返回为 {@code PT-1.3S}。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @return 基于此持续时间并取反的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration negated() {
        return multipliedBy(-1);
    }

    /**
     * 返回一个副本，其中持续时间的长度为正。
     * <p>
     * 此方法通过有效地移除任何负的总长度的符号来返回一个正的持续时间。
     * 例如，{@code PT-1.3S} 将返回为 {@code PT1.3S}。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @return 基于此持续时间并取绝对值的 {@code Duration}，不为空
     * @throws ArithmeticException 如果发生数值溢出
     */
    public Duration abs() {
        return isNegative() ? negated() : this;
    }

    //-------------------------------------------------------------------------
    /**
     * 将此持续时间添加到指定的时间对象。
     * <p>
     * 这将返回一个与输入类型相同的时间对象，其中添加了此持续时间。
     * <p>
     * 在大多数情况下，通过使用 {@link Temporal#plus(TemporalAmount)} 反转调用模式更为清晰。
     * <pre>
     *   // 这两行是等效的，但推荐第二种方法
     *   dateTime = thisDuration.addTo(dateTime);
     *   dateTime = dateTime.plus(thisDuration);
     * </pre>
     * <p>
     * 计算将添加秒数，然后是纳秒数。
     * 只有非零量才会被添加。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @param temporal  要调整的时间对象，不为空
     * @return 调整后的同类型对象，不为空
     * @throws DateTimeException 如果无法添加
     * @throws ArithmeticException 如果发生数值溢出
     */
    @Override
    public Temporal addTo(Temporal temporal) {
        if (seconds != 0) {
            temporal = temporal.plus(seconds, SECONDS);
        }
        if (nanos != 0) {
            temporal = temporal.plus(nanos, NANOS);
        }
        return temporal;
    }

    /**
     * 从指定的时间对象中减去此持续时间。
     * <p>
     * 这将返回一个与输入类型相同的时间对象，其中减去了此持续时间。
     * <p>
     * 在大多数情况下，通过使用 {@link Temporal#minus(TemporalAmount)} 反转调用模式更为清晰。
     * <pre>
     *   // 这两行是等效的，但推荐第二种方法
     *   dateTime = thisDuration.subtractFrom(dateTime);
     *   dateTime = dateTime.minus(thisDuration);
     * </pre>
     * <p>
     * 计算将减去秒数，然后是纳秒数。
     * 只有非零量才会被添加。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @param temporal  要调整的时间对象，不为空
     * @return 调整后的同类型对象，不为空
     * @throws DateTimeException 如果无法减去
     * @throws ArithmeticException 如果发生数值溢出
     */
    @Override
    public Temporal subtractFrom(Temporal temporal) {
        if (seconds != 0) {
            temporal = temporal.minus(seconds, SECONDS);
        }
        if (nanos != 0) {
            temporal = temporal.minus(nanos, NANOS);
        }
        return temporal;
    }

    //-----------------------------------------------------------------------
    /**
     * 获取此持续时间中的天数。
     * <p>
     * 这将通过将秒数除以86400来返回持续时间中的总天数。
     * 这是基于标准的一天定义为24小时。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @return 持续时间中的天数，可能为负数
     */
    public long toDays() {
        return seconds / SECONDS_PER_DAY;
    }

    /**
     * 获取此持续时间中的小时数。
     * <p>
     * 这将通过将秒数除以3600来返回持续时间中的总小时数。
     * <p>
     * 本实例是不可变的，不会受此方法调用的影响。
     *
     * @return 持续时间中的小时数，可能为负数
     */
    public long toHours() {
        return seconds / SECONDS_PER_HOUR;
    }


                /**
     * 获取此持续时间中的分钟数。
     * <p>
     * 通过将秒数除以60来返回持续时间中的总分钟数。
     * <p>
     * 该实例是不可变的，此方法调用不会对其产生影响。
     *
     * @return 持续时间中的分钟数，可能为负数
     */
    public long toMinutes() {
        return seconds / SECONDS_PER_MINUTE;
    }

    /**
     * 将此持续时间转换为总毫秒数。
     * <p>
     * 如果此持续时间太大，无法适应 {@code long} 毫秒，则会抛出异常。
     * <p>
     * 如果此持续时间的精度大于毫秒，则转换将丢弃多余的精度信息，就像纳秒数被100万整除一样。
     *
     * @return 持续时间的总毫秒数
     * @throws ArithmeticException 如果发生数值溢出
     */
    public long toMillis() {
        long millis = Math.multiplyExact(seconds, 1000);
        millis = Math.addExact(millis, nanos / 1000_000);
        return millis;
    }

    /**
     * 将此持续时间转换为总纳秒数，表示为 {@code long}。
     * <p>
     * 如果此持续时间太大，无法适应 {@code long} 纳秒，则会抛出异常。
     *
     * @return 持续时间的总纳秒数
     * @throws ArithmeticException 如果发生数值溢出
     */
    public long toNanos() {
        long totalNanos = Math.multiplyExact(seconds, NANOS_PER_SECOND);
        totalNanos = Math.addExact(totalNanos, nanos);
        return totalNanos;
    }

    //-----------------------------------------------------------------------
    /**
     * 将此持续时间与指定的 {@code Duration} 进行比较。
     * <p>
     * 比较基于持续时间的总长度。
     * 它与 {@link Comparable} 中定义的“与 equals 一致”。
     *
     * @param otherDuration  要比较的其他持续时间，不为 null
     * @return 比较值，小于0表示小于，大于0表示大于
     */
    @Override
    public int compareTo(Duration otherDuration) {
        int cmp = Long.compare(seconds, otherDuration.seconds);
        if (cmp != 0) {
            return cmp;
        }
        return nanos - otherDuration.nanos;
    }

    //-----------------------------------------------------------------------
    /**
     * 检查此持续时间是否等于指定的 {@code Duration}。
     * <p>
     * 比较基于持续时间的总长度。
     *
     * @param otherDuration  其他持续时间，null 返回 false
     * @return 如果其他持续时间等于此持续时间，则返回 true
     */
    @Override
    public boolean equals(Object otherDuration) {
        if (this == otherDuration) {
            return true;
        }
        if (otherDuration instanceof Duration) {
            Duration other = (Duration) otherDuration;
            return this.seconds == other.seconds &&
                   this.nanos == other.nanos;
        }
        return false;
    }

    /**
     * 此持续时间的哈希码。
     *
     * @return 一个合适的哈希码
     */
    @Override
    public int hashCode() {
        return ((int) (seconds ^ (seconds >>> 32))) + (51 * nanos);
    }

    //-----------------------------------------------------------------------
    /**
     * 使用 ISO-8601 秒表示法返回此持续时间的字符串表示形式，例如 {@code PT8H6M12.345S}。
     * <p>
     * 返回字符串的格式为 {@code PTnHnMnS}，其中 n 是持续时间的相应小时、分钟或秒部分。
     * 任何小数秒都在秒部分的小数点后表示。
     * 如果某部分的值为零，则省略该部分。
     * 小时、分钟和秒将具有相同的符号。
     * <p>
     * 示例：
     * <pre>
     *    "20.345 秒"                 -- "PT20.345S
     *    "15 分钟" (15 * 60 秒)   -- "PT15M"
     *    "10 小时" (10 * 3600 秒)   -- "PT10H"
     *    "2 天" (2 * 86400 秒)     -- "PT48H"
     * </pre>
     * 注意，24小时的倍数不会输出为天，以避免与 {@code Period} 混淆。
     *
     * @return 此持续时间的 ISO-8601 表示形式，不为 null
     */
    @Override
    public String toString() {
        if (this == ZERO) {
            return "PT0S";
        }
        long hours = seconds / SECONDS_PER_HOUR;
        int minutes = (int) ((seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE);
        int secs = (int) (seconds % SECONDS_PER_MINUTE);
        StringBuilder buf = new StringBuilder(24);
        buf.append("PT");
        if (hours != 0) {
            buf.append(hours).append('H');
        }
        if (minutes != 0) {
            buf.append(minutes).append('M');
        }
        if (secs == 0 && nanos == 0 && buf.length() > 2) {
            return buf.toString();
        }
        if (secs < 0 && nanos > 0) {
            if (secs == -1) {
                buf.append("-0");
            } else {
                buf.append(secs + 1);
            }
        } else {
            buf.append(secs);
        }
        if (nanos > 0) {
            int pos = buf.length();
            if (secs < 0) {
                buf.append(2 * NANOS_PER_SECOND - nanos);
            } else {
                buf.append(nanos + NANOS_PER_SECOND);
            }
            while (buf.charAt(buf.length() - 1) == '0') {
                buf.setLength(buf.length() - 1);
            }
            buf.setCharAt(pos, '.');
        }
        buf.append('S');
        return buf.toString();
    }

    //-----------------------------------------------------------------------
    /**
     * 使用
     * <a href="../../serialized-form.html#java.time.Ser">专用序列化形式</a> 写入对象。
     * @serialData
     * <pre>
     *  out.writeByte(1);  // 标识一个 Duration
     *  out.writeLong(seconds);
     *  out.writeInt(nanos);
     * </pre>
     *
     * @return {@code Ser} 的实例，不为 null
     */
    private Object writeReplace() {
        return new Ser(Ser.DURATION_TYPE, this);
    }

    /**
     * 防御恶意流。
     *
     * @param s 要读取的流
     * @throws InvalidObjectException 总是抛出
     */
    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("通过序列化代理进行反序列化");
    }

    void writeExternal(DataOutput out) throws IOException {
        out.writeLong(seconds);
        out.writeInt(nanos);
    }

    static Duration readExternal(DataInput in) throws IOException {
        long seconds = in.readLong();
        int nanos = in.readInt();
        return Duration.ofSeconds(seconds, nanos);
    }

}
