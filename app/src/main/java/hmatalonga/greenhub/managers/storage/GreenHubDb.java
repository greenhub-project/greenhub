/*
 * Copyright (C) 2016 Hugo Matalonga & João Paulo Fernandes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hmatalonga.greenhub.managers.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import hmatalonga.greenhub.models.data.Sample;

/**
 *
 * Created by hugo on 16-04-2016.
 */
public class GreenHubDb {
    private static final String TAG = "GreenHubDb";

    private static final HashMap<String, String> COLUMN_MAP;
    private static final Object DB_LOCK;

    private static GreenHubDb sInstance = null;

    private Sample mLastSample = null;
    private SQLiteDatabase mDatabase = null;
    private GreenHubDbHelper mHelper = null;

    static {
        DB_LOCK = new Object();
        COLUMN_MAP = buildColumnMap();
    }

    public static GreenHubDb getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new GreenHubDb(context);
        }
        return sInstance;
    }

    private GreenHubDb(final Context context) {
        synchronized (DB_LOCK) {
            mHelper = new GreenHubDbHelper(context);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        synchronized (DB_LOCK) {
            if (mDatabase != null) mDatabase.close();
        }
        super.finalize();
    }

    /**
     *
     * Builds a map for all columns that may be requested, which will be given
     * to the SQLiteQueryBuilder. This is a good way to define aliases for
     * column names, but must include all columns, even if the value is the key.
     * This allows the ContentProvider to request columns w/o the need to know
     * real column names and create the alias itself.
     *
     * TODO: Needs to be updated when fields update.
     */
    private static HashMap<String, String> buildColumnMap() {
        HashMap<String, String> map = new HashMap<>();
        map.put(GreenHubDbContract.GreenHubEntry.COLUMN_TIMESTAMP, GreenHubDbContract.GreenHubEntry.COLUMN_TIMESTAMP);
        map.put(GreenHubDbContract.GreenHubEntry.COLUMN_SAMPLE, GreenHubDbContract.GreenHubEntry.COLUMN_SAMPLE);
        map.put(BaseColumns._ID, "rowid AS " + BaseColumns._ID);

        return map;
    }

    /**
     * Performs a database query.
     *
     * @param selection
     *            The selection clause
     * @param selectionArgs
     *            Selection arguments for "?" components in the selection
     * @param columns
     *            The columns to return
     * @return A Cursor over all rows matching the query
     */
    private Cursor query(String selection, String[] selectionArgs,
                         String[] columns, String groupBy, String having, String sortOrder) {
        /*
         * The SQLiteBuilder provides a map for all possible columns requested
         * to actual columns in the database, creating a simple column alias
         * mechanism by which the ContentProvider does not need to know the real
         * column names
         */
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(GreenHubDbContract.GreenHubEntry.SAMPLES_VIRTUAL_TABLE);
        builder.setProjectionMap(COLUMN_MAP);

        Cursor cursor = builder.query(mDatabase, columns, selection, selectionArgs,
                groupBy, having, sortOrder);

        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }

        return cursor;
    }

    public int countSamples() {
        try {
            synchronized (DB_LOCK) {
                if (mDatabase == null || !mDatabase.isOpen()) {
                    try {
                        mDatabase = mHelper.getWritableDatabase();
                    } catch (android.database.sqlite.SQLiteException ex){
                        Log.e(TAG, "Could not open database", ex);
                        return -1;
                    }
                }

                Cursor cursor = mDatabase.rawQuery("select count(timestamp) FROM " +
                        GreenHubDbContract.GreenHubEntry.SAMPLES_VIRTUAL_TABLE, null);

                if (cursor == null)
                    // There are no results
                    return -1;
                else {
                    int ret = -1;
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        ret = cursor.getInt(0);
                        cursor.moveToNext();
                    }
                    cursor.close();

                    return ret;
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }

        return -1;
    }

    public SortedMap<Long, Sample> queryOldestSamples(int how_many) {
        SortedMap<Long, Sample> results = new TreeMap<>();

        try {
            synchronized (DB_LOCK) {
                if (mDatabase == null || !mDatabase.isOpen()) {
                    try {
                        mDatabase = mHelper.getWritableDatabase();
                    } catch (android.database.sqlite.SQLiteException ex) {
                        return results;
                    }
                }
                String[] columns = COLUMN_MAP.keySet().toArray(new String[COLUMN_MAP.size()]);

                Cursor cursor = query(null, null, columns, null, null, GreenHubDbContract.GreenHubEntry.COLUMN_TIMESTAMP +
                        " ASC LIMIT " + how_many);

                if (cursor == null) {
                    // Log.d("CaratSampleDB", "query returned null");
                    // There are no results
                    return results;
                } else {
                    // Log.d("CaratSampleDB", "query is successfull!");
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        Sample s = fillSample(cursor);
                        if (s != null) {
                            results.put(cursor.getLong(cursor
                                    .getColumnIndex(BaseColumns._ID)), s);
                            cursor.moveToNext();
                        }
                    }
                    cursor.close();

                }
            }
        } catch (Throwable th) {
            Log.e(TAG, "Failed to query oldest samples!", th);
        }

        return results;
    }

    private int delete(String whereClause, String[] whereArgs) {
        return mDatabase.delete(GreenHubDbContract.GreenHubEntry.SAMPLES_VIRTUAL_TABLE, whereClause, whereArgs);
    }

    public int deleteSamples(Set<Long> row_ids) {
        int ret = 0;

        try {
            synchronized (DB_LOCK) {
                if (mDatabase == null || !mDatabase.isOpen())
                    mDatabase = mHelper.getWritableDatabase();

                StringBuilder sb = new StringBuilder();
                int i = 0;
                sb.append("(");

                for (Long row_id : row_ids) {
                    sb.append("").append(row_id);
                    i++;
                    if (i != row_ids.size()) {
                        sb.append(", ");
                    }
                }
                sb.append(")");
                ret = delete("rowid in " + sb.toString(), null);

                if (mDatabase != null && mDatabase.isOpen()) {
                    mDatabase.close();
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }

        return ret;
    }

    private Sample queryLastSample() {
        String[] columns = COLUMN_MAP.keySet().toArray(new String[COLUMN_MAP.size()]);

        Cursor cursor = query(
                null,
                null,
                columns,
                null,
                null,
                GreenHubDbContract.GreenHubEntry.COLUMN_TIMESTAMP + " DESC LIMIT 1"
        );

        if (cursor == null) {
            // There are no results
            return null;
        } else {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                Sample sample = fillSample(cursor);
                cursor.close();
                mLastSample = sample;
                return sample;
            }
            cursor.close();

            return null;
        }
    }

    /**
     * Reads a sample from the current position of the cursor.
     *
     * @param cursor
     * @return
     */
    private Sample fillSample(Cursor cursor) {
        String json = cursor.getString(
                cursor.getColumnIndex(GreenHubDbContract.GreenHubEntry.COLUMN_SAMPLE)
        );

        return (json != null) ? getSampleFromJson(json) : null;
    }

    private Sample getSampleFromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, new TypeToken<Sample>() {}.getType());
    }

    public Sample getLastSample() {
        try {
            synchronized (DB_LOCK) {
                if (mDatabase == null || !mDatabase.isOpen()) {
                    try {
                        mDatabase = mHelper.getWritableDatabase();
                    } catch (android.database.sqlite.SQLiteException ex){
                        Log.e(TAG, "Could not open database", ex);
                        return mLastSample;
                    }
                }
                if (mLastSample == null)
                    queryLastSample();
            }
        } catch (Throwable th) {
            Log.e(TAG, "Failed to get last sample!", th);
        }

        return mLastSample;
    }

    /**
     * Store the sample into the database
     * @param sample the sample to be saved
     * @return positive int if the operation is successful, otherwise zero
     */
    public long putSample(Sample sample) {
        long id = 0;
        try {
            synchronized (DB_LOCK) {
                if (mDatabase == null || !mDatabase.isOpen()) {
                    mDatabase = mHelper.getWritableDatabase();
                }

                // force init
                id = addSample(sample);

                if (id >= 0) mLastSample = sample;

                mDatabase.close();
            }
        } catch (Throwable th) {
            Log.e(TAG, "Failed to add a sample!", th);
        }

        return id;
    }

    /**
     * Add a sample to the database.
     *
     * @return rowId or -1 if failed
     */
    private long addSample(Sample sample) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(
                GreenHubDbContract.GreenHubEntry.COLUMN_TIMESTAMP,
                sample.getTimestamp()
        );

        // Write the sample as a JSON string
        Gson gson = new Gson();
        initialValues.put(
                GreenHubDbContract.GreenHubEntry.COLUMN_SAMPLE,
                gson.toJson(sample)
        );

        return mDatabase.insert(
                GreenHubDbContract.GreenHubEntry.SAMPLES_VIRTUAL_TABLE,
                null,
                initialValues
        );
    }

    private static class GreenHubDbHelper extends SQLiteOpenHelper {
        private SQLiteDatabase mDatabase;

        // If you change the database schema, you must increment the database version.
        static final int DATABASE_VERSION = 1;
        static final String DATABASE_NAME = "GreenHubHelper.mDatabase";

        private static final String FTS_TABLE_CREATE = "CREATE VIRTUAL TABLE "
                + GreenHubDbContract.GreenHubEntry.SAMPLES_VIRTUAL_TABLE + " USING fts3 (" + createStatement()
                + ");";

        private static String createStatement() {
            Set<String> set = COLUMN_MAP.keySet();
            StringBuilder b = new StringBuilder();
            int i = 0;
            int size = set.size() - 1;
            for (String s : set) {
                if (s.equals(BaseColumns._ID))
                    continue;
                if (i + 1 == size)
                    b.append(s);
                else
                    b.append(s).append(", ");
                i++;
            }
            return b.toString();
        }

        GreenHubDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            mDatabase = db;
            try {
                mDatabase.execSQL(FTS_TABLE_CREATE);
                mDatabase.execSQL(GreenHubDbContract.GreenHubEntry.SQL_COMPACT_DATABASE);
            }
            catch (NullPointerException | SQLException e) {
                e.printStackTrace();
            }
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL("DROP TABLE IF EXISTS " + GreenHubDbContract.GreenHubEntry.SAMPLES_VIRTUAL_TABLE);
            onCreate(db);
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }
}