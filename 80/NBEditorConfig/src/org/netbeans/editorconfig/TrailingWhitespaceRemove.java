package org.netbeans.editorconfig;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.text.Document;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.editor.settings.SimpleValueNames;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.editor.indent.api.IndentUtils;
import org.netbeans.modules.editor.lib2.document.ModRootElement;
import org.netbeans.modules.editor.lib2.document.TrailingWhitespaceRemoveProcessor;
import org.netbeans.spi.editor.document.OnSaveTask;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.NbPreferences;

/**
 * Removal of trailing whitespace per file on save
 *
 */
public final class TrailingWhitespaceRemove implements OnSaveTask {

    // -J-Dorg.netbeans.modules.editor.lib.TrailingWhitespaceRemove.level=FINE
    static final Logger LOG = Logger.getLogger(TrailingWhitespaceRemove.class.getName());

    private final Document doc;

    private AtomicBoolean canceled = new AtomicBoolean();

    TrailingWhitespaceRemove(Document doc) {
        this.doc = doc;
    }

    @Override
    public void performTask() {
        Preferences prefs = null;
        FileObject fo = findFileObject(doc);
        if (fo != null) {
            Project p = FileOwnerQuery.getOwner(fo);
            if (p != null) {
                String foName = fo.getName();
                String projectName = p.getProjectDirectory().getName();
                prefs = NbPreferences.forModule(IndentUtils.class).node(projectName).node(foName);
            }
        }
        String policy = prefs.get(SimpleValueNames.ON_SAVE_REMOVE_TRAILING_WHITESPACE, "never"); //NOI18N
        if (!"never".equals(policy)) { //NOI18N
            ModRootElement modRootElement = ModRootElement.get(doc);
            if (modRootElement != null) {
                boolean origEnabled = modRootElement.isEnabled();
                modRootElement.setEnabled(false);
                try {
                    new TrailingWhitespaceRemoveProcessor(doc, "modified-lines".equals(policy), canceled).removeWhitespace(); //NOI18N
                } finally {
                    modRootElement.setEnabled(origEnabled);
                }
            }
        }
    }

    private static FileObject findFileObject(Document doc) {
        if (doc != null) {
            Object sdp = doc.getProperty(Document.StreamDescriptionProperty);
            if (sdp instanceof DataObject) {
                return ((DataObject) sdp).getPrimaryFile();
            } else if (sdp instanceof FileObject) {
                return (FileObject) sdp;
            }
        }
        return null;
    }

    @Override
    public void runLocked(Runnable run) {
        run.run();
    }

    @Override
    public boolean cancel() {
        canceled.set(true);
        return true;
    }

    @MimeRegistration(mimeType = "", service = OnSaveTask.Factory.class, position = 1000)
    public static final class FactoryImpl implements Factory {

        @Override
        public OnSaveTask createTask(Context context) {
            return new TrailingWhitespaceRemove(context.getDocument());
        }

    }

}
