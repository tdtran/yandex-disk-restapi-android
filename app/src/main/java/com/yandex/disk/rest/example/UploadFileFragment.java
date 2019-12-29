/*
* (C) 2015 Yandex LLC (https://yandex.com/)
*
* The source code of Java SDK for Yandex.Disk REST API
* is available to use under terms of Apache License,
* Version 2.0. See the file LICENSE for the details.
*/

package com.yandex.disk.rest.example;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.yandex.disk.rest.ProgressListener;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.CancelledUploadingException;
import com.yandex.disk.rest.exceptions.ServerException;
import com.yandex.disk.rest.exceptions.http.HttpCodeException;
import com.yandex.disk.rest.json.Link;

import java.io.File;
import java.io.IOException;

import androidx.fragment.app.FragmentManager;

public class UploadFileFragment extends IODialogFragment {

    private static final String TAG = "LoadFileFragment";

    private static final String WORK_FRAGMENT_TAG = "UploadFileFragment.Background";

    private static final String CREDENTIALS = "example.credentials";
    private static final String SERVER_PATH = "example.server.path";
    private static final String LOCAL_FILE = "example.local.file";

    private static final int PROGRESS_DIV = 1024 * 1024;

    private Credentials credentials;
    private String serverPath, localFile;

    private UploadFileRetainedFragment workFragment;

    public static UploadFileFragment newInstance(Credentials credentials, String serverPath, String localFile) {
        UploadFileFragment fragment = new UploadFileFragment();

        Bundle args = new Bundle();
        args.putParcelable(CREDENTIALS, credentials);
        args.putString(SERVER_PATH, serverPath);
        args.putString(LOCAL_FILE, localFile);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        credentials = getArguments().getParcelable(CREDENTIALS);
        serverPath = getArguments().getString(SERVER_PATH);
        localFile = getArguments().getString(LOCAL_FILE);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentManager fragmentManager = getFragmentManager();
        workFragment = (UploadFileRetainedFragment) fragmentManager.findFragmentByTag(WORK_FRAGMENT_TAG);
        if (workFragment == null || workFragment.getTargetFragment() == null) {
            workFragment = new UploadFileRetainedFragment();
            fragmentManager.beginTransaction().add(workFragment, WORK_FRAGMENT_TAG).commit();
            workFragment.uploadFile(credentials, serverPath, localFile);
        }
        workFragment.setTargetFragment(this, 0);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (workFragment != null) {
            workFragment.setTargetFragment(null, 0);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        dialog = new ProgressDialog(getActivity());
        dialog.setTitle(R.string.example_uploading_file_title);
        dialog.setMessage(localFile);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setIndeterminate(true);
        dialog.setButton(ProgressDialog.BUTTON_NEUTRAL, getString(R.string.example_loading_file_cancel_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick (DialogInterface dialog, int which) {
                dialog.dismiss();
                onCancel();
            }
        });
        dialog.setOnCancelListener(this);
        dialog.show();
        return dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        onCancel();
    }

    private void onCancel() {
        workFragment.cancelUpload();
    }

    public void onUploadComplete() {
        dialog.dismiss();
        Toast.makeText(getActivity(), R.string.example_file_uploaded_toast, Toast.LENGTH_LONG).show();
    }

    public void setDownloadProgress(long loaded, long total) {
        if (dialog != null) {
            if (dialog.isIndeterminate()) {
                dialog.setIndeterminate(false);
            }
            if (total > Integer.MAX_VALUE) {
                dialog.setProgress((int)(loaded / PROGRESS_DIV));
                dialog.setMax((int)(total / PROGRESS_DIV));
            } else {
                dialog.setProgress((int)loaded);
                dialog.setMax((int)total);
            }
        }
    }

    public static class UploadFileRetainedFragment extends IODialogRetainedFragment implements ProgressListener {

        private boolean cancelled;

        public void uploadFile(final Credentials credentials, final String serverPath, final String localFile) {

            new Thread(new Runnable() {
                @Override
                public void run () {
                    try {
                        RestClient client = RestClientUtil.getInstance(credentials);
                        Link link = client.getUploadLink(serverPath, true);
                        client.uploadFile(link, true, new File(localFile), UploadFileRetainedFragment.this);
                        uploadComplete();
                    } catch (CancelledUploadingException ex) {
                        // cancelled by user
                    } catch (HttpCodeException ex) {
                        Log.d(TAG, "uploadFile", ex);
                        sendException(ex.getResponse().getDescription());
                    } catch (IOException | ServerException ex) {
                        Log.d(TAG, "uploadFile", ex);
                        sendException(ex);
                    }
                }
            }).start();
        }

        @Override
        public void updateProgress (final long loaded, final long total) {
            handler.post(new Runnable() {
                @Override
                public void run () {
                    UploadFileFragment targetFragment = (UploadFileFragment) getTargetFragment();
                    if (targetFragment != null) {
                        targetFragment.setDownloadProgress(loaded, total);
                    }
                }
            });
        }

        @Override
        public boolean hasCancelled () {
            return cancelled;
        }

        public void uploadComplete() {
            handler.post(new Runnable() {
                @Override
                public void run () {
                    UploadFileFragment targetFragment = (UploadFileFragment) getTargetFragment();
                    if (targetFragment != null) {
                        targetFragment.onUploadComplete();
                    }
                }
            });
        }

        public void cancelUpload() {
            cancelled = true;
        }
    }
}
