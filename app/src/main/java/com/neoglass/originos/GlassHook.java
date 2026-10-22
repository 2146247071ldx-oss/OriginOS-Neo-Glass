
package com.neoglass.originos;

import android.util.Log;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public class GlassHook extends XposedModule {

    private static final String TAG = "OriginBreaker";

    @Override
    public void onModuleLoaded(
            XposedModuleInterface.ModuleLoadedParam param) {
        super.onModuleLoaded(param);

        String processName = param.getProcessName();

        log(
                Log.INFO,
                TAG,
                "Module loaded. API="
                        + getApiVersion()
                        + ", process="
                        + processName
        );
    }
}
