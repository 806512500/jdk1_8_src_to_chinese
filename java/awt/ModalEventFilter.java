/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
package java.awt;

import java.awt.event.*;

import sun.awt.AppContext;

abstract class ModalEventFilter implements EventFilter {

    protected Dialog modalDialog;
    protected boolean disabled;

    protected ModalEventFilter(Dialog modalDialog) {
        this.modalDialog = modalDialog;
        disabled = false;
    }

    Dialog getModalDialog() {
        return modalDialog;
    }

    public FilterAction acceptEvent(AWTEvent event) {
        if (disabled || !modalDialog.isVisible()) {
            return FilterAction.ACCEPT;
        }
        int eventID = event.getID();
        if ((eventID >= MouseEvent.MOUSE_FIRST &&
             eventID <= MouseEvent.MOUSE_LAST) ||
            (eventID >= ActionEvent.ACTION_FIRST &&
             eventID <= ActionEvent.ACTION_LAST) ||
            eventID == WindowEvent.WINDOW_CLOSING)
        {
            Object o = event.getSource();
            if (o instanceof sun.awt.ModalExclude) {
                // 从模态性中排除此对象，并继续处理其事件。
            } else if (o instanceof Component) {
                Component c = (Component)o;
                while ((c != null) && !(c instanceof Window)) {
                    c = c.getParent_NoClientCode();
                }
                if (c != null) {
                    return acceptWindow((Window)c);
                }
            }
        }
        return FilterAction.ACCEPT;
    }

    protected abstract FilterAction acceptWindow(Window w);

    // 当模态对话框被隐藏时，其模态过滤器可能不会立即从 EventDispatchThread 事件过滤器中删除，
    // 因此我们需要将过滤器标记为禁用以防止其工作。仅检查模态对话框的可见性是不够的，
    // 因为它可能被隐藏，然后再次显示，带有新的事件泵和新的过滤器。
    void disable() {
        disabled = true;
    }

    int compareTo(ModalEventFilter another) {
        Dialog anotherDialog = another.getModalDialog();
        // 检查 modalDialog 是否在 anotherDialog 的层次结构中
        // 或反之亦然
        Component c = modalDialog;
        while (c != null) {
            if (c == anotherDialog) {
                return 1;
            }
            c = c.getParent_NoClientCode();
        }
        c = anotherDialog;
        while (c != null) {
            if (c == modalDialog) {
                return -1;
            }
            c = c.getParent_NoClientCode();
        }
        // 检查一个对话框是否直接或间接地阻止另一个对话框
        Dialog blocker = modalDialog.getModalBlocker();
        while (blocker != null) {
            if (blocker == anotherDialog) {
                return -1;
            }
            blocker = blocker.getModalBlocker();
        }
        blocker = anotherDialog.getModalBlocker();
        while (blocker != null) {
            if (blocker == modalDialog) {
                return 1;
            }
            blocker = blocker.getModalBlocker();
        }
        // 比较模态类型
        return modalDialog.getModalityType().compareTo(anotherDialog.getModalityType());
    }

    static ModalEventFilter createFilterForDialog(Dialog modalDialog) {
        switch (modalDialog.getModalityType()) {
            case DOCUMENT_MODAL: return new DocumentModalEventFilter(modalDialog);
            case APPLICATION_MODAL: return new ApplicationModalEventFilter(modalDialog);
            case TOOLKIT_MODAL: return new ToolkitModalEventFilter(modalDialog);
        }
        return null;
    }

    private static class ToolkitModalEventFilter extends ModalEventFilter {

        private AppContext appContext;

        ToolkitModalEventFilter(Dialog modalDialog) {
            super(modalDialog);
            appContext = modalDialog.appContext;
        }

        protected FilterAction acceptWindow(Window w) {
            if (w.isModalExcluded(Dialog.ModalExclusionType.TOOLKIT_EXCLUDE)) {
                return FilterAction.ACCEPT;
            }
            if (w.appContext != appContext) {
                return FilterAction.REJECT;
            }
            while (w != null) {
                if (w == modalDialog) {
                    return FilterAction.ACCEPT_IMMEDIATELY;
                }
                w = w.getOwner();
            }
            return FilterAction.REJECT;
        }
    }

    private static class ApplicationModalEventFilter extends ModalEventFilter {

        private AppContext appContext;

        ApplicationModalEventFilter(Dialog modalDialog) {
            super(modalDialog);
            appContext = modalDialog.appContext;
        }

        protected FilterAction acceptWindow(Window w) {
            if (w.isModalExcluded(Dialog.ModalExclusionType.APPLICATION_EXCLUDE)) {
                return FilterAction.ACCEPT;
            }
            if (w.appContext == appContext) {
                while (w != null) {
                    if (w == modalDialog) {
                        return FilterAction.ACCEPT_IMMEDIATELY;
                    }
                    w = w.getOwner();
                }
                return FilterAction.REJECT;
            }
            return FilterAction.ACCEPT;
        }
    }

    private static class DocumentModalEventFilter extends ModalEventFilter {

        private Window documentRoot;

        DocumentModalEventFilter(Dialog modalDialog) {
            super(modalDialog);
            documentRoot = modalDialog.getDocumentRoot();
        }

        protected FilterAction acceptWindow(Window w) {
            // 应用程序和工具包排除的窗口被其子层次结构中的文档模态对话框阻止
            if (w.isModalExcluded(Dialog.ModalExclusionType.APPLICATION_EXCLUDE)) {
                Window w1 = modalDialog.getOwner();
                while (w1 != null) {
                    if (w1 == w) {
                        return FilterAction.REJECT;
                    }
                    w1 = w1.getOwner();
                }
                return FilterAction.ACCEPT;
            }
            while (w != null) {
                if (w == modalDialog) {
                    return FilterAction.ACCEPT_IMMEDIATELY;
                }
                if (w == documentRoot) {
                    return FilterAction.REJECT;
                }
                w = w.getOwner();
            }
            return FilterAction.ACCEPT;
        }
    }
}
