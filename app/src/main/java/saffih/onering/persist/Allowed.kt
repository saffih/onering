/*
 * Copyright (c) 5/29/17 3:35 PM. by Saffi Hartal
 */

package saffih.onering.persist

import android.content.ContentValues
import android.content.Context
import saffih.tools.DatabaseHandler

/**
 * Model for Allowed view
 */
data class MAllowed(val ROWID: Int? = null, val name: String)

abstract class DBHelper<T>(context: Context) : DatabaseHandler<T>(context, "onering.db", 1)


class AllowedHelper(context: Context) : DBHelper<MAllowed>(context) {
    override val instance = MAllowed(name = "")
    val NAME = "name"
    override fun to(values: ContentValues) =
            MAllowed(ROWID = values.get(ID) as Int, name = values.get(NAME) as String)

    override fun toPairs(t: MAllowed) = listOf(ID to t.ROWID, NAME to t.name)

    override val TBL = "Allowed"
}
