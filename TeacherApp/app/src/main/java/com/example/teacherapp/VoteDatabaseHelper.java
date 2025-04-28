package com.example.teacherapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

public class VoteDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "voting.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "votes";
    private static final String COLUMN_STUDENT = "student";
    private static final String COLUMN_CANDIDATE = "candidate";

    public VoteDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_STUDENT + " TEXT PRIMARY KEY,"
                + COLUMN_CANDIDATE + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Not needed now
    }

    public boolean addVote(String student, String candidate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STUDENT, student);
        values.put(COLUMN_CANDIDATE, candidate);

        long result = db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        return result != -1;
    }

    public Cursor getAllVotes() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }
}
