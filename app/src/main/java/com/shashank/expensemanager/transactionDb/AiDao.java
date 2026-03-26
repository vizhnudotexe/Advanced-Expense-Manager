package com.shashank.expensemanager.transactionDb;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.shashank.expensemanager.models.ChatMessage;

import java.util.List;

@Dao
public interface AiDao {

    @Query("SELECT * FROM chatMessageTable ORDER BY id ASC")
    List<ChatMessage> getAllChats();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChat(ChatMessage chat);

    @Query("DELETE FROM chatMessageTable")
    void deleteAllChats();

    @Query("SELECT * FROM personalityTable WHERE id = 1")
    PersonalityEntry getPersonality();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPersonality(PersonalityEntry entry);
}
