package com.example.myapplication;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "GeoMed.db";
    private static final int DATABASE_VERSION = 3;
    private static final String TAG = "DatabaseHelper";

    // Singleton instance
    private static DatabaseHelper instance;

    // Tabele
    public static final String TABLE_SPITALE = "spitale";
    public static final String TABLE_MEDICI = "medici";
    public static final String TABLE_WEATHER_CACHE = "weather_cache";
    public static final String TABLE_SYNC_LOG = "sync_log";
    public static final String TABLE_PROGRAMARI = "programari";
    public static final String TABLE_USERS = "users";

    // Singleton pattern
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tabela spitale
        String CREATE_SPITALE_TABLE = "CREATE TABLE " + TABLE_SPITALE + "("
                + "id TEXT PRIMARY KEY,"
                + "nume TEXT,"
                + "adresa TEXT,"
                + "telefon TEXT,"
                + "descriere TEXT,"
                + "email TEXT,"
                + "website TEXT,"
                + "imagine TEXT,"
                + "locatie TEXT,"
                + "firebase_last_updated INTEGER,"
                + "local_last_updated INTEGER"
                + ")";
        db.execSQL(CREATE_SPITALE_TABLE);

        // Tabela medici
        String CREATE_MEDICI_TABLE = "CREATE TABLE " + TABLE_MEDICI + "("
                + "id TEXT PRIMARY KEY,"
                + "nume TEXT,"
                + "prenume TEXT,"
                + "specialitate TEXT,"
                + "spital TEXT,"
                + "telefon TEXT,"
                + "email TEXT,"
                + "program TEXT,"
                + "imagine TEXT,"
                + "descriere TEXT,"
                + "firebase_last_updated INTEGER,"
                + "local_last_updated INTEGER"
                + ")";
        db.execSQL(CREATE_MEDICI_TABLE);

        // Tabela programari
        String CREATE_PROGRAMARI_TABLE = "CREATE TABLE " + TABLE_PROGRAMARI + "("
                + "id TEXT PRIMARY KEY,"
                + "userId TEXT,"
                + "spital TEXT,"
                + "specialitate TEXT,"
                + "medic TEXT,"
                + "medicId TEXT,"
                + "data TEXT,"
                + "ora TEXT,"
                + "status TEXT,"
                + "timestamp INTEGER,"
                + "firebase_last_updated INTEGER,"
                + "local_last_updated INTEGER"
                + ")";
        db.execSQL(CREATE_PROGRAMARI_TABLE);

        // Tabela weather cache
        String CREATE_WEATHER_TABLE = "CREATE TABLE " + TABLE_WEATHER_CACHE + "("
                + "location TEXT PRIMARY KEY,"
                + "weather_data TEXT,"
                + "timestamp INTEGER"
                + ")";
        db.execSQL(CREATE_WEATHER_TABLE);

        // Tabela sync log
        String CREATE_SYNC_LOG_TABLE = "CREATE TABLE " + TABLE_SYNC_LOG + "("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "table_name TEXT,"
                + "record_id TEXT,"
                + "operation TEXT,"
                + "data TEXT,"
                + "timestamp INTEGER,"
                + "synced INTEGER DEFAULT 0"
                + ")";
        db.execSQL(CREATE_SYNC_LOG_TABLE);

        // Tabela users
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT,"
                + "email TEXT,"
                + "username TEXT UNIQUE,"
                + "password TEXT,"
                + "firebase_last_updated INTEGER,"
                + "local_last_updated INTEGER"
                + ")";
        db.execSQL(CREATE_USERS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Add programari table if it doesn't exist
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PROGRAMARI + "("
                    + "id TEXT PRIMARY KEY,"
                    + "userId TEXT,"
                    + "spital TEXT,"
                    + "specialitate TEXT,"
                    + "medic TEXT,"
                    + "medicId TEXT,"
                    + "data TEXT,"
                    + "ora TEXT,"
                    + "status TEXT,"
                    + "timestamp INTEGER,"
                    + "firebase_last_updated INTEGER,"
                    + "local_last_updated INTEGER"
                    + ")");
        }
    }

    // Thread-safe database operations
    private synchronized SQLiteDatabase getDatabase(boolean writable) {
        return writable ? getWritableDatabase() : getReadableDatabase();
    }

    // CRUD Operations for Spitale - Thread Safe
    public synchronized long insertOrUpdateSpital(Spital spital, boolean fromFirebase) {
        SQLiteDatabase db = null;
        try {
            db = getDatabase(true);
            ContentValues values = new ContentValues();

            values.put("id", spital.getId());
            values.put("nume", spital.getNume());
            values.put("adresa", spital.getAdresa());
            values.put("telefon", spital.getTelefon());
            values.put("descriere", spital.getDescriere());
            values.put("email", spital.getEmail());
            values.put("website", spital.getWebsite());
            values.put("imagine", spital.getImagine());
            values.put("locatie", spital.getLocatie());

            long timestamp = System.currentTimeMillis();
            if (fromFirebase) {
                values.put("firebase_last_updated", timestamp);
            } else {
                values.put("local_last_updated", timestamp);
                logOperation(TABLE_SPITALE, spital.getId(), "INSERT", spitalToJson(spital));
            }

            return db.insertWithOnConflict(TABLE_SPITALE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            Log.e(TAG, "Error in insertOrUpdateSpital", e);
            return -1;
        }
        // Don't close db - singleton will manage it
    }

    public synchronized List<Spital> getAllSpitale() {
        List<Spital> spitale = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getDatabase(false);
            cursor = db.rawQuery("SELECT * FROM " + TABLE_SPITALE, null);

            if (cursor.moveToFirst()) {
                do {
                    spitale.add(cursorToSpital(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getAllSpitale", e);
        } finally {
            if (cursor != null) cursor.close();
            // Don't close db
        }
        return spitale;
    }

    // CRUD Operations for Medici - Thread Safe
    public synchronized long insertOrUpdateMedic(Medic medic, boolean fromFirebase) {
        SQLiteDatabase db = null;
        try {
            db = getDatabase(true);
            ContentValues values = new ContentValues();

            values.put("id", medic.getId());
            values.put("nume", medic.getNume());
            values.put("prenume", medic.getPrenume());
            values.put("specialitate", medic.getSpecialitate());
            values.put("spital", medic.getSpital());
            values.put("telefon", medic.getTelefon());
            values.put("email", medic.getEmail());
            values.put("program", medic.getProgram());
            values.put("imagine", medic.getImagine());
            values.put("descriere", medic.getDescriere());

            long timestamp = System.currentTimeMillis();
            if (fromFirebase) {
                values.put("firebase_last_updated", timestamp);
            } else {
                values.put("local_last_updated", timestamp);
                logOperation(TABLE_MEDICI, medic.getId(), "INSERT", medicToJson(medic));
            }

            return db.insertWithOnConflict(TABLE_MEDICI, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            Log.e(TAG, "Error in insertOrUpdateMedic", e);
            return -1;
        }
    }

    public synchronized List<Medic> getMediciForSpital(String spitalNume) {
        List<Medic> medici = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getDatabase(false);
            cursor = db.query(TABLE_MEDICI, null, "spital = ?", new String[]{spitalNume}, null, null, null);

            if (cursor.moveToFirst()) {
                do {
                    medici.add(cursorToMedic(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getMediciForSpital", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return medici;
    }

    // CRUD Operations for Programari - Thread Safe
    public synchronized long insertOrUpdateProgramare(Map<String, Object> programare, boolean fromFirebase) {
        SQLiteDatabase db = null;
        try {
            db = getDatabase(true);
            ContentValues values = new ContentValues();

            values.put("id", (String) programare.get("id"));
            values.put("userId", (String) programare.get("userId"));
            values.put("spital", (String) programare.get("spital"));
            values.put("specialitate", (String) programare.get("specialitate"));
            values.put("medic", (String) programare.get("medic"));
            values.put("medicId", (String) programare.get("medicId"));
            values.put("data", (String) programare.get("data"));
            values.put("ora", (String) programare.get("ora"));
            values.put("status", (String) programare.get("status"));

            Object timestamp = programare.get("timestamp");
            if (timestamp instanceof Long) {
                values.put("timestamp", (Long) timestamp);
            } else if (timestamp != null) {
                values.put("timestamp", Long.parseLong(timestamp.toString()));
            }

            long currentTimestamp = System.currentTimeMillis();
            if (fromFirebase) {
                values.put("firebase_last_updated", currentTimestamp);
            } else {
                values.put("local_last_updated", currentTimestamp);
                String programareId = (String) programare.get("id");
                logOperation(TABLE_PROGRAMARI, programareId, "INSERT", programareMapToJson(programare));
            }

            return db.insertWithOnConflict(TABLE_PROGRAMARI, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            Log.e(TAG, "Error in insertOrUpdateProgramare", e);
            return -1;
        }
    }

    public synchronized List<Map<String, Object>> getProgramariForDate(String medicId, String data, String spital) {
        List<Map<String, Object>> programari = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getDatabase(false);
            cursor = db.query(TABLE_PROGRAMARI, null,
                    "medicId = ? AND data = ? AND spital = ?",
                    new String[]{medicId, data, spital}, null, null, "ora ASC");

            if (cursor.moveToFirst()) {
                do {
                    programari.add(cursorToProgramareMap(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getProgramariForDate", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return programari;
    }

    // Sync Operations - Thread Safe
    private synchronized void logOperation(String tableName, String recordId, String operation, String data) {
        SQLiteDatabase db = null;
        try {
            db = getDatabase(true);
            ContentValues values = new ContentValues();

            values.put("table_name", tableName);
            values.put("record_id", recordId);
            values.put("operation", operation);
            values.put("data", data);
            values.put("timestamp", System.currentTimeMillis());
            values.put("synced", 0);

            db.insert(TABLE_SYNC_LOG, null, values);
        } catch (Exception e) {
            Log.e(TAG, "Error in logOperation", e);
        }
    }

    public synchronized List<SyncOperation> getUnsyncedOperations() {
        List<SyncOperation> operations = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getDatabase(false);
            cursor = db.query(TABLE_SYNC_LOG, null, "synced = 0", null, null, null, "timestamp ASC");

            if (cursor.moveToFirst()) {
                do {
                    SyncOperation op = new SyncOperation();
                    op.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    op.tableName = cursor.getString(cursor.getColumnIndexOrThrow("table_name"));
                    op.recordId = cursor.getString(cursor.getColumnIndexOrThrow("record_id"));
                    op.operation = cursor.getString(cursor.getColumnIndexOrThrow("operation"));
                    op.data = cursor.getString(cursor.getColumnIndexOrThrow("data"));
                    op.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
                    operations.add(op);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getUnsyncedOperations", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return operations;
    }

    public synchronized void markOperationAsSynced(int operationId) {
        SQLiteDatabase db = null;
        try {
            db = getDatabase(true);
            ContentValues values = new ContentValues();
            values.put("synced", 1);
            db.update(TABLE_SYNC_LOG, values, "id = ?", new String[]{String.valueOf(operationId)});
        } catch (Exception e) {
            Log.e(TAG, "Error in markOperationAsSynced", e);
        }
    }

    // Helper methods pentru conversii
    private Spital cursorToSpital(Cursor cursor) {
        Spital spital = new Spital();
        spital.setId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
        spital.setNume(cursor.getString(cursor.getColumnIndexOrThrow("nume")));
        spital.setAdresa(cursor.getString(cursor.getColumnIndexOrThrow("adresa")));
        spital.setTelefon(cursor.getString(cursor.getColumnIndexOrThrow("telefon")));
        spital.setDescriere(cursor.getString(cursor.getColumnIndexOrThrow("descriere")));
        spital.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("email")));
        spital.setWebsite(cursor.getString(cursor.getColumnIndexOrThrow("website")));
        spital.setImagine(cursor.getString(cursor.getColumnIndexOrThrow("imagine")));
        spital.setLocatie(cursor.getString(cursor.getColumnIndexOrThrow("locatie")));

        if (!cursor.isNull(cursor.getColumnIndexOrThrow("firebase_last_updated"))) {
            spital.setFirebase_last_updated(cursor.getLong(cursor.getColumnIndexOrThrow("firebase_last_updated")));
        }
        if (!cursor.isNull(cursor.getColumnIndexOrThrow("local_last_updated"))) {
            spital.setLocal_last_updated(cursor.getLong(cursor.getColumnIndexOrThrow("local_last_updated")));
        }

        return spital;
    }

    private Medic cursorToMedic(Cursor cursor) {
        Medic medic = new Medic();
        medic.setId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
        medic.setNume(cursor.getString(cursor.getColumnIndexOrThrow("nume")));
        medic.setPrenume(cursor.getString(cursor.getColumnIndexOrThrow("prenume")));
        medic.setSpecialitate(cursor.getString(cursor.getColumnIndexOrThrow("specialitate")));
        medic.setSpital(cursor.getString(cursor.getColumnIndexOrThrow("spital")));
        medic.setTelefon(cursor.getString(cursor.getColumnIndexOrThrow("telefon")));
        medic.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("email")));
        medic.setProgram(cursor.getString(cursor.getColumnIndexOrThrow("program")));
        medic.setImagine(cursor.getString(cursor.getColumnIndexOrThrow("imagine")));
        medic.setDescriere(cursor.getString(cursor.getColumnIndexOrThrow("descriere")));

        if (!cursor.isNull(cursor.getColumnIndexOrThrow("firebase_last_updated"))) {
            medic.setFirebase_last_updated(cursor.getLong(cursor.getColumnIndexOrThrow("firebase_last_updated")));
        }
        if (!cursor.isNull(cursor.getColumnIndexOrThrow("local_last_updated"))) {
            medic.setLocal_last_updated(cursor.getLong(cursor.getColumnIndexOrThrow("local_last_updated")));
        }

        return medic;
    }

    private Map<String, Object> cursorToProgramareMap(Cursor cursor) {
        Map<String, Object> programare = new HashMap<>();
        programare.put("id", cursor.getString(cursor.getColumnIndexOrThrow("id")));
        programare.put("userId", cursor.getString(cursor.getColumnIndexOrThrow("userId")));
        programare.put("spital", cursor.getString(cursor.getColumnIndexOrThrow("spital")));
        programare.put("specialitate", cursor.getString(cursor.getColumnIndexOrThrow("specialitate")));
        programare.put("medic", cursor.getString(cursor.getColumnIndexOrThrow("medic")));
        programare.put("medicId", cursor.getString(cursor.getColumnIndexOrThrow("medicId")));
        programare.put("data", cursor.getString(cursor.getColumnIndexOrThrow("data")));
        programare.put("ora", cursor.getString(cursor.getColumnIndexOrThrow("ora")));
        programare.put("status", cursor.getString(cursor.getColumnIndexOrThrow("status")));
        programare.put("timestamp", cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")));

        if (!cursor.isNull(cursor.getColumnIndexOrThrow("firebase_last_updated"))) {
            programare.put("firebase_last_updated", cursor.getLong(cursor.getColumnIndexOrThrow("firebase_last_updated")));
        }
        if (!cursor.isNull(cursor.getColumnIndexOrThrow("local_last_updated"))) {
            programare.put("local_last_updated", cursor.getLong(cursor.getColumnIndexOrThrow("local_last_updated")));
        }

        return programare;
    }

    // JSON conversions (fără Gson)
    private String spitalToJson(Spital spital) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", spital.getId());
            json.put("nume", spital.getNume());
            json.put("adresa", spital.getAdresa());
            json.put("telefon", spital.getTelefon());
            json.put("descriere", spital.getDescriere());
            json.put("email", spital.getEmail());
            json.put("website", spital.getWebsite());
            json.put("imagine", spital.getImagine());
            json.put("locatie", spital.getLocatie());
            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Spital jsonToSpital(String json) {
        try {
            JSONObject jsonObj = new JSONObject(json);
            Spital spital = new Spital();
            spital.setId(jsonObj.optString("id"));
            spital.setNume(jsonObj.optString("nume"));
            spital.setAdresa(jsonObj.optString("adresa"));
            spital.setTelefon(jsonObj.optString("telefon"));
            spital.setDescriere(jsonObj.optString("descriere"));
            spital.setEmail(jsonObj.optString("email"));
            spital.setWebsite(jsonObj.optString("website"));
            spital.setImagine(jsonObj.optString("imagine"));
            spital.setLocatie(jsonObj.optString("locatie"));
            return spital;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String medicToJson(Medic medic) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", medic.getId());
            json.put("nume", medic.getNume());
            json.put("prenume", medic.getPrenume());
            json.put("specialitate", medic.getSpecialitate());
            json.put("spital", medic.getSpital());
            json.put("telefon", medic.getTelefon());
            json.put("email", medic.getEmail());
            json.put("program", medic.getProgram());
            json.put("imagine", medic.getImagine());
            json.put("descriere", medic.getDescriere());
            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Medic jsonToMedic(String json) {
        try {
            JSONObject jsonObj = new JSONObject(json);
            Medic medic = new Medic();
            medic.setId(jsonObj.optString("id"));
            medic.setNume(jsonObj.optString("nume"));
            medic.setPrenume(jsonObj.optString("prenume"));
            medic.setSpecialitate(jsonObj.optString("specialitate"));
            medic.setSpital(jsonObj.optString("spital"));
            medic.setTelefon(jsonObj.optString("telefon"));
            medic.setEmail(jsonObj.optString("email"));
            medic.setProgram(jsonObj.optString("program"));
            medic.setImagine(jsonObj.optString("imagine"));
            medic.setDescriere(jsonObj.optString("descriere"));
            return medic;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // JSON conversions pentru Map
    private String programareMapToJson(Map<String, Object> programare) {
        try {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, Object> entry : programare.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<String, Object> jsonToProgramareMap(String json) {
        try {
            JSONObject jsonObj = new JSONObject(json);
            Map<String, Object> programare = new HashMap<>();

            programare.put("id", jsonObj.optString("id"));
            programare.put("userId", jsonObj.optString("userId"));
            programare.put("spital", jsonObj.optString("spital"));
            programare.put("specialitate", jsonObj.optString("specialitate"));
            programare.put("medic", jsonObj.optString("medic"));
            programare.put("medicId", jsonObj.optString("medicId"));
            programare.put("data", jsonObj.optString("data"));
            programare.put("ora", jsonObj.optString("ora"));
            programare.put("status", jsonObj.optString("status"));
            programare.put("timestamp", jsonObj.optLong("timestamp"));

            if (jsonObj.has("firebase_last_updated")) {
                programare.put("firebase_last_updated", jsonObj.optLong("firebase_last_updated"));
            }
            if (jsonObj.has("local_last_updated")) {
                programare.put("local_last_updated", jsonObj.optLong("local_last_updated"));
            }

            return programare;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Adaugă și metodele care lipsesc pentru users și alte funcții necesare
    public synchronized long insertOrUpdateUser(HelperClass user, boolean fromFirebase) {
        SQLiteDatabase db = null;
        try {
            db = getDatabase(true);
            ContentValues values = new ContentValues();

            values.put("name", user.getName());
            values.put("email", user.getEmail());
            values.put("username", user.getUsername());
            values.put("password", user.getPassword());

            long timestamp = System.currentTimeMillis();
            if (fromFirebase) {
                values.put("firebase_last_updated", timestamp);
            } else {
                values.put("local_last_updated", timestamp);
                logOperation(TABLE_USERS, user.getUsername(), "INSERT", userToJson(user));
            }

            return db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            Log.e(TAG, "Error in insertOrUpdateUser", e);
            return -1;
        }
    }

    public synchronized HelperClass getUserByUsername(String username) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getDatabase(false);
            cursor = db.query(TABLE_USERS, null, "username = ?", new String[]{username}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                return cursorToUser(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getUserByUsername", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public synchronized boolean validateUser(String username, String password) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getDatabase(false);
            cursor = db.query(TABLE_USERS, null, "username = ? AND password = ?",
                    new String[]{username, password}, null, null, null);
            return cursor != null && cursor.getCount() > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error in validateUser", e);
            return false;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    // Helper methods pentru conversii users
    private HelperClass cursorToUser(Cursor cursor) {
        String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
        String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
        String password = cursor.getString(cursor.getColumnIndexOrThrow("password"));

        return new HelperClass(name, email, username, password);
    }

    // JSON conversions pentru sincronizare user
    private String userToJson(HelperClass user) {
        try {
            JSONObject json = new JSONObject();
            json.put("name", user.getName());
            json.put("email", user.getEmail());
            json.put("username", user.getUsername());
            json.put("password", user.getPassword());
            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public HelperClass jsonToUser(String json) {
        try {
            JSONObject jsonObj = new JSONObject(json);
            String name = jsonObj.getString("name");
            String email = jsonObj.getString("email");
            String username = jsonObj.getString("username");
            String password = jsonObj.getString("password");

            return new HelperClass(name, email, username, password);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public synchronized void clearSyncedOperations() {
        SQLiteDatabase db = null;
        try {
            db = getDatabase(true);
            // Delete all operations that have been marked as synced
            int rowsDeleted = db.delete(TABLE_SYNC_LOG, "synced = 1", null);
            Log.d(TAG, "Deleted " + rowsDeleted + " synced operations from sync log");
        } catch (Exception e) {
            Log.e(TAG, "Error in clearSyncedOperations", e);
        }
    }

    // Optional: Clear all sync operations (including unsynced ones)
    public synchronized void clearAllSyncOperations() {
        SQLiteDatabase db = null;
        try {
            db = getDatabase(true);
            // Delete all operations from sync log
            int rowsDeleted = db.delete(TABLE_SYNC_LOG, null, null);
            Log.d(TAG, "Deleted " + rowsDeleted + " total operations from sync log");
        } catch (Exception e) {
            Log.e(TAG, "Error in clearAllSyncOperations", e);
        }
    }

    // Optional: Get count of synced operations
    public synchronized int getSyncedOperationsCount() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getDatabase(false);
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SYNC_LOG + " WHERE synced = 1", null);
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getSyncedOperationsCount", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return 0;
    }

    public synchronized Medic getMedic(String medicId) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getDatabase(false);
            cursor = db.query(TABLE_MEDICI, null, "id = ?", new String[]{medicId}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                return cursorToMedic(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getMedic", e);
        } finally {
            if (cursor != null) cursor.close();
            // Don't close db - singleton will manage it
        }
        return null;
    }

    public synchronized List<Medic> getAllMedici() {
        List<Medic> medici = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getDatabase(false);
            cursor = db.rawQuery("SELECT * FROM " + TABLE_MEDICI, null);

            if (cursor.moveToFirst()) {
                do {
                    medici.add(cursorToMedic(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getAllMedici", e);
        } finally {
            if (cursor != null) cursor.close();
            // Don't close db - singleton will manage it
        }
        return medici;
    }

    // Override close to prevent premature closing
    @Override
    public synchronized void close() {
        // Don't allow closing - singleton manages lifecycle
    }
}