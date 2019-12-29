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

public class DeleteItemFragment extends IODialogFragment {

    private static final String TAG = "DeleteItemFragment";

    private static final String WORK_FRAGMENT_TAG = "DeleteItemFragment.Background";

    private static final String DELETE_PATH = "example.delete.path";
    private static final String DELETE_DISPLAY_NAME = "example.delete.display.name";
    private static final String DELETE_IS_COLLECTION = "example.delete.is.collection";

    private Credentials credentials;
    private String path, displayName;
    private boolean isCollection;

    private DeleteItemRetainedFragment workFragment;

    public static DeleteItemFragment newInstance(Credentials credentials, String path, String displayName, boolean isCollection) {
        DeleteItemFragment fragment = new DeleteItemFragment();

        Bundle args = new Bundle();
        args.putParcelable(CREDENTIALS, credentials);
        args.putString(DELETE_PATH, path);
        args.putString(DELETE_DISPLAY_NAME, displayName);
        args.putBoolean(DELETE_IS_COLLECTION, isCollection);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        credentials = getArguments().getParcelable(CREDENTIALS);
        path = getArguments().getString(DELETE_PATH);
        displayName = getArguments().getString(DELETE_DISPLAY_NAME);
        isCollection = getArguments().getBoolean(DELETE_IS_COLLECTION);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentManager fragmentManager = getFragmentManager();
        workFragment = (DeleteItemRetainedFragment) fragmentManager.findFragmentByTag(WORK_FRAGMENT_TAG);
        if (workFragment == null || workFragment.getTargetFragment() == null) {
            workFragment = new DeleteItemRetainedFragment();
            fragmentManager.beginTransaction().add(workFragment, WORK_FRAGMENT_TAG).commit();
            workFragment.deleteItem(credentials, path);
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
        dialog.setTitle(isCollection ? R.string.example_delete_folder_title : R.string.example_delete_file_title);
        dialog.setMessage(displayName);
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
        Toast.makeText(getActivity(), isCollection ? R.string.example_delete_folder_done : R.string.example_delete_file_done, Toast.LENGTH_LONG).show();
        ((ExampleActivity) getActivity()).reloadContent();
    }

    public static class DeleteItemRetainedFragment extends IODialogRetainedFragment {

        public void deleteItem(final Credentials credentials, final String path) {

            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        RestClient client = RestClientUtil.getInstance(credentials);
                        client.delete(path, false);
                    } catch (HttpCodeException ex) {
                        Log.d(TAG, "deleteItem", ex);
                        sendException(ex.getResponse().getDescription());
                    } catch (IOException | ServerException ex) {
                        Log.d(TAG, "deleteItem", ex);
                        sendException(ex);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    DeleteItemFragment targetFragment = (DeleteItemFragment) getTargetFragment();
                    if (targetFragment != null) {
                        targetFragment.onComplete();
                    }
                }
            }.execute();
        }
    }
}
