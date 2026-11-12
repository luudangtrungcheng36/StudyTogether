-- Run this statement separately while connected to the postgres database.
-- Use autocommit; CREATE DATABASE cannot run inside a transaction.
-- If study_community already exists, skip this file. Nothing is dropped.
CREATE DATABASE study_community
    WITH ENCODING = 'UTF8'
         TEMPLATE = template0;
