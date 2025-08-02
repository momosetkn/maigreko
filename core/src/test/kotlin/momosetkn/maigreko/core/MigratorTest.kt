package momosetkn.maigreko.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import momosetkn.maigreko.db.PostgresDataSource
import momosetkn.maigreko.db.PostgresqlDatabase
import momosetkn.maigreko.util.sql

class MigratorTest : FunSpec({
    lateinit var migrator: Migrator
    beforeSpec {
        PostgresqlDatabase.start()
        val container = PostgresqlDatabase.startedContainer
        val dataSource = PostgresDataSource(container)
        migrator = Migrator(dataSource)
    }
    test("createTable") {
        migrator.createTable(
            "migrations",
            "version",
            "version2",
        )

        val ddl = PostgresqlDatabase.generateDdl()
        ddl shouldBe sql(
            """
            --
            -- PostgreSQL database dump
            --

            -- Dumped from database version 15.8 (Debian 15.8-1.pgdg120+1)
            -- Dumped by pg_dump version 15.8 (Debian 15.8-1.pgdg120+1)

            SET statement_timeout = 0;
            SET lock_timeout = 0;
            SET idle_in_transaction_session_timeout = 0;
            SET client_encoding = 'UTF8';
            SET standard_conforming_strings = on;
            SELECT pg_catalog.set_config('search_path', '', false);
            SET check_function_bodies = false;
            SET xmloption = content;
            SET client_min_messages = warning;
            SET row_security = off;

            SET default_tablespace = '';

            SET default_table_access_method = heap;

            --
            -- Name: migrations; Type: TABLE; Schema: public; Owner: test
            --

            CREATE TABLE public.migrations (
                version character varying(255) NOT NULL
            );


            ALTER TABLE public.migrations OWNER TO test;

            --
            -- Name: migrations migrations_pkey; Type: CONSTRAINT; Schema: public; Owner: test
            --

            ALTER TABLE ONLY public.migrations
                ADD CONSTRAINT migrations_pkey PRIMARY KEY (version);


            --
            -- PostgreSQL database dump complete
            --


            """.trimIndent()
        )
    }
})
