package com.millennialmedia.intellibot.psi.element;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.PyClass;
import com.millennialmedia.intellibot.psi.RobotFeatureFileType;
import com.millennialmedia.intellibot.psi.RobotLanguage;
import com.millennialmedia.intellibot.psi.ref.PythonResolver;
import com.millennialmedia.intellibot.psi.ref.RobotPythonClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author Stephen Abrams
 */
public class RobotFileImpl extends PsiFileBase implements RobotFile, KeywordFile {

    private Collection<DefinedKeyword> defiedKeywords;
    private Collection<KeywordFile> keywordFiles;

    public RobotFileImpl(FileViewProvider viewProvider) {
        super(viewProvider, RobotLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return RobotFeatureFileType.getInstance();
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        this.defiedKeywords = null;
        this.keywordFiles = null;
    }

    @NotNull
    @Override
    public Collection<PsiElement> getInvokedKeywords() {
        Collection<PsiElement> results = new HashSet<PsiElement>();
        for (PsiElement child : getChildren()) {
            if (child instanceof Heading) {
                if (((Heading) child).containsTestCases() || ((Heading) child).containsKeywordDefinitions()) {
                    for (PsiElement headingChild : child.getChildren()) {
                        if (headingChild instanceof KeywordDefinition) {
                            for (PsiElement definitionChild : headingChild.getChildren()) {
                                if (definitionChild instanceof KeywordStatement) {
                                    for (PsiElement statementChild : definitionChild.getChildren()) {
                                        if (statementChild instanceof KeywordInvokable) {
                                            PsiReference reference = statementChild.getReference();
                                            if (reference != null) {
                                                PsiElement resolved = reference.resolve();
                                                if (resolved != null) {
                                                    results.add(resolved);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return results;
    }

    @NotNull
    @Override
    public Collection<DefinedKeyword> getKeywords() {
        Collection<DefinedKeyword> results = this.defiedKeywords;
        if (results == null) {
            results = collectKeywords();
            this.defiedKeywords = results;
        }
        return results;
    }

    @NotNull
    private Collection<DefinedKeyword> collectKeywords() {
        List<DefinedKeyword> result = new ArrayList<DefinedKeyword>();
        for (PsiElement child : getChildren()) {
            if (child instanceof Heading && ((Heading) child).containsKeywordDefinitions()) {
                for (PsiElement headingChild : child.getChildren()) {
                    if (headingChild instanceof DefinedKeyword)
                        result.add(((DefinedKeyword) headingChild));
                }
            }
        }
        return result;
    }

    @Override
    @NotNull
    public Collection<KeywordFile> getImportedFiles() {
        Collection<KeywordFile> results = this.keywordFiles;
        if (results == null) {
            results = collectImportFiles();
            this.keywordFiles = results;
        }
        return results;
    }

    private Collection<KeywordFile> collectImportFiles() {
        List<KeywordFile> files = new ArrayList<KeywordFile>();
        for (PsiElement child : getChildren()) {
            if (child instanceof Heading && ((Heading) child).containsImports()) {
                for (PsiElement headingChild : child.getChildren()) {
                    if (headingChild instanceof Import) {
                        Import imp = (Import) headingChild;
                        if (imp.isResource()) {
                            Argument argument = PsiTreeUtil.findChildOfType(imp, Argument.class);
                            if (argument != null) {
                                PsiElement resolution = resolveImport(argument);
                                if (resolution instanceof KeywordFile) {
                                    files.add((KeywordFile) resolution);
                                }
                            }
                        } else if (imp.isLibrary()) {
                            Argument argument = PsiTreeUtil.findChildOfType(imp, Argument.class);
                            if (argument != null) {
                                PyClass resolution = PythonResolver.cast(resolveImport(argument));
                                if (resolution != null) {
                                    files.add(new RobotPythonClass(argument.getPresentableText(), resolution));
                                }
                            }
                        }
                    }
                }
            }
        }
        return files;
    }

    @Nullable
    private PsiElement resolveImport(@NotNull Argument argument) {
        PsiReference reference = argument.getReference();
        if (reference != null) {
            return reference.resolve();
        }
        return null;
    }
}
