package org.sqldroid;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;

import junit.framework.AssertionFailedError;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class SQLDroidTest {

    private static final File DB_DIR = new File("/data/data/org.sqldroid/databases/");

    static {
        registerDriver();
    }

    private static void registerDriver() {
        try {
            DriverManager.registerDriver((Driver) (Class
                    .forName("org.sqldroid.SQLDroidDriver", true, SQLDroidTest.class.getClassLoader()).newInstance()));
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
            throw new AssertionFailedError(e.toString());
        }
    }
    
    @Test
    public void shouldRetrieveInsertedBasicTypes() throws SQLException {
        try (Connection conn = DriverManager.getConnection(createDatabase("basic-types.db"))) {
            String createTableStatement = "CREATE TABLE dummytable (id int, aString VARCHAR(254), aByte byte, "
                    + "aShort short, anInt int, aLong long, aBool boolean, aFloat float, aDouble double, aText text)";
            conn.createStatement().execute(createTableStatement);
            
            int id = 4325;
            String string = "test";
            byte b = 23;
            short s = 421;
            int i = 12551;
            long l = 23423525322L;
            boolean bool = false;
            float f = 324235.0f;
            double d = 123425.125;
            String text = "some potentially very long text";
            
            String insertStmt = "insert into dummytable "
                    + "(id, aString, aByte, aShort, anInt, aLong, aBool, aFloat, aDouble, aText) "
                    + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertStmt)) {
                stmt.setInt(1, id);
                stmt.setString(2, string);
                stmt.setByte(3, b);
                stmt.setShort(4, s);
                stmt.setInt(5, i);
                stmt.setLong(6, l);
                stmt.setBoolean(7, bool);
                stmt.setFloat(8, f);
                stmt.setDouble(9, d);
                stmt.setString(10, text);
                int rowCount = stmt.executeUpdate();
                assertThat(rowCount).as("rowCount").isEqualTo(1);
            }
            
            
            String selectStmt = "SELECT aString, aByte, aShort, anInt, aLong, aBool, aFloat, aDouble, aText "
                    + " FROM dummytable where id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(selectStmt)) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    
                    assertThat(string)
                        .isEqualTo(rs.getString(1)).isEqualTo(rs.getString("aString"))
                        .isEqualTo(rs.getObject(1)).isEqualTo(rs.getObject("aString"));
                    assertThat(b)
                        .isEqualTo(rs.getByte(2)).isEqualTo(rs.getByte("aByte"));
                    assertThat(s)
                        .isEqualTo(rs.getShort(3)).isEqualTo(rs.getShort("aShort"))
                        .isEqualTo((short)rs.getInt(3)).isEqualTo((short)rs.getInt("aShort"));
                    assertThat(i)
                        .isEqualTo(rs.getInt(4)).isEqualTo(rs.getInt("anInt"))
                        .isEqualTo((int)rs.getLong(4)).isEqualTo((int)rs.getLong("anInt"))
                        .isEqualTo(rs.getObject(4)).isEqualTo(rs.getObject("anInt"));
                    assertThat(l)
                        .isEqualTo(rs.getLong(5)).isEqualTo(rs.getLong("aLong"));
                    assertThat(bool)
                        .isEqualTo(rs.getBoolean(6)).isEqualTo(rs.getBoolean("aBool"))
                        .isEqualTo(rs.getInt(6) == 1).isEqualTo(rs.getInt("aBool") == 1)
                        .isEqualTo((int)rs.getObject(6) == 1);
                    assertThat(f)
                        .isEqualTo(rs.getFloat(7)).isEqualTo(rs.getFloat("aFloat"))
                        .isEqualTo(rs.getObject(7)).isEqualTo(rs.getObject("aFloat"))
                        .isEqualTo((float)rs.getDouble(7)).isEqualTo((float)rs.getDouble("aFloat"));
                    assertThat(d)
                        .isEqualTo(rs.getDouble(8)).isEqualTo(rs.getDouble("aDouble"))
                        .isEqualTo((double)(Float)rs.getObject(8)); // Is this intended?
                    assertThat(text)
                        .isEqualTo(rs.getString(9)).isEqualTo(rs.getString("aText"))
                        .isEqualTo(rs.getObject(9)).isEqualTo(rs.getObject("aText"));
                }
            }
        }
    }

    @Test
    public void shouldRetrieveInsertedNullValues() throws SQLException {
        try (Connection conn = DriverManager.getConnection(createDatabase("null-values.db"))) {
            String createTableStatement = "CREATE TABLE dummytable (id int, aString VARCHAR(254), aByte byte, "
                    + "aShort short, anInt int, aLong long, aBool boolean, aFloat float, aDouble double, aText text)";
            conn.createStatement().execute(createTableStatement);
            
            int id = 13155;
            
            String insertStmt = "insert into dummytable "
                    + "(id, aString, aByte, aShort, anInt, aLong, aBool, aFloat, aDouble, aText) "
                    + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertStmt)) {
                stmt.setInt(1, id);
                for (int i=2; i<=10; i++) {
                    stmt.setObject(i, null);
                }
                stmt.executeUpdate();
            }
            
            
            String selectStmt = "SELECT aString, aByte, aShort, anInt, aLong, aBool, aFloat, aDouble, aText "
                    + " FROM dummytable where id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(selectStmt)) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    
                    assertThat(rs.getString(1)).isNull();
                    
                    assertThat(rs.getByte(2)).isEqualTo((byte)0);
                    assertThat(rs.wasNull()).isTrue();
                    
                    assertThat(rs.getShort(3)).isEqualTo((short)0);
                    assertThat(rs.wasNull()).isTrue();
                    
                    assertThat(rs.getObject(4)).isNull();
                    assertThat(rs.getInt(4)).isEqualTo(0);
                    assertThat(rs.wasNull()).isTrue();
                    
                    assertThat(rs.getLong(5)).isEqualTo(0);
                    assertThat(rs.wasNull()).isTrue();

                    assertThat(rs.getBoolean(6)).isEqualTo(false);
                    assertThat(rs.wasNull()).isTrue();

                    assertThat(rs.getObject(7)).isNull();
                    assertThat(rs.getFloat(7)).isEqualTo(0.0f);
                    assertThat(rs.wasNull()).isTrue();
                    
                    assertThat(rs.getDouble(8)).isEqualTo(0.0);
                    assertThat(rs.wasNull()).isTrue();

                    assertThat(rs.getString(9)).isNull();
                }
            }
        }
    }

    @Test
    public void shouldRetrieveSavedBlob() throws SQLException {
        try (Connection conn = DriverManager.getConnection(createDatabase("blobs.db"))) {
            conn.createStatement().execute("create table blobtest (key int, value blob)");

            // create a blob
            final int blobSize = 70000;
            byte[] aBlob = new byte[blobSize];
            for (int counter = 0; counter < blobSize; counter++) {
                aBlob[counter] = (byte) (counter % 10);
            }

            int id = 441;
            try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO blobtest(key,value) VALUES (?, ?)")) {
                stmt.setInt(1, id);
                stmt.setBinaryStream(2, new ByteArrayInputStream(aBlob), aBlob.length);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement("SELECT value,key FROM blobtest where key = ?")) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    Blob blob = rs.getBlob(1);
                    assertThat(blob.getBytes(0, aBlob.length)).isEqualTo(aBlob);
                }
            }
        }
    }

    @Test
    public void shouldSaveAndRetrieveTimestamps() throws SQLException {
        try (Connection conn = DriverManager.getConnection(createDatabase("timestamps.db"))) {
            conn.createStatement()
                    .execute("create table timestamptest (id integer primary key autoincrement, created_at timestamp)");

            long id;

            Calendar calendar = new GregorianCalendar(2016, 7, 15, 12, 0, 0);
            Timestamp timestamp = new Timestamp(calendar.getTimeInMillis());

            try (PreparedStatement stmt = conn.prepareStatement("insert into timestamptest (created_at) values (?)",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setTimestamp(1, timestamp);
                stmt.executeUpdate();
                try (ResultSet rs = conn.createStatement().executeQuery("select last_insert_rowid();")) {
                    rs.next();
                    id = rs.getLong(1);
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement("select created_at from timestamptest where id = ?")) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    assertThat(rs.getTimestamp(1)).isEqualTo(timestamp);
                }
            }
        }
    }

    private String createDatabase(String filename) {
        DB_DIR.mkdirs();
        assertThat(DB_DIR).exists();

        File dbFile = new File(DB_DIR, filename);
        dbFile.delete();
        assertThat(dbFile).doesNotExist();

        return "jdbc:sqlite:/data/data/org.sqldroid/databases/" + filename;
    }
}
