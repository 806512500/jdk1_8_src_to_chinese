/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

import java.util.Arrays;
import static java.lang.invoke.LambdaForm.*;
import static java.lang.invoke.MethodHandleStatics.*;

/**
 * 一个由目标确定调用行为的方法句柄。
 * 委托的方法句柄本身可以持有超出简单行为的额外“意图”。
 * @author jrose
 */
/*non-public*/
abstract class DelegatingMethodHandle extends MethodHandle {
    protected DelegatingMethodHandle(MethodHandle target) {
        this(target.type(), target);
    }

    protected DelegatingMethodHandle(MethodType type, MethodHandle target) {
        super(type, chooseDelegatingForm(target));
    }

    protected DelegatingMethodHandle(MethodType type, LambdaForm form) {
        super(type, form);
    }

    /** 定义此方法以提取提供调用行为的委托目标。 */
    abstract protected MethodHandle getTarget();

    @Override
    abstract MethodHandle asTypeUncached(MethodType newType);

    @Override
    MemberName internalMemberName() {
        return getTarget().internalMemberName();
    }

    @Override
    boolean isInvokeSpecial() {
        return getTarget().isInvokeSpecial();
    }

    @Override
    Class<?> internalCallerClass() {
        return getTarget().internalCallerClass();
    }

    @Override
    MethodHandle copyWith(MethodType mt, LambdaForm lf) {
        // FIXME: 重新考虑 'copyWith' 协议；它对于所有方法句柄的使用来说太底层了
        throw newIllegalArgumentException("do not use this");
    }

    @Override
    String internalProperties() {
        return "\n& Class="+getClass().getSimpleName()+
               "\n& Target="+getTarget().debugString();
    }

    @Override
    BoundMethodHandle rebind() {
        return getTarget().rebind();
    }

    private static LambdaForm chooseDelegatingForm(MethodHandle target) {
        if (target instanceof SimpleMethodHandle)
            return target.internalForm();  // 无需间接调用
        return makeReinvokerForm(target, MethodTypeForm.LF_DELEGATE, DelegatingMethodHandle.class, NF_getTarget);
    }

    static LambdaForm makeReinvokerForm(MethodHandle target,
                                        int whichCache,
                                        Object constraint,
                                        NamedFunction getTargetFn) {
        String debugString;
        switch(whichCache) {
            case MethodTypeForm.LF_REBIND:            debugString = "BMH.reinvoke";      break;
            case MethodTypeForm.LF_DELEGATE:          debugString = "MH.delegate";       break;
            default:                                  debugString = "MH.reinvoke";       break;
        }
        // 无需预操作。
        return makeReinvokerForm(target, whichCache, constraint, debugString, true, getTargetFn, null);
    }
    /** 创建一个简单重新调用给定基本类型目标的 LF。 */
    static LambdaForm makeReinvokerForm(MethodHandle target,
                                        int whichCache,
                                        Object constraint,
                                        String debugString,
                                        boolean forceInline,
                                        NamedFunction getTargetFn,
                                        NamedFunction preActionFn) {
        MethodType mtype = target.type().basicType();
        boolean customized = (whichCache < 0 ||
                mtype.parameterSlotCount() > MethodType.MAX_MH_INVOKER_ARITY);
        boolean hasPreAction = (preActionFn != null);
        LambdaForm form;
        if (!customized) {
            form = mtype.form().cachedLambdaForm(whichCache);
            if (form != null)  return form;
        }
        final int THIS_DMH    = 0;
        final int ARG_BASE    = 1;
        final int ARG_LIMIT   = ARG_BASE + mtype.parameterCount();
        int nameCursor = ARG_LIMIT;
        final int PRE_ACTION   = hasPreAction ? nameCursor++ : -1;
        final int NEXT_MH     = customized ? -1 : nameCursor++;
        final int REINVOKE    = nameCursor++;
        LambdaForm.Name[] names = LambdaForm.arguments(nameCursor - ARG_LIMIT, mtype.invokerType());
        assert(names.length == nameCursor);
        names[THIS_DMH] = names[THIS_DMH].withConstraint(constraint);
        Object[] targetArgs;
        if (hasPreAction) {
            names[PRE_ACTION] = new LambdaForm.Name(preActionFn, names[THIS_DMH]);
        }
        if (customized) {
            targetArgs = Arrays.copyOfRange(names, ARG_BASE, ARG_LIMIT, Object[].class);
            names[REINVOKE] = new LambdaForm.Name(target, targetArgs);  // 调用者是目标本身
        } else {
            names[NEXT_MH] = new LambdaForm.Name(getTargetFn, names[THIS_DMH]);
            targetArgs = Arrays.copyOfRange(names, THIS_DMH, ARG_LIMIT, Object[].class);
            targetArgs[0] = names[NEXT_MH];  // 覆盖此方法句柄为下一个方法句柄
            names[REINVOKE] = new LambdaForm.Name(mtype, targetArgs);
        }
        form = new LambdaForm(debugString, ARG_LIMIT, names, forceInline);
        if (!customized) {
            form = mtype.form().setCachedLambdaForm(whichCache, form);
        }
        return form;
    }

    static final NamedFunction NF_getTarget;
    static {
        try {
            NF_getTarget = new NamedFunction(DelegatingMethodHandle.class
                                             .getDeclaredMethod("getTarget"));
        } catch (ReflectiveOperationException ex) {
            throw newInternalError(ex);
        }
    }
}
