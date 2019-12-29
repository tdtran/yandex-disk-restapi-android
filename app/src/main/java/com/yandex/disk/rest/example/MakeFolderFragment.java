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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerException;
import com.yandex.disk.rest.exceptions.http.HttpCodeException;

import java.io.IOException;

import androidx.fragment.app.FragmentManager;

public class MakeFolderFragment extends IODialogFragment {

    private static final String TAG = "MakeFolderFragment";

    private static final String WORK_FRAGMENT_TAG = "MakeFolderFragment.Background";

    private static final String MKDIR_PATH = "example.mkdir.path";
    private static final String MKDIR_NAME = "example.mkdir.name";

    private Credentials credentials;
    private String path, name;

    private MakeFolderRetainedFragment workFragment;

    public static MakeFolderFragment newInstance(Credentials credentials, String path, String name) {
        MakeFolderFragment fragment = new MakeFolderFragment();

        Bundle args = new Bundle();
        args.putParcelable(CREDENTIALS, credentials);
        args.putString(MKDIR_PATH, path);
        args.putString(MKDIR_NAME, name);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        credentials = getArguments().getParcelable(CREDENTIALS);
        path = getArguments().getString(MKDIR_PATH);
        name = getArguments().getString(MKDIR_NAME);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentManager fragmentManager = getFragmentManager();
        workFragment = (MakeFolderRetainedFragment) fragmentManager.findFragmentByTag(WORK_FRAGMENT_TAG);
        if (workFragment == null || workFragment.getTargetFragment() == null) {
            workFragment = new MakeFolderRetainedFragment();
            fragmentManager.beginTransaction().add(workFragment, WORK_FRAGMENT_TAG).commit();
            workFragment.makeFolder(credentials, path+"/"+name);
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
        dialog.setTitle(R.string.example_progress_mkfolder_title);
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setButton(ProgressDialog.BUTTON_NEUTRAL, getText(R.string.example_make_folder_negative_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick (DialogInterface dialog, int which) {
                dialog.dismiss();
                onCancel();
            }
        });
        return dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        onCancel();
    }

    private void onCancel() {
    }

    public void onComplete() {
        dialog.dismiss();
        Toast.makeText(getActivity(), R.string.example_progress_mkfolder_done, Toast.LENGTH_LONG).show();
        ((ExampleActivity) getActivity()).reloadContent();
    }

    public static class MakeFolderRetainedFragment extends IODialogRetainedFragment {

        public void makeFolder(final Credentials credentials, final String path) {

            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        RestClient client = RestClientUtil.getInstance(credentials);
                        client.makeFolder(path);
                    } catch (HttpCodeException ex) {
                        Log.d(TAG, "makeFolder", ex);
                        sendException(ex.getResponse().getDescription());
                    } catch (IOException | ServerException ex) {
                        Log.d(TAG, "makeFolder", ex);
                        sendException(ex);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    MakeFolderFragment targetFragment = (MakeFolderFragment) getTargetFragment();
                    if (targetFragment != null) {
                        targetFragment.onComplete();
                    }
                }
            }.execute();
        }
    }
}
