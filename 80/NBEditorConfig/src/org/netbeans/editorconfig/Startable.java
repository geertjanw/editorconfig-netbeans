package org.netbeans.editorconfig;

import org.openide.modules.OnStart;
import org.openide.util.NbPreferences;

@OnStart
public class Startable implements Runnable {

    @Override
    public void run() {
        NbPreferences.root().node("org").node("netbeans").node("core").put("IgnoredFiles", "");
    }
    
}
