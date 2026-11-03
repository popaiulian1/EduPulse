-- DEV ONLY seed users (passwords: admin123 / officer123 / user123).
-- Remove or replace before any real deployment.
-- Hashes are BCrypt ($2a$), compatible with Spring's BCryptPasswordEncoder.
-- crypt/gen_salt come from pgcrypto in the public schema, hence the qualification.
INSERT INTO users (username, password_hash, role, org_unit) VALUES
    ('admin',   public.crypt('Admin123',   public.gen_salt('bf', 10)), 'ROLE_ADMIN',              'SCHOOL_1'),
    ('user',    public.crypt('User123',    public.gen_salt('bf', 10)), 'ROLE_USER',               'SCHOOL_2');
