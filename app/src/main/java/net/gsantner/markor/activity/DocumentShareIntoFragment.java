/*
 * Copyright (c) 2017-2018 Gregor Santner
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;

import net.gsantner.markor.R;
import net.gsantner.markor.format.converter.MarkdownTextConverter;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.FilesystemDialogCreator;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.DocumentIO;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.activity.GsFragmentBase;
import net.gsantner.opoc.format.todotxt.SttCommander;
import net.gsantner.opoc.preference.GsPreferenceFragmentCompat;
import net.gsantner.opoc.preference.SharedPreferencesPropertyBackend;
import net.gsantner.opoc.ui.FilesystemDialogData;

import java.io.File;

import butterknife.BindView;

public class DocumentShareIntoFragment extends GsFragmentBase {
    public static final String FRAGMENT_TAG = "DocumentShareIntoFragment";
    public static final String EXTRA_SHARED_TEXT = "EXTRA_SHARED_TEXT";

    public static DocumentShareIntoFragment newInstance(String sharedText) {
        DocumentShareIntoFragment f = new DocumentShareIntoFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_SHARED_TEXT, sharedText);
        f.setArguments(args);
        return f;
    }

    @BindView(R.id.document__fragment__share_into__webview)
    WebView _webView;

    private String _sharedText;

    public DocumentShareIntoFragment() {
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.document__fragment__share_into;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        AppSettings as = new AppSettings(view.getContext());
        ContextUtils cu = new ContextUtils(view.getContext());
        cu.setAppLanguage(as.getLanguage());
        _sharedText = getArguments() != null ? getArguments().getString(EXTRA_SHARED_TEXT, "") : "";
        _sharedText = _sharedText.trim();

        FragmentTransaction t = getChildFragmentManager().beginTransaction();
        t.replace(R.id.something, new ShareIntoImportOptions(), "lol").commit();

        Document document = new Document();
        document.setContent(_sharedText);
        new MarkdownTextConverter().convertMarkupShowInWebView(document, _webView);
    }

    @Override
    public String getFragmentTag() {
        return FRAGMENT_TAG;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }


    public static class ShareIntoImportOptions extends GsPreferenceFragmentCompat {
        public static final String TAG = "ShareIntoImportOptions";

        private String _sharedText = "hello";

        @Override
        public int getPreferenceResourceForInflation() {
            return R.xml.preference_actions_share_into;
        }

        @Override
        public String getFragmentTag() {
            return TAG;
        }

        @Override
        protected SharedPreferencesPropertyBackend getAppSettings(Context context) {
            return new AppSettings(context);
        }

        @Override
        public Integer getIconTintColor() {
            boolean dark = ((AppSettings) getAppSettings(getContext())).isDarkThemeEnabled();
            return _cu.rcolor(dark ? R.color.dark__primary_text : R.color.light__primary_text);
        }

        private void appendToExistingDocument(File file, boolean showEditor) {
            Bundle args = new Bundle();
            args.putSerializable(DocumentIO.EXTRA_PATH, file);
            args.putBoolean(DocumentIO.EXTRA_PATH_IS_FOLDER, false);
            Document document = DocumentIO.loadDocument(getContext(), args, null);
            String currentContent = TextUtils.isEmpty(document.getContent()) ? "" : (document.getContent().trim() + "\n");
            DocumentIO.saveDocument(document, false, currentContent + _sharedText);
            if (showEditor) {
                showInDocumentActivity(document);
            }
        }

        private void showAppendDialog() {
            FilesystemDialogCreator.showFileDialog(new FilesystemDialogData.SelectionListenerAdapter() {
                @Override
                public void onFsDialogConfig(FilesystemDialogData.Options opt) {
                    opt.rootFolder = AppSettings.get().getNotebookDirectory();
                }

                @Override
                public void onFsSelected(String request, File file) {
                    appendToExistingDocument(file, true);
                }

            }, getFragmentManager(), getActivity());
        }


        private void createNewDocument() {
            // Create a new document
            Bundle args = new Bundle();
            args.putSerializable(DocumentIO.EXTRA_PATH, AppSettings.get().getNotebookDirectory());
            args.putBoolean(DocumentIO.EXTRA_PATH_IS_FOLDER, true);
            Document document = DocumentIO.loadDocument(getContext(), args, null);
            DocumentIO.saveDocument(document, false, _sharedText);

            // Load document as file
            args.putSerializable(DocumentIO.EXTRA_PATH, document.getFile());
            args.putBoolean(DocumentIO.EXTRA_PATH_IS_FOLDER, false);
            document = DocumentIO.loadDocument(getContext(), args, null);
            document.setTitle("");
            showInDocumentActivity(document);
        }

        private void showInDocumentActivity(Document document) {
            if (getActivity() instanceof DocumentActivity) {
                DocumentActivity a = (DocumentActivity) getActivity();
                a.setDocument(document);
                if (AppSettings.get().isPreviewFirst()) {
                    a.showPreview(document, null);
                } else {
                    a.showTextEditor(document, null, false);
                }
            }
        }

        @Override
        @SuppressWarnings({"ConstantConditions", "ConstantIfStatement", "StatementWithEmptyBody"})
        public Boolean onPreferenceClicked(Preference preference) {
            AppSettings appSettings = new AppSettings(getActivity().getApplicationContext());
            PermissionChecker permc = new PermissionChecker(getActivity());
            if (isAdded() && preference.hasKey()) {
                String key = preference.getKey();
                if (false) {
                } else if (eq(key, R.string.pref_key__share_into__clipboard)) {
                    new ShareUtil(getContext()).setClipboard(_sharedText);
                    getActivity().finish();
                } else if (eq(key, R.string.pref_key__share_into__create_document)) {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        createNewDocument();
                    }
                } else if (eq(key, R.string.pref_key__share_into__existing_document)) {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        showAppendDialog();
                    }
                } else if (eq(key, R.string.pref_key__share_into__quicknote)) {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        appendToExistingDocument(AppSettings.get().getQuickNoteFile(), false);
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    }
                } else if (eq(key, R.string.pref_key__share_into__todo)) {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        if (appSettings.isTodoStartTasksWithTodaysDateEnabled()) {
                            String today = SttCommander.getToday() + " ";
                            if (!_sharedText.startsWith(today)) {
                                _sharedText = today + _sharedText;
                            }
                        }
                        appendToExistingDocument(AppSettings.get().getTodoFile(), false);
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    }
                }
            }
            return null;
        }
    }
}
