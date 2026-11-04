package com.expensetracker.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

/** Unit tests for {@link SQLiteDataSourceFactory} */
class SQLiteDataSourceFactoryTest {

    @TempDir
    Path tempDir;

    @Test
    void successfullyCreatesDataSourceWithUserSpecifiedPath()
            throws IOException {

        Path databasePath = tempDir.resolve("expenses.db");

        DataSource dataSource = SQLiteDataSourceFactory.create(databasePath);

        assertNotNull(dataSource);
        SQLiteDataSource sqliteDataSource = assertInstanceOf(SQLiteDataSource.class, dataSource);

        assertEquals(
                "jdbc:sqlite:" + databasePath.toAbsolutePath().normalize(),
                sqliteDataSource.getUrl());
    }

    @Test
    void successfullyCreatesMissingParentDirectories()
            throws IOException {

        Path databasePath = tempDir
                .resolve("nested")
                .resolve("directory")
                .resolve("expenses.db");

        assertFalse(Files.exists(databasePath.getParent()));

        SQLiteDataSourceFactory.create(databasePath);

        assertTrue(Files.isDirectory(databasePath.getParent()));
    }

    @Test
    void successfullyCreatesDataSourceWhenParentDirectoryExists()
            throws IOException {

        Path databasePath = tempDir.resolve("expenses.db");

        assertTrue(Files.isDirectory(databasePath.getParent()));

        DataSource dataSource = SQLiteDataSourceFactory.create(databasePath);

        assertNotNull(dataSource);
        assertTrue(Files.isDirectory(databasePath.getParent()));
    }

    @Test
    void throwsNullPointerExceptionWhenUserSpecifiedPathIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> SQLiteDataSourceFactory.create(null));
    }

    @Test
    void throwsIOExceptionWhenParentDirectoryCannotBeCreated()
            throws IOException {

        Path existingFile = tempDir.resolve("existing-file");
        Files.createFile(existingFile);

        Path databasePath = existingFile
                .resolve("nested")
                .resolve("expenses.db");

        assertThrows(
                IOException.class,
                () -> SQLiteDataSourceFactory.create(databasePath));
    }

    @Test
    void doesNotCreateDatabaseFileWhenCreatingDataSource()
            throws IOException {

        Path databasePath = tempDir
                .resolve("nested")
                .resolve("expenses.db");

        assertFalse(Files.exists(databasePath));

        SQLiteDataSourceFactory.create(databasePath);

        assertTrue(Files.isDirectory(databasePath.getParent()));
        assertFalse(Files.exists(databasePath));
    }

    @Test
    void normalizesUserSpecifiedDatabasePath()
            throws IOException {

        Path databasePath = tempDir
                .resolve("nested")
                .resolve("..")
                .resolve("expenses.db");

        DataSource dataSource = SQLiteDataSourceFactory.create(databasePath);

        SQLiteDataSource sqliteDataSource = assertInstanceOf(SQLiteDataSource.class, dataSource);

        Path expectedPath = tempDir
                .resolve("expenses.db")
                .toAbsolutePath()
                .normalize();

        assertEquals(
                "jdbc:sqlite:" + expectedPath,
                sqliteDataSource.getUrl());
    }
}
