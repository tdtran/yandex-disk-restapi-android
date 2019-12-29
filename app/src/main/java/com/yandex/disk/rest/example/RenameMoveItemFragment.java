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

public class RenameMoveItemFragment extends IODialogFragment {

    private static final String TAG = "RenameMoveItemFragment";

    private static final String WORK_FRAGMENT_TAG = "RenameMoveItemFragment.Background";

    private static final String MOVE_SRC_PATH = "example.move.src.path";
    private static final String MOVE_DST_PATH = "example.move.dst.path";

    private Credentials credentials;
    private String srcPath, dstPath;

    private RenameMoveItemRetainedFragment workFragment;

    public static RenameMoveItemFragment newInstance(Credentials credentials, String srcPath, String dstPath) {
        RenameMoveItemFragment fragment = new RenameMoveItemFragment();

        Bundle args = new Bundle();
        args.putParcelable(CREDENTIALS, credentials);
        args.putString(MOVE_SRC_PATH, srcPath);
        args.putString(MOVE_DST_PATH, dstPath);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        credentials = getArguments().getParcelable(CREDENTIALS);
        srcPath = getArguments().getString(MOVE_SRC_PATH);
        dstPath = getArguments().getString(MOVE_DST_PATH);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentManager fragmentManager = getFragmentManager();
        workFragment = (RenameMoveItemRetainedFragment) fragmentManager.findFragmentByTag(WORK_FRAGMENT_TAG);
        if (workFragment == null || workFragment.getTargetFragment() == null) {
            workFragment = new RenameMoveItemRetainedFragment();
            fragmentManager.beginTransaction().add(workFragment, WORK_FRAGMENT_TAG).commit();
            workFragment.renameMoveItem(credentials, srcPath, dstPath);
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
        dialog.setTitle(R.string.example_move_rename_item_title);
        dialog.setMessage(dstPath);
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
        Toast.makeText(getActivity(), R.string.example_move_rename_item_done, Toast.LENGTH_LONG).show();
        ((ExampleActivity) getActivity()).reloadContent();
    }

    public static class RenameMoveItemRetainedFragment extends IODialogRetainedFragment {

        public void renameMoveItem(final Credentials credentials, final String srcPath, final String dstPath) {

            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    RestClient client = null;
                    try {
                        client = RestClientUtil.getInstance(credentials);
                        client.move(srcPath, dstPath, false);
                    } catch (HttpCodeException ex) {
                        Log.d(TAG, "renameMoveItem", ex);
                        sendException(ex.getResponse().getDescription());
                    } catch (IOException | ServerException ex) {
                        Log.d(TAG, "renameMoveItem", ex);
                        sendException(ex);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    RenameMoveItemFragment targetFragment = (RenameMoveItemFragment) getTargetFragment();
                    if (targetFragment != null) {
                        targetFragment.onComplete();
                    }
                }
            }.execute();
        }
    }
}
