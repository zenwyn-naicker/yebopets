/**************************************************************************************************************
 * Author: Android Developers
 * Date: 2026
 * Title: Room releases and annotation processor setup
 * Type: [Source Code]
 * Available at: <https://developer.android.com/jetpack/androidx/releases/room> [Accessed 15 September 2026]
 **************************************************************************************************************/

package za.co.yebopets.part3;

import androidx.annotation.NonNull;
import androidx.room.*;
import java.util.List;

@Database(entities = {LocalDb.Pet.class, LocalDb.Pending.class, LocalDb.Draft.class}, version = 1, exportSchema = false)
public abstract class LocalDb extends RoomDatabase {
    public abstract Store store();
    @Entity(tableName="pets", primaryKeys={"uid","id"})
    public static class Pet {
        @NonNull public String uid="";
        @NonNull public String id="";
        @NonNull public String json="";
        public boolean saved;
    }
    @Entity(tableName="pending", indices={@Index("uid")})
    public static class Pending {
        @PrimaryKey @NonNull public String operationId="";
        @NonNull public String uid="";
        @NonNull public String animalId="";
        public boolean saved;
        public long createdAt;
        public int retryCount;
        public String lastError;
    }
    @Entity(tableName="drafts", primaryKeys={"uid","animalId"})
    public static class Draft {
        @NonNull public String uid="";
        @NonNull public String animalId="";
        @NonNull public String json="";
        public long lastEditedAt;
    }
    @Dao public interface Store {
        @Query("SELECT * FROM pets WHERE uid=:uid") List<Pet> pets(String uid);
        @Query("SELECT * FROM pets WHERE uid=:uid AND id=:id") Pet pet(String uid,String id);
        @Insert(onConflict=OnConflictStrategy.REPLACE) void pet(Pet value);
        @Insert void enqueue(Pending value);
        @Query("SELECT * FROM pending WHERE uid=:uid ORDER BY createdAt, operationId") List<Pending> pending(String uid);
        @Query("DELETE FROM pending WHERE uid=:uid AND operationId=:id") void acknowledge(String uid,String id);
        @Query("UPDATE pending SET retryCount=retryCount+1,lastError=:error WHERE uid=:uid AND operationId=:id") void failed(String uid,String id,String error);
        @Query("SELECT * FROM drafts WHERE uid=:uid AND animalId=:id") Draft draft(String uid,String id);
        @Insert(onConflict=OnConflictStrategy.REPLACE) void draft(Draft value);
        @Query("DELETE FROM drafts WHERE uid=:uid AND animalId=:id") void deleteDraft(String uid,String id);
        @Query("DELETE FROM pets WHERE uid=:uid") void clearPets(String uid);
        @Query("DELETE FROM pending WHERE uid=:uid") void clearPending(String uid);
        @Query("DELETE FROM drafts WHERE uid=:uid") void clearDrafts(String uid);
    }
}