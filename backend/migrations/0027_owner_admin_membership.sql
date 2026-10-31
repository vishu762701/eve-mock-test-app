-- Preserve existing owner roles, but require explicit membership for every admin.
-- Staged only; production migration requires the manual release workflow.
INSERT OR IGNORE INTO admins (email, created_at)
SELECT lower(trim(email)), updated_at FROM admin_rights
WHERE custom_title = 'Owner' AND lower(trim(email)) IN (
 'pronlike9@gmail.com', 'own.keni@gmail.com',
 'anyqueairdrop@gmail.com', 'ghatisarkar56@gmail.com'
);
