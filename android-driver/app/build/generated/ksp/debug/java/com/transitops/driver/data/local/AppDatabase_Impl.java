package com.transitops.driver.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile OutboxDao _outboxDao;

  private volatile TripDao _tripDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `cached_trips` (`tripId` TEXT NOT NULL, `status` TEXT NOT NULL, `sourceName` TEXT NOT NULL, `sourceLat` REAL NOT NULL, `sourceLng` REAL NOT NULL, `destName` TEXT NOT NULL, `destLat` REAL NOT NULL, `destLng` REAL NOT NULL, `cargoWeightKg` REAL NOT NULL, `vehicleId` TEXT NOT NULL, `vehicleRegNumber` TEXT NOT NULL, `routePolyline` TEXT, `dispatchedAt` INTEGER, PRIMARY KEY(`tripId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `outbox_actions` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `idempotencyKey` TEXT NOT NULL, `type` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `synced` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f2f372a4a47c93463efb3c82528ed649')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `cached_trips`");
        db.execSQL("DROP TABLE IF EXISTS `outbox_actions`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsCachedTrips = new HashMap<String, TableInfo.Column>(13);
        _columnsCachedTrips.put("tripId", new TableInfo.Column("tripId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedTrips.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedTrips.put("sourceName", new TableInfo.Column("sourceName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedTrips.put("sourceLat", new TableInfo.Column("sourceLat", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedTrips.put("sourceLng", new TableInfo.Column("sourceLng", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedTrips.put("destName", new TableInfo.Column("destName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedTrips.put("destLat", new TableInfo.Column("destLat", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedTrips.put("destLng", new TableInfo.Column("destLng", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedTrips.put("cargoWeightKg", new TableInfo.Column("cargoWeightKg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedTrips.put("vehicleId", new TableInfo.Column("vehicleId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedTrips.put("vehicleRegNumber", new TableInfo.Column("vehicleRegNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedTrips.put("routePolyline", new TableInfo.Column("routePolyline", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedTrips.put("dispatchedAt", new TableInfo.Column("dispatchedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCachedTrips = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCachedTrips = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCachedTrips = new TableInfo("cached_trips", _columnsCachedTrips, _foreignKeysCachedTrips, _indicesCachedTrips);
        final TableInfo _existingCachedTrips = TableInfo.read(db, "cached_trips");
        if (!_infoCachedTrips.equals(_existingCachedTrips)) {
          return new RoomOpenHelper.ValidationResult(false, "cached_trips(com.transitops.driver.data.local.CachedTrip).\n"
                  + " Expected:\n" + _infoCachedTrips + "\n"
                  + " Found:\n" + _existingCachedTrips);
        }
        final HashMap<String, TableInfo.Column> _columnsOutboxActions = new HashMap<String, TableInfo.Column>(6);
        _columnsOutboxActions.put("localId", new TableInfo.Column("localId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOutboxActions.put("idempotencyKey", new TableInfo.Column("idempotencyKey", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOutboxActions.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOutboxActions.put("payloadJson", new TableInfo.Column("payloadJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOutboxActions.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOutboxActions.put("synced", new TableInfo.Column("synced", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysOutboxActions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesOutboxActions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoOutboxActions = new TableInfo("outbox_actions", _columnsOutboxActions, _foreignKeysOutboxActions, _indicesOutboxActions);
        final TableInfo _existingOutboxActions = TableInfo.read(db, "outbox_actions");
        if (!_infoOutboxActions.equals(_existingOutboxActions)) {
          return new RoomOpenHelper.ValidationResult(false, "outbox_actions(com.transitops.driver.data.local.OutboxAction).\n"
                  + " Expected:\n" + _infoOutboxActions + "\n"
                  + " Found:\n" + _existingOutboxActions);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "f2f372a4a47c93463efb3c82528ed649", "1fa7d034b18a51f0ba69cb78ae523261");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "cached_trips","outbox_actions");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `cached_trips`");
      _db.execSQL("DELETE FROM `outbox_actions`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(OutboxDao.class, OutboxDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TripDao.class, TripDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public OutboxDao outboxDao() {
    if (_outboxDao != null) {
      return _outboxDao;
    } else {
      synchronized(this) {
        if(_outboxDao == null) {
          _outboxDao = new OutboxDao_Impl(this);
        }
        return _outboxDao;
      }
    }
  }

  @Override
  public TripDao tripDao() {
    if (_tripDao != null) {
      return _tripDao;
    } else {
      synchronized(this) {
        if(_tripDao == null) {
          _tripDao = new TripDao_Impl(this);
        }
        return _tripDao;
      }
    }
  }
}
