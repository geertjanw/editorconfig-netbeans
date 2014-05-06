package org.netbeans.editorconfig;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.editorconfig.core.EditorConfig;
import org.editorconfig.core.EditorConfigException;
import org.editorconfig.core.PythonException;
import org.netbeans.api.editor.settings.SimpleValueNames;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.editor.indent.api.IndentUtils;
import org.netbeans.spi.project.LookupProvider;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.Lookups;
@LookupProvider.Registration(projectType = {
    "org-netbeans-modules-java-j2seproject",
    "org-netbeans-modules-web-project",
    "org.netbeans.modules.web.clientproject",
    "org-netbeans-modules-web-clientproject",
    "org-netbeans-modules-maven",
    "org-netbeans-modules-apisupport-project"}
)
public class EditorConfigProjectOpenedHook implements LookupProvider {
    @Override
    public Lookup createAdditionalLookup(Lookup lookup) {
        final Project p = lookup.lookup(Project.class);
        return Lookups.fixed(new ProjectOpenedHook() {
            @Override
            protected void projectOpened() {
                FileObject projFo = p.getProjectDirectory();
                projFo.addRecursiveListener(new FileChangeAdapter() {
                    @Override
                    public void fileDataCreated(FileEvent fe) {
                        FileObject file = fe.getFile();
                        DataObject dobj;
                        try {
                            dobj = DataObject.find(file);
                            final DataFolder dof = dobj.getFolder();
                            if (file.getNameExt().equals(".editorconfig")) {
                                file.addFileChangeListener(new FileChangeAdapter() {
                                    @Override
                                    public void fileChanged(FileEvent fe) {
                                        applyEditorConfigToFolder(dof);
                                    }
                                });
                                applyEditorConfigToFolder(dof);
                            } else {
                                // if it isn't an editorconfig that's been added,
                                // apply the editorconfig file to it:
                                applyEditorConfigToFile(dobj);
                            }
                        } catch (DataObjectNotFoundException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                    private void applyEditorConfigToFolder(DataFolder dof) {
                        for (DataObject dobj : dof.getChildren()) {
                            applyEditorConfigToFile(dobj);
                        }
                    }
                    private void applyEditorConfigToFile(DataObject dobj) throws NumberFormatException {
                        EditorConfig ec;
                        try {
                            ec = new EditorConfig();
                            List<EditorConfig.OutPair> l = null;
                            l = ec.getProperties(dobj.getPrimaryFile().getPath());
                            for (int i = 0; i < l.size(); ++i) {
                                if (l.get(i).getKey().equals("indent_size")) {
                                    doIndentSize(
                                            dobj.getPrimaryFile(),
                                            Integer.valueOf(l.get(i).getVal()));
                                } else if (l.get(i).getKey().equals("max_line_length")) {
                                    doTextLimitWidth(
                                            dobj.getPrimaryFile(),
                                            Integer.valueOf(l.get(i).getVal()));
                                } else if (l.get(i).getKey().equals("indent_style")) {
                                    doIndentStyle(
                                            dobj.getPrimaryFile(),
                                            l.get(i).getVal());
                                } else if (l.get(i).getKey().equals("trim_trailing_whitespace")) {
                                    doTrimTrailingWhitespace(
                                            dobj.getPrimaryFile(),
                                            l.get(i).getVal());
                                }
                            }
                        } catch (PythonException ex) {
                            Exceptions.printStackTrace(ex);
                        } catch (EditorConfigException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                    public static final String indentSize = SimpleValueNames.INDENT_SHIFT_WIDTH;
                    public static final String textLimitWidth = SimpleValueNames.TEXT_LIMIT_WIDTH;
                    public static final String expandTabs = SimpleValueNames.EXPAND_TABS;
                    public static final String trailingWhitespace = SimpleValueNames.ON_SAVE_REMOVE_TRAILING_WHITESPACE;
                    private void doIndentSize(FileObject file, int value) {
                        Project p = FileOwnerQuery.getOwner(file);
                        String pName = p.getProjectDirectory().getName();
                        Preferences node = NbPreferences.forModule(IndentUtils.class).node(pName).node(file.getName());
                        node.putInt(indentSize, value);
                        try {
                            node.flush();
                        } catch (BackingStoreException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                    private void doTextLimitWidth(FileObject file, int value) {
                        Project p = FileOwnerQuery.getOwner(file);
                        String pName = p.getProjectDirectory().getName();
                        Preferences node = NbPreferences.forModule(IndentUtils.class).node(pName).node(file.getName());
                        node.putInt(textLimitWidth, value);
                        try {
                            node.flush();
                        } catch (BackingStoreException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                    private void doTrimTrailingWhitespace(FileObject file, String value) {
                        Project p = FileOwnerQuery.getOwner(file);
                        String pName = p.getProjectDirectory().getName();
                        Preferences node = NbPreferences.forModule(IndentUtils.class).node(pName).node(file.getName());
                        if (value.equals("true")) {
                            node.put(trailingWhitespace, "always");
                        } else {
                            node.put(trailingWhitespace, "never");
                        }
                        try {
                            node.flush();
                        } catch (BackingStoreException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                    private void doIndentStyle(FileObject file, String value) {
                        Project p = FileOwnerQuery.getOwner(file);
                        String pName = p.getProjectDirectory().getName();
                        Preferences node = NbPreferences.forModule(IndentUtils.class).node(pName).node(file.getName());
			if (value.equals("tab")) {
			    node.putBoolean(expandTabs, false);
			} else {
			    node.putBoolean(expandTabs, true);
			}
                        try {
                            node.flush();
                        } catch (BackingStoreException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                });
            }
            @Override
            protected void projectClosed() {
            }
        });
    }
}
