/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.util.prefs;

/**
 * 用于接收偏好设置更改事件的监听器。
 *
 * @author  Josh Bloch
 * @see Preferences
 * @see PreferenceChangeEvent
 * @see NodeChangeListener
 * @since   1.4
 */
@FunctionalInterface
public interface PreferenceChangeListener extends java.util.EventListener {
    /**
     * 当偏好设置被添加、移除或其值被更改时，此方法将被调用。
     * <p>
     * @param evt 描述事件源和已更改的偏好设置的 PreferenceChangeEvent 对象。
     */
    void preferenceChange(PreferenceChangeEvent evt);
}
