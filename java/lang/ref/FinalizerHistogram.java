/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.ref;


import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 该 FinalizerHistogram 类用于支持 GC.finalizer_info 诊断命令。
 * 它由 VM 调用。
 */

final class FinalizerHistogram {

    private static final class Entry {
        private int instanceCount;
        private final String className;

        int getInstanceCount() {
            return instanceCount;
        }

        void increment() {
            instanceCount += 1;
        }

        Entry(String className) {
            this.className = className;
        }
    }

    // 下面的方法由 VM 调用，VM 期望特定的条目类布局。

    static Entry[] getFinalizerHistogram() {
        Map<String, Entry> countMap = new HashMap<>();
        ReferenceQueue<Object> queue = Finalizer.getQueue();
        queue.forEach(r -> {
            Object referent = r.get();
            if (referent != null) {
                countMap.computeIfAbsent(
                    referent.getClass().getName(), Entry::new).increment();
                /* 清除包含此变量的堆栈槽，以减少
                   保守 GC 的错误保留机会 */
                referent = null;
            }
        });

        Entry fhe[] = countMap.values().toArray(new Entry[countMap.size()]);
        Arrays.sort(fhe,
                Comparator.comparingInt(Entry::getInstanceCount).reversed());
        return fhe;
    }
}
