// Copyright 2024 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.pure.ide.interpreted.debug;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.OutputWriter;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.statelistener.VoidExecutionActivityListener;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;

public class FunctionExecutionInterpretedWithDebugSupport extends FunctionExecutionInterpreted
{
    private volatile CompletableFuture<CoreInstance> currentExecution;
    private volatile CompletableFuture<CoreInstance> resultHandler;
    private volatile DebugState debugState;

    public FunctionExecutionInterpretedWithDebugSupport()
    {
        super(VoidExecutionActivityListener.VOID_EXECUTION_ACTIVITY_LISTENER);
    }

    public DebugState getDebugState()
    {
        return this.debugState;
    }

    public void setDebugState(DebugState debugState)
    {
        if (debugState != null && this.debugState != null)
        {
            throw new IllegalStateException("Debug session already exists?");
        }

        this.debugState = debugState;

        if (debugState != null)
        {
            this.getConsole().print("Entering debug mode.  Use terminal to introspect debug state.  F9 to continue execution.\n\nDebug summary:\n");
            this.getConsole().print(debugState.getSummary());
            this.resultHandler.complete(null);
        }
    }

    @Override
    public CoreInstance start(CoreInstance function, ListIterable<? extends CoreInstance> arguments)
    {
        this.resultHandler = new CompletableFuture<>();

        if (this.currentExecution == null)
        {
            this.currentExecution = CompletableFuture.supplyAsync(() -> this.startRaw(function, arguments));
            this.currentExecution.whenComplete((v, e) ->
            {
                if (e != null)
                {
                    this.resultHandler.completeExceptionally(e);
                }
                else
                {
                    this.resultHandler.complete(v);
                }
                this.currentExecution = null;
            });
        }
        else
        {
            this.debugState.release();
        }

        try
        {
            return this.resultHandler.join();
        }
        catch (CompletionException e)
        {
            throw (RuntimeException) e.getCause();
        }
    }

    public CoreInstance startRaw(CoreInstance function, ListIterable<? extends CoreInstance> arguments)
    {
        return super.start(function, arguments);
    }

    @Override
    public void start(CoreInstance func, ListIterable<? extends CoreInstance> arguments, OutputStream
            outputStream, OutputWriter writer)
    {
        CoreInstance result = this.startRaw(func, arguments);

        try
        {
            ListIterable<? extends CoreInstance> values = result.getValueForMetaPropertyToMany(M3Properties.values);
            writer.write(values, outputStream);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Failed to write to output stream", e);
        }
    }
}