package saffih.tools

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 18/05/17.
 */


abstract class DatabaseHandler<T>(context: Context,
                                  DATABASE_NAME: String = "db",
                                  DATABASE_VERSION: Int = 1) : SQLiteOpenHelper(
        context,
        DATABASE_NAME,
        null,
        DATABASE_VERSION) {

    abstract fun to(values: ContentValues): T
    //    abstract fun from(t:T): ContentValues
    abstract fun toPairs(t: T): List<Pair<String, Any?>>

    inline fun from(t: T) = toPairs(t).asContentValues()
    enum class ColumnType(name: String) {
        TEXT(name = "TEXT"),
        INTEGER(name = "INTEGER"),
        REAL(name = "REAL"),
        BLOB(name = "BLOB"),
        NUMERIC(name = "NUMERIC");
    }

    enum class FieldType(columnType: ColumnType) {
        FString(ColumnType.TEXT),
        FByte(ColumnType.INTEGER),
        FShort(ColumnType.INTEGER),
        FInt(ColumnType.INTEGER),
        FLong(ColumnType.INTEGER),
        FFloat(ColumnType.REAL),
        FDouble(ColumnType.REAL),
        FByteArray(ColumnType.BLOB);

        companion object {
            fun from(p: Any?): FieldType? {
                return when (p) {
                    is String -> FString
                    is Byte -> FByte
                    is Short -> FShort
                    is Int -> FInt
                    is Long -> FLong
                    is Float -> FFloat
                    is Double -> FDouble
                    is ByteArray -> FByteArray
                    else -> null
                }
            }

        }

        fun from(c: Cursor, i: Int): Any {
            return when (this) {
                FString -> c.getString(i)
                FByte -> c.getShort(i)
                FShort -> c.getShort(i)
                FInt -> c.getInt(i)
                FLong -> c.getLong(i)
                FFloat -> c.getFloat(i)
                FDouble -> c.getDouble(i)
                FByteArray -> c.getBlob(i)
            }
        }
    }


    // Todo - use Typed Fields and envorce it.
    abstract val TBL: String
    open val ID: String = "ROWID"
    open fun fieldGen(t: T) = toPairs(t).map { it.first to FieldType.from(it.second) }
    abstract val instance: T
    val FIELDGEN by lazy { fieldGen(instance) }
    val FIELDS by lazy { FIELDGEN.filterIndexed { index, pair -> pair.first != ID } }
    val IDFIELDS by lazy { listOf(ID to FieldType.FInt) + FIELDS }
    val FIELDNAMES by lazy { IDFIELDS.map { it.first }.toTypedArray() }

    // Creating Tables
    override fun onCreate(db: SQLiteDatabase) {
        val sql =
                (listOf(ID to "INTEGER PRIMARY KEY") + FIELDS).map { "${it.first} ${it.second}" }.
                        joinToString(", ", "CREATE TABLE ${TBL} (", ")")
        db.execSQL(sql)
    }

    // Upgrading database
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS ${TBL}")
        // Create tables again
        onCreate(db)
    }

    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */

    // Adding new
    internal fun add(t: T) = addContentValues(from(t))

    internal fun addContentValues(values: ContentValues): Long {
        assert(values.size() == FIELDS.size)
        val db = this.writableDatabase
        // Inserting Row
        val res = db.insert(TBL, null, values)
        db.close() // Closing database connection
        return res
    }


    open fun current(cursor: Cursor): ContentValues {
        val rowid = cursor.getInt(0)
        val name = cursor.getString(1)
        val pairs = currentValues(cursor)
        return pairs.asContentValues()
    }

    fun currentValues(cursor: Cursor): List<Pair<String, Any?>> {
        val pairs = IDFIELDS.mapIndexed { index, pair ->
            val (name, f) = pair
            name to f?.from(cursor, index)
        }
        return pairs
    }

    // Getting single
    internal fun get(id: Int): ContentValues {
        val db = this.readableDatabase
        val cursor = db.query(TBL, FIELDNAMES, "${ID}=?", arrayOf("${id}")
                , null, null, null, null)
        cursor?.moveToFirst()
        val res = current(cursor)
        cursor.close()
        return res
    }


    // Getting All
    // Select All Query
    // looping through all rows and adding to list
    // Adding MAllowed to list
    // return MAllowed list
    fun selectAll(): List<T> = selectAllContentValues().map { to(it) }

    fun selectAllContentValues(): ArrayList<ContentValues> {
        val lst = ArrayList<ContentValues>()
        val selectQuery = "SELECT  * FROM ${TBL}"
        val db = this.writableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        if (cursor.moveToFirst()) {
            do {
                lst.add(current(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lst
    }

    // todo add  orm conditions etd
    // Updating single
    fun update(t: T) = updateContentValues(from(t))

    fun updateContentValues(values: ContentValues): Int {
        val db = this.writableDatabase

        // updating row
        return db.update(TBL, values, "ID = ?", arrayOf("${values.get(ID)}"))
    }

    // Deleting single MAllowed
    fun delete(t: T) = deleteContentValues(from(t))

    fun deleteContentValues(values: ContentValues) {
        val db = this.writableDatabase
        db.delete(TBL, "${ID} = ?", arrayOf("${values.get(ID)}"))
        db.close()
    }


    // Getting MAlloweds Count
    // return count
    val count: Int
        get() {
            val countQuery = "SELECT  * FROM ${TBL}"
            val db = this.readableDatabase
            val cursor = db.rawQuery(countQuery, null)
            val res = cursor.count
            cursor.close()
            return res
        }

}

fun ContentValues.asSet(): MutableSet<MutableMap.MutableEntry<String, Any>>? {
    return this.valueSet()
}

fun List<Pair<String, Any?>>.asContentValues(): ContentValues {
    val res = ContentValues()
    for (pair in this) {
        val (k, v) = pair
        when (v) {
            is String -> res.put(k, v)
            is Byte -> res.put(k, v)
            is Short -> res.put(k, v)
            is Int -> res.put(k, v)
            is Long -> res.put(k, v)
            is Float -> res.put(k, v)
            is Double -> res.put(k, v)
            is ByteArray -> res.put(k, v)
        }
    }
    return res
}


