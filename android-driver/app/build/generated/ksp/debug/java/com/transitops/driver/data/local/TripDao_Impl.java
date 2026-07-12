package com.transitops.driver.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TripDao_Impl implements TripDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CachedTrip> __insertionAdapterOfCachedTrip;

  private final SharedSQLiteStatement __preparedStmtOfClearTrips;

  public TripDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCachedTrip = new EntityInsertionAdapter<CachedTrip>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cached_trips` (`tripId`,`status`,`sourceName`,`sourceLat`,`sourceLng`,`destName`,`destLat`,`destLng`,`cargoWeightKg`,`vehicleId`,`vehicleRegNumber`,`routePolyline`,`dispatchedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CachedTrip entity) {
        statement.bindString(1, entity.getTripId());
        statement.bindString(2, entity.getStatus());
        statement.bindString(3, entity.getSourceName());
        statement.bindDouble(4, entity.getSourceLat());
        statement.bindDouble(5, entity.getSourceLng());
        statement.bindString(6, entity.getDestName());
        statement.bindDouble(7, entity.getDestLat());
        statement.bindDouble(8, entity.getDestLng());
        statement.bindDouble(9, entity.getCargoWeightKg());
        statement.bindString(10, entity.getVehicleId());
        statement.bindString(11, entity.getVehicleRegNumber());
        if (entity.getRoutePolyline() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getRoutePolyline());
        }
        if (entity.getDispatchedAt() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getDispatchedAt());
        }
      }
    };
    this.__preparedStmtOfClearTrips = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cached_trips";
        return _query;
      }
    };
  }

  @Override
  public Object insertTrip(final CachedTrip trip, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCachedTrip.insert(trip);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearTrips(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearTrips.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearTrips.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getActiveTrip(final Continuation<? super CachedTrip> $completion) {
    final String _sql = "SELECT * FROM cached_trips LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CachedTrip>() {
      @Override
      @Nullable
      public CachedTrip call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTripId = CursorUtil.getColumnIndexOrThrow(_cursor, "tripId");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfSourceName = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceName");
          final int _cursorIndexOfSourceLat = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceLat");
          final int _cursorIndexOfSourceLng = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceLng");
          final int _cursorIndexOfDestName = CursorUtil.getColumnIndexOrThrow(_cursor, "destName");
          final int _cursorIndexOfDestLat = CursorUtil.getColumnIndexOrThrow(_cursor, "destLat");
          final int _cursorIndexOfDestLng = CursorUtil.getColumnIndexOrThrow(_cursor, "destLng");
          final int _cursorIndexOfCargoWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "cargoWeightKg");
          final int _cursorIndexOfVehicleId = CursorUtil.getColumnIndexOrThrow(_cursor, "vehicleId");
          final int _cursorIndexOfVehicleRegNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "vehicleRegNumber");
          final int _cursorIndexOfRoutePolyline = CursorUtil.getColumnIndexOrThrow(_cursor, "routePolyline");
          final int _cursorIndexOfDispatchedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "dispatchedAt");
          final CachedTrip _result;
          if (_cursor.moveToFirst()) {
            final String _tmpTripId;
            _tmpTripId = _cursor.getString(_cursorIndexOfTripId);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpSourceName;
            _tmpSourceName = _cursor.getString(_cursorIndexOfSourceName);
            final double _tmpSourceLat;
            _tmpSourceLat = _cursor.getDouble(_cursorIndexOfSourceLat);
            final double _tmpSourceLng;
            _tmpSourceLng = _cursor.getDouble(_cursorIndexOfSourceLng);
            final String _tmpDestName;
            _tmpDestName = _cursor.getString(_cursorIndexOfDestName);
            final double _tmpDestLat;
            _tmpDestLat = _cursor.getDouble(_cursorIndexOfDestLat);
            final double _tmpDestLng;
            _tmpDestLng = _cursor.getDouble(_cursorIndexOfDestLng);
            final double _tmpCargoWeightKg;
            _tmpCargoWeightKg = _cursor.getDouble(_cursorIndexOfCargoWeightKg);
            final String _tmpVehicleId;
            _tmpVehicleId = _cursor.getString(_cursorIndexOfVehicleId);
            final String _tmpVehicleRegNumber;
            _tmpVehicleRegNumber = _cursor.getString(_cursorIndexOfVehicleRegNumber);
            final String _tmpRoutePolyline;
            if (_cursor.isNull(_cursorIndexOfRoutePolyline)) {
              _tmpRoutePolyline = null;
            } else {
              _tmpRoutePolyline = _cursor.getString(_cursorIndexOfRoutePolyline);
            }
            final Long _tmpDispatchedAt;
            if (_cursor.isNull(_cursorIndexOfDispatchedAt)) {
              _tmpDispatchedAt = null;
            } else {
              _tmpDispatchedAt = _cursor.getLong(_cursorIndexOfDispatchedAt);
            }
            _result = new CachedTrip(_tmpTripId,_tmpStatus,_tmpSourceName,_tmpSourceLat,_tmpSourceLng,_tmpDestName,_tmpDestLat,_tmpDestLng,_tmpCargoWeightKg,_tmpVehicleId,_tmpVehicleRegNumber,_tmpRoutePolyline,_tmpDispatchedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
