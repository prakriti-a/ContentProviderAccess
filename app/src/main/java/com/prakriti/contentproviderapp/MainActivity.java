package com.prakriti.contentproviderapp;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
// Loader automatically keeps data updated parallel to content provider

    private static final String TAG = "MainActivity";
    private Cursor cursor;
    private ArrayList<String> contactNameList;
    private EditText edtEnteredName, edtNewName;
    private TextView txtQueryResult;

    private final int CONTACTS_REQUEST = 777;

    //    private ArrayAdapter<String> adapter;
//    private boolean firstTimeLoaded = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate called");


//        ListView contactsListView = findViewById(R.id.contactsListView);
        contactNameList = new ArrayList<>();
//        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, contactNameList);
//        contactsListView.setAdapter(adapter);

        txtQueryResult = findViewById(R.id.txtQueryResult);
        edtEnteredName = findViewById(R.id.edtEnteredName);
        edtNewName = findViewById(R.id.edtNewName);

        ImageButton btnRetrieveData = findViewById(R.id.btnRetrieveData);
        Button btnAddData = findViewById(R.id.btnAddData);
        Button btnDeleteData = findViewById(R.id.btnDeleteData);
        Button btnUpdateData = findViewById(R.id.btnUpdateData);

        btnRetrieveData.setOnClickListener(this);
        btnAddData.setOnClickListener(this);
        btnDeleteData.setOnClickListener(this);
        btnUpdateData.setOnClickListener(this);

        getUserPermission();
    }

    @Override
    public void onClick(View view) {
        // load data here
        switch (view.getId()) {
            case R.id.btnRetrieveData:
                retrieveContacts();
                break;
            case R.id.btnAddData:
                if(!isFieldNull(edtEnteredName)) {
                    String nameToAdd = edtEnteredName.getText().toString();
                    addContact(nameToAdd);
                }
                break;
            case R.id.btnUpdateData:
                edtNewName.setVisibility(View.VISIBLE);
                if(!isFieldNull(edtEnteredName) && !isFieldNull(edtNewName)) {
                    String oldName = edtEnteredName.getText().toString();
                    String newName = edtNewName.getText().toString();
                    updateContact(oldName, newName);
                }
                break;
            case R.id.btnDeleteData:
                if(!isFieldNull(edtEnteredName)) {
                    String nameToDelete = edtEnteredName.getText().toString();
                    deleteContact(nameToDelete);
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + view.getId());
        }


        // trigger loader initialisation
//        if(!firstTimeLoaded) {
//            // id -> to identify a loader in an activity with multiple loaders
//            getLoaderManager().initLoader(1, null, this); // triggers onCreateLoader
//            firstTimeLoaded = true;
//        }
//        else {
//            getLoaderManager().restartLoader(1, null, this);
//        }

    }

    private void retrieveContacts() {
        if(edtNewName.getVisibility() == View.VISIBLE){
            edtNewName.setVisibility(View.GONE);
        }

        String[] columnProjection = new String[] { ContactsContract.Contacts.DISPLAY_NAME_PRIMARY };
        String selectionClause = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " = ?";
        String[] selectionArgs = new String[] { "Prakriti" };
        String orderBy = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY;

        ContentResolver contentResolver = getContentResolver(); // init content resolver
        cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, // table to query
                columnProjection, // columns to fetch
                null,   // WHERE clause
                null, // this value replaces ? in WHERE clause
                null);

        /* this iteration is happening in main thread -> in case of huge data may cause ANR
            Loader by default loads data on non ui thread
        */
        if(cursor != null && cursor.getCount() > 0) { // null check
            Log.i(TAG, "displayContacts: Running cursor iteration");
            StringBuilder queryResult = new StringBuilder();
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                queryResult.append(name).append("\n").append("\n");
                if(!contactNameList.contains(name)) {
                    contactNameList.add(name.toLowerCase());
                }
            }
            txtQueryResult.setText(queryResult.toString());
        }
        else {
            Toast.makeText(this, R.string.no_contacts, Toast.LENGTH_LONG).show();
            Log.i(TAG, "displayContacts: Empty cursor object");
        }
    }

    private void addContact(String name) {
        Log.i(TAG, "addContact: " + name);
        // Batch operations
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        operations.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, "acc_name@gmail.com")
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, "com.google")
                .build());
        operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build());
        try {
            ContentProviderResult[] res = getContentResolver().applyBatch(ContactsContract.AUTHORITY, operations);
            if (res == null) {
                Toast.makeText(this, R.string.op_failed, Toast.LENGTH_SHORT).show();
                Log.i(TAG, "addContact: Failed");
            } else {
                Toast.makeText(this, R.string.op_success, Toast.LENGTH_SHORT).show();
                Log.i(TAG, "addContact: Success " + res[0]);
                edtEnteredName.setText("");
            }
        } catch (Exception e) {
            Log.e(TAG, "addContact: " + e.getMessage());
        }
    }

    private void deleteContact(String name) {
        Log.i(TAG, "deleteContact: " + name);

        String whereClause = "lower(" + ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY + ")=lower( ? )"; // case insensitive
        String[] whereArgs = new String[] { name };
        // delete from table where col=val
        int del = getContentResolver().delete(ContactsContract.RawContacts.CONTENT_URI, whereClause, whereArgs);
        if(del <= 0) {
            Toast.makeText(this, R.string.dne, Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(this, R.string.op_success, Toast.LENGTH_SHORT).show();
            edtEnteredName.setText("");
        }
    }

    private void updateContact(String oldName, String newName) {
        Log.i(TAG, "updateContact: " + oldName + " == " + newName);

        String whereClause = ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY + " = ? COLLATE NOCASE"; // case insensitive
        String[] whereArgs = new String[] { oldName };
        ContentValues contentValues = new ContentValues();
        contentValues.put(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY, newName);
        int update = getContentResolver().update(ContactsContract.RawContacts.CONTENT_URI, contentValues, whereClause, whereArgs);
        if(update <= 0) {
            Toast.makeText(this, R.string.op_failed, Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(this, R.string.op_success, Toast.LENGTH_SHORT).show();
            edtEnteredName.setText("");
            edtNewName.setText("");
            edtNewName.setVisibility(View.GONE);
        }
    }

    public boolean isFieldNull(EditText field) {
        if (field.getText().toString().trim().equals("")) {
            field.setError("This field cannot be blank");
            field.requestFocus();
            return true;
        }
        return false;
    }

    private void getUserPermission() {
        if(checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "getUserPermission: GRANTED");
            retrieveContacts();
        }
        else {
            Log.i(TAG, "getUserPermission: DECLINED");
            requestPermissions(new String[] {Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS}, CONTACTS_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == CONTACTS_REQUEST && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "onRequestPermissionsResult: GRANTED");
            retrieveContacts();
        }
        else {
            Log.i(TAG, "onRequestPermissionsResult: DECLINED");
            requestPermissions(new String[] {Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS}, CONTACTS_REQUEST);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(cursor != null) {
            cursor.close();
        }
    }
}



/*   @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // inits worker thread to load data
        if(id == 1) {
            // similar to ContentResolver query() method
            return new CursorLoader(this, ContactsContract.Contacts.CONTENT_URI, columnProjection, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // once data is loaded, set UI
        if(cursor != null && cursor.getCount() > 0) { // null check
            Log.i(TAG, "displayContacts: Running cursor iteration");
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                if(!displayNameList.contains(name)) {
                    displayNameList.add(cursor.getString(0));
                    adapter.notifyDataSetChanged();
                }
            }
        }
        else { // fail case
            Toast.makeText(this, R.string.no_contacts, Toast.LENGTH_LONG).show();
            Log.i(TAG, "displayContacts: Empty cursor object");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {}




    //        ContentValues contentValues = new ContentValues(); // api to hold key-value pairs of col & val to be inserted
//        contentValues.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name);
//        Uri uri = getContentResolver().insert(ContactsContract.Contacts.CONTENT_URI, contentValues);
//        if(uri == null) {
//            Toast.makeText(this, R.string.op_failed, Toast.LENGTH_SHORT).show();
//            Log.i(TAG, "addContact: Failed");
//        }
//        else {
//            Toast.makeText(this, R.string.op_success, Toast.LENGTH_SHORT).show();
//            Log.i(TAG, "addContact: Success " + uri);
//            // refresh...
//            edtEnteredName.setText("");
//        }

*/