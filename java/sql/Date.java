/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.sql;

import java.time.Instant;
import java.time.LocalDate;

/**
 * <P>一个薄包装器，用于封装毫秒值，使 JDBC 能够识别此值为 SQL <code>DATE</code> 值。毫秒值表示自 1970 年 1 月 1 日 00:00:00.000 GMT 以来经过的毫秒数。
 * <p>
 * 为了符合 SQL <code>DATE</code> 的定义，由 <code>java.sql.Date</code> 实例封装的毫秒值必须通过将与该实例关联的特定时区中的小时、分钟、秒和毫秒设置为零来“规范化”。
 */
public class Date extends java.util.Date {

    /**
     * 使用给定的年、月和日构造一个 <code>Date</code> 对象。
     * <P>
     * 如果给定的参数超出范围，则结果是未定义的。
     *
     * @param year 年份减去 1900；必须为 0 到 8099。（注意 8099 是 9999 减去 1900。）
     * @param month 0 到 11
     * @param day 1 到 31
     * @deprecated 请改用构造函数 <code>Date(long date)</code>
     */
    @Deprecated
    public Date(int year, int month, int day) {
        super(year, month, day);
    }

    /**
     * 使用给定的毫秒时间值构造一个 <code>Date</code> 对象。如果给定的毫秒值包含时间信息，驱动程序将把时间组件设置为默认时区（运行应用程序的 Java 虚拟机的时区）中对应于零 GMT 的时间。
     *
     * @param date 自 1970 年 1 月 1 日 00:00:00 GMT 以来的毫秒数，不超过 8099 年的毫秒表示。负数表示 1970 年 1 月 1 日 00:00:00 GMT 之前的毫秒数。
     */
    public Date(long date) {
        // 如果毫秒日期值包含时间信息，将其屏蔽。
        super(date);

    }

    /**
     * 使用给定的毫秒时间值设置现有的 <code>Date</code> 对象。如果给定的毫秒值包含时间信息，驱动程序将把时间组件设置为默认时区（运行应用程序的 Java 虚拟机的时区）中对应于零 GMT 的时间。
     *
     * @param date 自 1970 年 1 月 1 日 00:00:00 GMT 以来的毫秒数，不超过 8099 年的毫秒表示。负数表示 1970 年 1 月 1 日 00:00:00 GMT 之前的毫秒数。
     */
    public void setTime(long date) {
        // 如果毫秒日期值包含时间信息，将其屏蔽。
        super.setTime(date);
    }

    /**
     * 将 JDBC 日期转义格式的字符串转换为 <code>Date</code> 值。
     *
     * @param s 表示日期的 <code>String</code> 对象，格式为 "yyyy-[m]m-[d]d"。月份和日期的前导零也可以省略。
     * @return 表示给定日期的 <code>java.sql.Date</code> 对象
     * @throws IllegalArgumentException 如果给定的日期不在 JDBC 日期转义格式 (yyyy-[m]m-[d]d) 中
     */
    public static Date valueOf(String s) {
        final int YEAR_LENGTH = 4;
        final int MONTH_LENGTH = 2;
        final int DAY_LENGTH = 2;
        final int MAX_MONTH = 12;
        final int MAX_DAY = 31;
        int firstDash;
        int secondDash;
        Date d = null;
        if (s == null) {
            throw new java.lang.IllegalArgumentException();
        }

        firstDash = s.indexOf('-');
        secondDash = s.indexOf('-', firstDash + 1);

        if ((firstDash > 0) && (secondDash > 0) && (secondDash < s.length() - 1)) {
            String yyyy = s.substring(0, firstDash);
            String mm = s.substring(firstDash + 1, secondDash);
            String dd = s.substring(secondDash + 1);
            if (yyyy.length() == YEAR_LENGTH &&
                    (mm.length() >= 1 && mm.length() <= MONTH_LENGTH) &&
                    (dd.length() >= 1 && dd.length() <= DAY_LENGTH)) {
                int year = Integer.parseInt(yyyy);
                int month = Integer.parseInt(mm);
                int day = Integer.parseInt(dd);

                if ((month >= 1 && month <= MAX_MONTH) && (day >= 1 && day <= MAX_DAY)) {
                    d = new Date(year - 1900, month - 1, day);
                }
            }
        }
        if (d == null) {
            throw new java.lang.IllegalArgumentException();
        }

        return d;

    }


    /**
     * 将日期格式化为日期转义格式 yyyy-mm-dd。
     * <P>
     * @return 格式为 yyyy-mm-dd 的字符串
     */
    @SuppressWarnings("deprecation")
    public String toString () {
        int year = super.getYear() + 1900;
        int month = super.getMonth() + 1;
        int day = super.getDate();

        char buf[] = "2000-00-00".toCharArray();
        buf[0] = Character.forDigit(year/1000,10);
        buf[1] = Character.forDigit((year/100)%10,10);
        buf[2] = Character.forDigit((year/10)%10,10);
        buf[3] = Character.forDigit(year%10,10);
        buf[5] = Character.forDigit(month/10,10);
        buf[6] = Character.forDigit(month%10,10);
        buf[8] = Character.forDigit(day/10,10);
        buf[9] = Character.forDigit(day%10,10);

        return new String(buf);
    }

    // 覆盖从 java.util.Date 继承的所有时间操作；

   /**
    * 此方法已废弃，不应使用，因为 SQL 日期值没有时间组件。
    *
    * @deprecated
    * @exception java.lang.IllegalArgumentException 如果调用此方法
    * @see #setHours
    */
    @Deprecated
    public int getHours() {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * 此方法已废弃，不应使用，因为 SQL 日期值没有时间组件。
    *
    * @deprecated
    * @exception java.lang.IllegalArgumentException 如果调用此方法
    * @see #setMinutes
    */
    @Deprecated
    public int getMinutes() {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * 此方法已废弃，不应使用，因为 SQL 日期值没有时间组件。
    *
    * @deprecated
    * @exception java.lang.IllegalArgumentException 如果调用此方法
    * @see #setSeconds
    */
    @Deprecated
    public int getSeconds() {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * 此方法已废弃，不应使用，因为 SQL 日期值没有时间组件。
    *
    * @deprecated
    * @exception java.lang.IllegalArgumentException 如果调用此方法
    * @see #getHours
    */
    @Deprecated
    public void setHours(int i) {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * 此方法已废弃，不应使用，因为 SQL 日期值没有时间组件。
    *
    * @deprecated
    * @exception java.lang.IllegalArgumentException 如果调用此方法
    * @see #getMinutes
    */
    @Deprecated
    public void setMinutes(int i) {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * 此方法已废弃，不应使用，因为 SQL 日期值没有时间组件。
    *
    * @deprecated
    * @exception java.lang.IllegalArgumentException 如果调用此方法
    * @see #getSeconds
    */
    @Deprecated
    public void setSeconds(int i) {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * 私有序列化版本唯一 ID，以确保序列化兼容性。
    */
    static final long serialVersionUID = 1511598038487230103L;

    /**
     * 从 {@link LocalDate} 对象获取一个 {@code Date} 实例，该实例具有与给定的 {@code LocalDate} 相同的年、月和日值。
     * <p>
     * 提供的 {@code LocalDate} 被解释为本地时区中的日期。
     *
     * @param date 要转换的 {@code LocalDate}
     * @return 一个 {@code Date} 对象
     * @exception NullPointerException 如果 {@code date} 为 null
     * @since 1.8
     */
    @SuppressWarnings("deprecation")
    public static Date valueOf(LocalDate date) {
        return new Date(date.getYear() - 1900, date.getMonthValue() -1,
                        date.getDayOfMonth());
    }

    /**
     * 将此 {@code Date} 对象转换为一个 {@code LocalDate}
     * <p>
     * 转换创建一个表示与此 {@code Date} 在本地时区中相同日期值的 {@code LocalDate}
     *
     * @return 表示相同日期值的 {@code LocalDate} 对象
     *
     * @since 1.8
     */
    @SuppressWarnings("deprecation")
    public LocalDate toLocalDate() {
        return LocalDate.of(getYear() + 1900, getMonth() + 1, getDate());
    }

   /**
    * 此方法总是抛出一个 UnsupportedOperationException，不应使用，因为 SQL {@code Date} 值没有时间组件。
    *
    * @exception java.lang.UnsupportedOperationException 如果调用此方法
    */
    @Override
    public Instant toInstant() {
        throw new java.lang.UnsupportedOperationException();
    }
}
