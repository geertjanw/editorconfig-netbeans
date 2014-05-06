package org.netbeans.editorconfig;
import java.util.prefs.Preferences;
import javax.swing.text.Document;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.editor.indent.api.IndentUtils;
import org.netbeans.modules.editor.indent.spi.CodeStylePreferences;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
@ServiceProvider(service = CodeStylePreferences.Provider.class, position = 1)
public final class FileCodeStylePreferences implements CodeStylePreferences.Provider {
    @Override
    public Preferences forFile(FileObject fo, String string) {
        return singleton.forFile(fo, string);
    }
    @Override
    public Preferences forDocument(Document dcmnt, String string) {
        return singleton.forDocument(dcmnt, string);
    }
    private static final CodeStylePreferences.Provider singleton = new CodeStylePreferences.Provider() {
        @Override
        public Preferences forDocument(Document doc, String mimeType) {
            Preferences node = null;
            FileObject fo = findFileObject(doc);
            if (fo != null) {
                Project p = FileOwnerQuery.getOwner(fo);
                if (p != null) {
                    String foName = fo.getName();
                    String projectName = p.getProjectDirectory().getName();
                    node = NbPreferences.forModule(IndentUtils.class).node(projectName).node(foName);
                }
            }
            return node;
        }
        @Override
        public Preferences forFile(FileObject fo, String mimeType) {
            //not used:
            return null;
        }
    };
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
}
